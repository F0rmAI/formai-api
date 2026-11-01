package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.AssignmentNotFoundException;
import com.formai.api.planning.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.planning.domain.exceptions.RoutineNotFoundException;
import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.commands.PersonalizeAssignmentCommand;
import com.formai.api.planning.domain.model.queries.GetClientPlanQuery;
import com.formai.api.planning.domain.model.queries.GetRoutineByIdQuery;
import com.formai.api.planning.domain.model.valueobjects.AssignmentNote;
import com.formai.api.planning.domain.model.valueobjects.PrescriptionAdjustment;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;
import com.formai.api.planning.domain.services.ClientPlanCommandService;
import com.formai.api.planning.domain.services.ClientPlanQueryService;
import com.formai.api.planning.domain.services.RoutineQueryService;
import com.formai.api.planning.interfaces.rest.transform.ClientPlanAssemblerImpl;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.START_DATE;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.emptyPlan;
import static com.formai.api.planning.PlanningTestData.routine;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssignmentsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ClientPlanAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class AssignmentsControllerTest {

    private static final RequestPostProcessor TRAINER = user(TRAINER_HOLDER_ID).roles("TRAINER");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ClientPlanCommandService clientPlanCommandService;

    @MockitoBean
    ClientPlanQueryService clientPlanQueryService;

    @MockitoBean
    RoutineQueryService routineQueryService;

    @Test
    void shouldListTheClientsAssignmentsMostRecentFirst() throws Exception {
        var current = routine();
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(new RoutineId(UUID.randomUUID()), CLIENT_ID, TRAINER_HOLDER_ID, START_DATE, TrainingDays.everyDay()));
        plan.assign(new AssignRoutineCommand(current.getId(), CLIENT_ID, TRAINER_HOLDER_ID, START_DATE.plusDays(30), TrainingDays.everyDay()));
        when(clientPlanQueryService.handle(argThat((GetClientPlanQuery query) ->
                query.clientId().equals(CLIENT_ID) && query.holderId().equals(TRAINER_HOLDER_ID))))
                .thenReturn(Optional.of(plan));
        when(routineQueryService.handle(any(GetRoutineByIdQuery.class))).thenReturn(Optional.empty());
        when(routineQueryService.handle(new GetRoutineByIdQuery(current.getId(), TRAINER_HOLDER_ID)))
                .thenReturn(Optional.of(current));

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value() + "/assignments").with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].routineName").value("Strength 12 weeks"))
                .andExpect(jsonPath("$[0].current").value(true))
                .andExpect(jsonPath("$[1].current").value(false))
                .andExpect(jsonPath("$[1].endDate").value(START_DATE.plusDays(29).toString()));
    }

    @Test
    void shouldReturnAnEmptyListForAClientWithoutAssignments() throws Exception {
        when(clientPlanQueryService.handle(any(GetClientPlanQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value() + "/assignments").with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn403ForAClientThatIsNotTheTrainers() throws Exception {
        when(clientPlanQueryService.handle(any(GetClientPlanQuery.class))).thenThrow(new ClientAccessDeniedException());

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value() + "/assignments").with(TRAINER))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldPersonalizeTheCurrentAssignment() throws Exception {
        var current = routine();
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(current.getId(), CLIENT_ID, TRAINER_HOLDER_ID, START_DATE,
                TrainingDays.everyDay()));
        plan.personalizeCurrentAssignment(new PersonalizeAssignmentCommand(CLIENT_ID, TRAINER_HOLDER_ID,
                Optional.of(new AssignmentNote("Keep the back straight")),
                List.of(new PrescriptionAdjustment(1, SQUAT_ID, new BigDecimal("50"), 90))), current.currentVersion());
        when(clientPlanCommandService.handle(argThat((PersonalizeAssignmentCommand command) ->
                command.clientId().equals(CLIENT_ID) && command.holderId().equals(TRAINER_HOLDER_ID)
                        && command.note().map(AssignmentNote::value).equals(Optional.of("Keep the back straight"))
                        && command.adjustments().size() == 1)))
                .thenReturn(Optional.of(plan));
        when(routineQueryService.handle(any(GetRoutineByIdQuery.class))).thenReturn(Optional.of(current));

        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/assignments/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Keep the back straight\",\"adjustments\":[{\"sessionOrder\":1,"
                                + "\"exerciseId\":\"" + SQUAT_ID.value() + "\",\"targetLoadKg\":50,\"restSeconds\":90}]}")
                        .with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("Keep the back straight"))
                .andExpect(jsonPath("$.adjustments[0].targetLoadKg").value(50))
                .andExpect(jsonPath("$.adjustments[0].restSeconds").value(90))
                .andExpect(jsonPath("$.current").value(true));
    }

    @Test
    void shouldReturn404WhenTheClientHasNoCurrentAssignment() throws Exception {
        when(clientPlanCommandService.handle(any(PersonalizeAssignmentCommand.class)))
                .thenThrow(new AssignmentNotFoundException());

        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/assignments/current")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Hi\"}").with(TRAINER))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn422ForAnAdjustmentWithNothingToChange() throws Exception {
        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/assignments/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adjustments\":[{\"sessionOrder\":1,\"exerciseId\":\"" + SQUAT_ID.value() + "\"}]}")
                        .with(TRAINER))
                .andExpect(status().isUnprocessableEntity());

        verifyNoInteractions(clientPlanCommandService);
    }

    @Test
    void shouldReturn400ForANoteThatIsTooLong() throws Exception {
        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/assignments/current")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"" + "x".repeat(501) + "\"}")
                        .with(TRAINER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenAClientPersonalizesAnAssignment() throws Exception {
        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/assignments/current")
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .with(user(TRAINER_HOLDER_ID).roles("CLIENT")))
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldRejectFractionalRestWithoutTruncatingIt() throws Exception {
        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/assignments/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adjustments\":[{\"sessionOrder\":1,\"exerciseId\":\"" + SQUAT_ID.value()
                                + "\",\"restSeconds\":1.5}]}").with(TRAINER))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(clientPlanCommandService);
    }

    @Test
    void shouldReturn404WhenTheAssignedRoutineIsUnavailableToTheTrainer() throws Exception {
        when(clientPlanCommandService.handle(any(PersonalizeAssignmentCommand.class)))
                .thenThrow(new RoutineNotFoundException());

        mockMvc.perform(put("/api/v1/clients/" + CLIENT_ID.value() + "/assignments/current")
                        .contentType(MediaType.APPLICATION_JSON).content("{}").with(TRAINER))
                .andExpect(status().isNotFound());
    }
}
