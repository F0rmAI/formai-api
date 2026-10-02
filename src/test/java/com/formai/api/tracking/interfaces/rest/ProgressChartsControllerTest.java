package com.formai.api.tracking.interfaces.rest;

import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.model.queries.GetExerciseProgressQuery;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseProgress;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.ProgressPoint;
import com.formai.api.tracking.domain.model.valueobjects.ProgressWindow;
import com.formai.api.tracking.domain.model.valueobjects.TrainingVolume;
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

import java.math.BigDecimal;
import java.util.List;

import static com.formai.api.tracking.TrackingTestData.CLIENT_HOLDER_ID;
import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.SQUAT;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static com.formai.api.tracking.TrackingTestData.TRAINER_HOLDER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressChartsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WorkoutSessionAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class ProgressChartsControllerTest {

    private static final RequestPostProcessor TRAINER = user(TRAINER_HOLDER_ID).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_HOLDER_ID).roles("CLIENT");

    private static final String SQUAT_ID = SQUAT.exerciseId().value().toString();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WorkoutSessionQueryService workoutSessionQueryService;

    private static ProgressPoint point(int daysAgo, String maxLoadKg, String volumeKg) {
        return new ProgressPoint(TODAY.minusDays(daysAgo), new Load(new BigDecimal(maxLoadKg)),
                new TrainingVolume(new BigDecimal(volumeKg)));
    }

    @Test
    void shouldReturnTheClientsOwnChartForTheChosenWeeks() throws Exception {
        when(workoutSessionQueryService.handle(argThat((GetExerciseProgressQuery query) ->
                query.clientId().equals(CLIENT_ID) && query.window() == ProgressWindow.WEEKS_8)))
                .thenReturn(new ExerciseProgress(SQUAT.exerciseId(), ProgressWindow.WEEKS_8,
                        List.of(point(14, "65", "1120"), point(7, "70", "560")), true));

        mockMvc.perform(get("/api/v1/progress-charts/me").param("exerciseId", SQUAT_ID).param("weeks", "8").with(CLIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks").value(8))
                .andExpect(jsonPath("$.enoughData").value(true))
                .andExpect(jsonPath("$.points.length()").value(2))
                .andExpect(jsonPath("$.points[1].maxLoadKg").value(70));
    }

    @Test
    void shouldReturn400ForAPeriodOtherThan4812Weeks() throws Exception {
        mockMvc.perform(get("/api/v1/progress-charts/me").param("exerciseId", SQUAT_ID).param("weeks", "6").with(CLIENT))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workoutSessionQueryService);
    }

    @Test
    void shouldReturnTheChartOfTheTrainersClientWithTheNotEnoughDataFlag() throws Exception {
        when(workoutSessionQueryService.handle(argThat((GetExerciseProgressQuery query) ->
                query.requesterHolderId().equals(TRAINER_HOLDER_ID) && query.window() == ProgressWindow.WEEKS_4)))
                .thenReturn(new ExerciseProgress(SQUAT.exerciseId(), ProgressWindow.WEEKS_4,
                        List.of(point(3, "60", "600")), false));

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value() + "/progress-charts").param("exerciseId", SQUAT_ID).with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks").value(4))
                .andExpect(jsonPath("$.enoughData").value(false));
    }

    @Test
    void shouldReturn403ForAClientOfAnotherTrainer() throws Exception {
        when(workoutSessionQueryService.handle(any(GetExerciseProgressQuery.class)))
                .thenThrow(new ClientAccessDeniedException());

        mockMvc.perform(get("/api/v1/clients/" + CLIENT_ID.value() + "/progress-charts").param("exerciseId", SQUAT_ID).with(TRAINER))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenATrainerAsksForTheClientsOwnChart() throws Exception {
        mockMvc.perform(get("/api/v1/progress-charts/me").param("exerciseId", SQUAT_ID).with(TRAINER))
                .andExpect(status().isForbidden());
    }
}
