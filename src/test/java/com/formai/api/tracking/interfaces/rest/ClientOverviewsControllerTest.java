package com.formai.api.tracking.interfaces.rest;

import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import com.formai.api.tracking.domain.model.queries.GetClientOverviewsQuery;
import com.formai.api.tracking.domain.model.valueobjects.ClientOverview;
import com.formai.api.tracking.domain.model.valueobjects.ClientOverviewPage;
import com.formai.api.tracking.domain.services.WorkoutSessionQueryService;
import com.formai.api.tracking.interfaces.rest.transform.WorkoutSessionAssemblerImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static com.formai.api.tracking.TrackingTestData.CLIENT_HOLDER_ID;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.TRAINER_HOLDER_ID;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientOverviewsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WorkoutSessionAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class ClientOverviewsControllerTest {

    private static final RequestPostProcessor TRAINER = user(TRAINER_HOLDER_ID).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_HOLDER_ID).roles("CLIENT");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WorkoutSessionQueryService workoutSessionQueryService;

    @Test
    void shouldListTheTrainersClientsWithRoutineAndLastWorkout() throws Exception {
        when(workoutSessionQueryService.handle(argThat((GetClientOverviewsQuery query) ->
                query.holderId().equals(TRAINER_HOLDER_ID) && query.search().equals(Optional.of("lu"))
                        && query.status().equals(Optional.of("ACTIVE")))))
                .thenReturn(new ClientOverviewPage(List.of(
                        new ClientOverview(CLIENT_ID, "Luis Ramos", "ACTIVE", Optional.of("Strength 12 weeks"),
                                Optional.of(TODAY)),
                        new ClientOverview(CLIENT_ID, "Luis Ramos Jr.", "ACTIVE", Optional.empty(), Optional.empty())),
                        0, 20, 2, 1));

        mockMvc.perform(get("/api/v1/client-overviews").param("search", "lu").param("status", "ACTIVE").with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activeRoutineName").value("Strength 12 weeks"))
                .andExpect(jsonPath("$.content[0].lastWorkoutOn").value(TODAY.toString()))
                .andExpect(jsonPath("$.content[1].activeRoutineName").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldReturn403ForAClientToken() throws Exception {
        mockMvc.perform(get("/api/v1/client-overviews").with(CLIENT))
                .andExpect(status().isForbidden());

        verifyNoInteractions(workoutSessionQueryService);
    }
}
