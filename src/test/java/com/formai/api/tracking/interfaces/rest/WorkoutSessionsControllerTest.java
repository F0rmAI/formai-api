package com.formai.api.tracking.interfaces.rest;

import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.exceptions.InvalidSetValueException;
import com.formai.api.tracking.domain.exceptions.PartialFinishNotConfirmedException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionAlreadyFinishedException;
import com.formai.api.tracking.domain.exceptions.WorkoutSessionNotFoundException;
import com.formai.api.tracking.domain.model.commands.CorrectSetCommand;
import com.formai.api.tracking.domain.model.commands.FinishWorkoutSessionCommand;
import com.formai.api.tracking.domain.model.commands.RecordSetCommand;
import com.formai.api.tracking.domain.model.queries.GetWorkoutHistoryQuery;
import com.formai.api.tracking.domain.model.queries.GetWorkoutSessionByIdQuery;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.Reps;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;
import com.formai.api.tracking.domain.services.WorkoutSessionCommandService;
import com.formai.api.tracking.domain.services.WorkoutSessionQueryService;
import com.formai.api.tracking.interfaces.rest.transform.WorkoutSessionAssemblerImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.tracking.TrackingTestData.CLIENT_HOLDER_ID;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.SQUAT;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.TRAINER_HOLDER_ID;
import static com.formai.api.tracking.TrackingTestData.pendingSession;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkoutSessionsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WorkoutSessionAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class WorkoutSessionsControllerTest {

    private static final RequestPostProcessor TRAINER = user(TRAINER_HOLDER_ID).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_HOLDER_ID).roles("CLIENT");

    private static final String SET_BODY =
            "{\"exerciseId\":\"" + SQUAT.exerciseId().value() + "\",\"setNumber\":1,\"loadKg\":60,\"reps\":10}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WorkoutSessionCommandService workoutSessionCommandService;

    @MockitoBean
    WorkoutSessionQueryService workoutSessionQueryService;

    private static String setsPath(UUID sessionId) {
        return "/api/v1/workout-sessions/" + sessionId + "/sets";
    }

    @Test
    void shouldReturnTheClientsHistoryWithVolumeAndSets() throws Exception {
        var session = pendingSession(TODAY);
        session.recordSet(new RecordSetCommand(session.getId(), CLIENT_ID, SQUAT.exerciseId(), 1,
                new Load(new BigDecimal("60")), new Reps(10)));
        when(workoutSessionQueryService.handle(argThat((GetWorkoutHistoryQuery query) ->
                query.clientId().equals(CLIENT_ID) && query.requesterHolderId().equals(CLIENT_HOLDER_ID)
                        && query.period().isEmpty() && query.pagination().size() == 20)))
                .thenReturn(new WorkoutSessionPage(List.of(session), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/workout-sessions").with(CLIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].totalVolumeKg").value(600))
                .andExpect(jsonPath("$.content[0].exercises[0].targetSets").value(3))
                .andExpect(jsonPath("$.content[0].exercises[0].sets[0].loadKg").value(60))
                .andExpect(jsonPath("$.content[0].exercises[0].sets[0].reps").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterTheHistoryByDateRange() throws Exception {
        when(workoutSessionQueryService.handle(argThat((GetWorkoutHistoryQuery query) ->
                query.period().isPresent() && query.period().get().from().equals(TODAY.minusDays(7)))))
                .thenReturn(new WorkoutSessionPage(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/workout-sessions")
                        .param("from", TODAY.minusDays(7).toString())
                        .param("to", TODAY.toString()).with(CLIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldReturn400WhenOnlyOneDateIsGiven() throws Exception {
        mockMvc.perform(get("/api/v1/workout-sessions").param("from", TODAY.toString()).with(CLIENT))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workoutSessionQueryService);
    }

    @Test
    void shouldReturn400WhenThePageSizeIsTooLarge() throws Exception {
        mockMvc.perform(get("/api/v1/workout-sessions").param("size", "500").with(CLIENT))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLetATrainerReadTheirClientsHistory() throws Exception {
        when(workoutSessionQueryService.handle(argThat((GetWorkoutHistoryQuery query) ->
                query.clientId().equals(CLIENT_ID) && query.requesterHolderId().equals(TRAINER_HOLDER_ID))))
                .thenReturn(new WorkoutSessionPage(List.of(pendingSession(TODAY)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value() + "/workout-sessions").with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldReturn403ForAClientOfAnotherTrainer() throws Exception {
        when(workoutSessionQueryService.handle(any(GetWorkoutHistoryQuery.class)))
                .thenThrow(new ClientAccessDeniedException());

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value() + "/workout-sessions").with(TRAINER))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectARequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/workout-sessions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForATrainerOnTheClientsOwnHistory() throws Exception {
        mockMvc.perform(get("/api/v1/workout-sessions").with(TRAINER))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnOneOfTheClientsSessions() throws Exception {
        var session = pendingSession(TODAY);
        when(workoutSessionQueryService.handle(any(GetWorkoutSessionByIdQuery.class))).thenReturn(Optional.of(session));

        mockMvc.perform(get("/api/v1/workout-sessions/" + session.getId().value()).with(CLIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId().value().toString()))
                .andExpect(jsonPath("$.dayLabel").value("Day A · Legs"));
    }

    @Test
    void shouldReturn404ForAnUnknownSession() throws Exception {
        when(workoutSessionQueryService.handle(any(GetWorkoutSessionByIdQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/workout-sessions/" + UUID.randomUUID()).with(CLIENT))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn201WithTheUpdatedSessionWhenASetIsRecorded() throws Exception {
        var session = pendingSession(TODAY);
        when(workoutSessionCommandService.handle(argThat((RecordSetCommand command) ->
                command.clientId().equals(CLIENT_ID) && command.setNumber() == 1 && command.reps().value() == 10)))
                .thenReturn(Optional.of(session));

        mockMvc.perform(post(setsPath(session.getId().value()))
                        .contentType(MediaType.APPLICATION_JSON).content(SET_BODY).with(CLIENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(session.getId().value().toString()));
    }

    @Test
    void shouldReturn422ForNegativeLoad() throws Exception {
        var body = "{\"exerciseId\":\"" + SQUAT.exerciseId().value() + "\",\"setNumber\":1,\"loadKg\":-5,\"reps\":10}";

        mockMvc.perform(post(setsPath(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON).content(body).with(CLIENT))
                .andExpect(status().isUnprocessableEntity());

        verifyNoInteractions(workoutSessionCommandService);
    }

    @Test
    void shouldReturn422ForASetOutsideThePrescription() throws Exception {
        when(workoutSessionCommandService.handle(any(RecordSetCommand.class)))
                .thenThrow(new InvalidSetValueException("Set number must be between 1 and 3 for Squat"));

        mockMvc.perform(post(setsPath(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON).content(SET_BODY).with(CLIENT))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturn400WhenRepsAreMissing() throws Exception {
        var body = "{\"exerciseId\":\"" + SQUAT.exerciseId().value() + "\",\"setNumber\":1,\"loadKg\":60}";

        mockMvc.perform(post(setsPath(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON).content(body).with(CLIENT))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenRecordingOnAnotherClientsSession() throws Exception {
        when(workoutSessionCommandService.handle(any(RecordSetCommand.class)))
                .thenThrow(new WorkoutSessionNotFoundException());

        mockMvc.perform(post(setsPath(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON).content(SET_BODY).with(CLIENT))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenRecordingOnAFinishedSession() throws Exception {
        when(workoutSessionCommandService.handle(any(RecordSetCommand.class)))
                .thenThrow(new WorkoutSessionAlreadyFinishedException());

        mockMvc.perform(post(setsPath(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON).content(SET_BODY).with(CLIENT))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn201WhenASetIsCorrected() throws Exception {
        var session = pendingSession(TODAY);
        when(workoutSessionCommandService.handle(any(CorrectSetCommand.class))).thenReturn(Optional.of(session));

        mockMvc.perform(post("/api/v1/workout-sessions/" + session.getId().value() + "/corrections")
                        .contentType(MediaType.APPLICATION_JSON).content(SET_BODY).with(CLIENT))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn201WithTheComplianceStatusWhenFinished() throws Exception {
        var session = pendingSession(TODAY);
        session.recordSet(new RecordSetCommand(session.getId(), CLIENT_ID, SQUAT.exerciseId(), 1,
                new Load(new BigDecimal("60")), new Reps(10)));
        session.finish(new FinishWorkoutSessionCommand(session.getId(), CLIENT_ID, true));
        when(workoutSessionCommandService.handle(argThat((FinishWorkoutSessionCommand command) ->
                command.confirmPartial()))).thenReturn(Optional.of(session));

        mockMvc.perform(post("/api/v1/workout-sessions/" + session.getId().value() + "/completions")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmPartial\":true}").with(CLIENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.finishedAt").exists());
    }

    @Test
    void shouldReturn409WhenAPartialFinishIsNotConfirmed() throws Exception {
        when(workoutSessionCommandService.handle(any(FinishWorkoutSessionCommand.class)))
                .thenThrow(new PartialFinishNotConfirmedException());

        mockMvc.perform(post("/api/v1/workout-sessions/" + UUID.randomUUID() + "/completions")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmPartial\":false}").with(CLIENT))
                .andExpect(status().isConflict());
    }
}
