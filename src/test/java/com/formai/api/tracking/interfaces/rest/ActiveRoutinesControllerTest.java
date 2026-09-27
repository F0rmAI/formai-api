package com.formai.api.tracking.interfaces.rest;

import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import com.formai.api.tracking.domain.exceptions.ActiveRoutineNotFoundException;
import com.formai.api.tracking.domain.model.queries.GetActiveRoutineQuery;
import com.formai.api.tracking.domain.model.valueobjects.TodayPlan;
import com.formai.api.tracking.domain.services.ActiveRoutineQueryService;
import com.formai.api.tracking.interfaces.rest.transform.ActiveRoutineAssemblerImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static com.formai.api.tracking.TrackingTestData.CLIENT_HOLDER_ID;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.activeRoutine;
import static com.formai.api.tracking.TrackingTestData.pendingSession;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActiveRoutinesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ActiveRoutineAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class ActiveRoutinesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ActiveRoutineQueryService activeRoutineQueryService;

    @Test
    @WithMockUser(username = CLIENT_HOLDER_ID)
    void shouldReturnTheRoutineWithTodaysSession() throws Exception {
        var today = pendingSession(TODAY);
        when(activeRoutineQueryService.handle(argThat((GetActiveRoutineQuery query) ->
                query.clientId().equals(CLIENT_ID))))
                .thenReturn(Optional.of(new TodayPlan(activeRoutine(), Optional.of(today))));

        mockMvc.perform(get("/api/v1/active-routines/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routineName").value("Strength 12 weeks"))
                .andExpect(jsonPath("$.todaySessionOrder").value(1))
                .andExpect(jsonPath("$.todayWorkoutSessionId").value(today.getId().value().toString()))
                .andExpect(jsonPath("$.sessions.length()").value(2))
                .andExpect(jsonPath("$.sessions[0].exercises[0].exerciseName").value("Squat"))
                .andExpect(jsonPath("$.sessions[0].exercises[0].targetLoadKg").value(60.0));
    }

    @Test
    @WithMockUser(username = CLIENT_HOLDER_ID)
    void shouldReturnNullTodayFieldsWhenNothingIsScheduledToday() throws Exception {
        when(activeRoutineQueryService.handle(any(GetActiveRoutineQuery.class)))
                .thenReturn(Optional.of(new TodayPlan(activeRoutine(), Optional.empty())));

        mockMvc.perform(get("/api/v1/active-routines/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todaySessionOrder").doesNotExist())
                .andExpect(jsonPath("$.todayWorkoutSessionId").doesNotExist());
    }

    @Test
    @WithMockUser(username = CLIENT_HOLDER_ID)
    void shouldReturn404WhenTheClientHasNoRoutine() throws Exception {
        when(activeRoutineQueryService.handle(any(GetActiveRoutineQuery.class)))
                .thenThrow(new ActiveRoutineNotFoundException());

        mockMvc.perform(get("/api/v1/active-routines/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectARequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/active-routines/me"))
                .andExpect(status().isForbidden());
    }
}
