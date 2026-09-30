package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.planning.domain.model.commands.AssignRoutineCommand;
import com.formai.api.planning.domain.model.queries.GetClientPlanQuery;
import com.formai.api.planning.domain.model.queries.GetRoutineByIdQuery;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.services.ClientPlanQueryService;
import com.formai.api.planning.domain.services.RoutineQueryService;
import com.formai.api.planning.interfaces.rest.transform.ClientPlanAssemblerImpl;
import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;
import java.util.UUID;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.START_DATE;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.emptyPlan;
import static com.formai.api.planning.PlanningTestData.routine;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    ClientPlanQueryService clientPlanQueryService;

    @MockitoBean
    RoutineQueryService routineQueryService;

    @Test
    void shouldListTheClientsAssignmentsMostRecentFirst() throws Exception {
        var current = routine();
        var plan = emptyPlan();
        plan.assign(new AssignRoutineCommand(new RoutineId(UUID.randomUUID()), CLIENT_ID, TRAINER_HOLDER_ID, START_DATE));
        plan.assign(new AssignRoutineCommand(current.getId(), CLIENT_ID, TRAINER_HOLDER_ID, START_DATE.plusDays(30)));
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
}
