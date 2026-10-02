package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.ClientNotAssignableException;
import com.formai.api.planning.domain.exceptions.ExerciseNotFoundException;
import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.CreateRoutineCommand;
import com.formai.api.planning.domain.model.commands.DuplicateRoutineCommand;
import com.formai.api.planning.domain.model.commands.UpdateRoutineCommand;
import com.formai.api.planning.domain.model.queries.GetRoutineByIdQuery;
import com.formai.api.planning.domain.model.queries.GetRoutineVersionsQuery;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.RoutineName;
import com.formai.api.planning.domain.services.ClientPlanCommandService;
import com.formai.api.planning.domain.services.RoutineCommandService;
import com.formai.api.planning.domain.services.RoutineQueryService;
import com.formai.api.planning.interfaces.rest.transform.ClientPlanAssemblerImpl;
import com.formai.api.planning.interfaces.rest.transform.RoutineAssemblerImpl;
import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.START_DATE;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.routine;
import static com.formai.api.planning.PlanningTestData.twoSessions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutinesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RoutineAssemblerImpl.class,
        ClientPlanAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class RoutinesControllerTest {

    private static final RequestPostProcessor TRAINER = user(TRAINER_HOLDER_ID).roles("TRAINER");

    private static final String ROUTINE_BODY = "{\"name\":\"Legs only\",\"sessions\":[{\"label\":\"Day A\","
            + "\"exercises\":[{\"exerciseId\":\"" + SQUAT_ID.value() + "\",\"sets\":3,\"reps\":10,"
            + "\"targetLoadKg\":60,\"restSeconds\":90}]}]}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RoutineCommandService routineCommandService;

    @MockitoBean
    RoutineQueryService routineQueryService;

    @MockitoBean
    ClientPlanCommandService clientPlanCommandService;

    private static String routineBody(int sets) {
        return ROUTINE_BODY.replace("\"sets\":3", "\"sets\":" + sets);
    }

    @Test
    void shouldReturn201WithTheDraftWhenARoutineIsCreated() throws Exception {
        var routine = routine();
        when(routineCommandService.handle(argThat((CreateRoutineCommand command) ->
                command.holderId().equals(TRAINER_HOLDER_ID) && command.sessions().getFirst().getOrder() == 1
                        && command.sessions().getFirst().getExercises().getFirst().getPrescription().sets() == 3)))
                .thenReturn(Optional.of(routine));

        mockMvc.perform(post("/api/v1/routines").contentType(MediaType.APPLICATION_JSON).content(ROUTINE_BODY).with(TRAINER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andExpect(jsonPath("$.sessions[0].exercises[0].exerciseName").value("Squat"))
                .andExpect(jsonPath("$.sessions[0].exercises[0].sets").value(3));
    }

    @Test
    void shouldReturn422ForSetsOfZero() throws Exception {
        mockMvc.perform(post("/api/v1/routines").contentType(MediaType.APPLICATION_JSON).content(routineBody(0)).with(TRAINER))
                .andExpect(status().isUnprocessableEntity());

        verifyNoInteractions(routineCommandService);
    }

    @Test
    void shouldReturn400WhenAPrescriptionValueIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/routines").contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTINE_BODY.replace("\"reps\":10,", "")).with(TRAINER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForAnExerciseOutsideTheCatalog() throws Exception {
        when(routineCommandService.handle(any(CreateRoutineCommand.class))).thenThrow(new ExerciseNotFoundException());

        mockMvc.perform(post("/api/v1/routines").contentType(MediaType.APPLICATION_JSON).content(ROUTINE_BODY).with(TRAINER))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnTheNewVersionWhenARoutineIsModified() throws Exception {
        var routine = routine();
        routine.revise(new UpdateRoutineCommand(routine.getId(), TRAINER_HOLDER_ID, new RoutineName("Legs only"),
                twoSessions()));
        when(routineCommandService.handle(any(UpdateRoutineCommand.class))).thenReturn(Optional.of(routine));

        mockMvc.perform(put("/api/v1/routines/" + routine.getId().value())
                        .contentType(MediaType.APPLICATION_JSON).content(ROUTINE_BODY).with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion").value(2));
    }

    @Test
    void shouldListTheVersionHistory() throws Exception {
        var routine = routine();
        routine.revise(new UpdateRoutineCommand(routine.getId(), "editor", routine.getName(), twoSessions()));
        when(routineQueryService.handle(any(GetRoutineVersionsQuery.class)))
                .thenReturn(List.of(routine.getVersions().get(1), routine.getVersions().get(0)));

        mockMvc.perform(get("/api/v1/routines/" + routine.getId().value() + "/versions").with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value(2))
                .andExpect(jsonPath("$[0].author").value("editor"))
                .andExpect(jsonPath("$[1].number").value(1))
                .andExpect(jsonPath("$[1].changedAt").exists());
    }

    @Test
    void shouldReturn404ForTheVersionsOfAnUnknownRoutine() throws Exception {
        when(routineQueryService.handle(any(GetRoutineVersionsQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/routines/" + UUID.randomUUID() + "/versions").with(TRAINER))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn201WhenARoutineIsDuplicated() throws Exception {
        var copy = routine();
        when(routineCommandService.handle(any(DuplicateRoutineCommand.class))).thenReturn(Optional.of(copy));

        mockMvc.perform(post("/api/v1/routines/" + UUID.randomUUID() + "/duplicates")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Copy\"}").with(TRAINER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldAssignTheRoutineToEveryClientListedOnce() throws Exception {
        var routine = routine();
        var otherClient = new ClientId(UUID.randomUUID());
        when(routineQueryService.handle(any(GetRoutineByIdQuery.class))).thenReturn(Optional.of(routine));
        when(clientPlanCommandService.handle(any(AssignRoutineCommand.class))).thenAnswer(call -> {
            AssignRoutineCommand command = call.getArgument(0);
            var plan = ClientPlan.startFor(command.clientId(), TRAINER_HOLDER_ID);
            plan.assign(command);
            return Optional.of(plan);
        });
        var body = "{\"clientIds\":[\"" + CLIENT_ID.value() + "\",\"" + otherClient.value() + "\",\""
                + CLIENT_ID.value() + "\"],\"startDate\":\"" + START_DATE + "\"}";

        mockMvc.perform(post("/api/v1/routines/" + routine.getId().value() + "/assignments")
                        .contentType(MediaType.APPLICATION_JSON).content(body).with(TRAINER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].routineName").value("Strength 12 weeks"))
                .andExpect(jsonPath("$[0].startDate").value(START_DATE.toString()))
                .andExpect(jsonPath("$[0].current").value(true));
    }

    @Test
    void shouldReturn422WhenAClientIsNotActive() throws Exception {
        when(routineQueryService.handle(any(GetRoutineByIdQuery.class))).thenReturn(Optional.of(routine()));
        when(clientPlanCommandService.handle(any(AssignRoutineCommand.class)))
                .thenThrow(new ClientNotAssignableException());
        var body = "{\"clientIds\":[\"" + CLIENT_ID.value() + "\"],\"startDate\":\"" + START_DATE + "\"}";

        mockMvc.perform(post("/api/v1/routines/" + UUID.randomUUID() + "/assignments")
                        .contentType(MediaType.APPLICATION_JSON).content(body).with(TRAINER))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturn400WhenNoClientIsListed() throws Exception {
        mockMvc.perform(post("/api/v1/routines/" + UUID.randomUUID() + "/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientIds\":[],\"startDate\":\"" + START_DATE + "\"}").with(TRAINER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForAnotherTrainersRoutine() throws Exception {
        when(routineQueryService.handle(any(GetRoutineByIdQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/routines/" + UUID.randomUUID()).with(TRAINER))
                .andExpect(status().isNotFound());
    }
}
