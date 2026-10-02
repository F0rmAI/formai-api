package com.formai.api.tracking.interfaces.rest;

import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import com.formai.api.tracking.domain.exceptions.ClientAccessDeniedException;
import com.formai.api.tracking.domain.model.queries.GetProgressReportQuery;
import com.formai.api.tracking.domain.model.valueobjects.Adherence;
import com.formai.api.tracking.domain.model.valueobjects.ExerciseMetric;
import com.formai.api.tracking.domain.model.valueobjects.Load;
import com.formai.api.tracking.domain.model.valueobjects.ProgressReport;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
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

@WebMvcTest(ProgressReportsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, WorkoutSessionAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class ProgressReportsControllerTest {

    private static final RequestPostProcessor TRAINER = user(TRAINER_HOLDER_ID).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_HOLDER_ID).roles("CLIENT");

    private static final String URL = "/api/v1/clients/" + CLIENT_ID.value() + "/progress-reports";
    private final ReportPeriod period = new ReportPeriod(TODAY.minusDays(27), TODAY);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WorkoutSessionQueryService workoutSessionQueryService;

    private static Load kg(String value) {
        return new Load(new BigDecimal(value));
    }

    @Test
    void shouldReturnTheClientsAdherenceAndExerciseMetrics() throws Exception {
        var squat = new ExerciseMetric(SQUAT.exerciseId(), "Squat", kg("65"), kg("70"),
                new TrainingVolume(new BigDecimal("1120")), new TrainingVolume(new BigDecimal("560")));
        when(workoutSessionQueryService.handle(argThat((GetProgressReportQuery query) ->
                query.clientId().equals(CLIENT_ID) && query.requesterHolderId().equals(TRAINER_HOLDER_ID)
                        && query.period().equals(period))))
                .thenReturn(new ProgressReport(CLIENT_ID, period, Adherence.of(1, 3), 3, 1, 1, 1, List.of(squat), true));

        mockMvc.perform(get(URL).param("from", period.from().toString()).param("to", period.to().toString()).with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adherencePercentage").value(33.33))
                .andExpect(jsonPath("$.scheduled").value(3))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.exercises[0].lastMaxLoadKg").value(70))
                .andExpect(jsonPath("$.hasData").value(true));
    }

    @Test
    void shouldReturn400WithoutAPeriod() throws Exception {
        mockMvc.perform(get(URL).param("from", TODAY.toString()).with(TRAINER))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("from", TODAY.toString()).param("to", TODAY.minusDays(1).toString()).with(TRAINER))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workoutSessionQueryService);
    }

    @Test
    void shouldReturn403ForAClientOfAnotherTrainer() throws Exception {
        when(workoutSessionQueryService.handle(any(GetProgressReportQuery.class)))
                .thenThrow(new ClientAccessDeniedException());

        mockMvc.perform(get(URL).param("from", period.from().toString()).param("to", period.to().toString()).with(TRAINER))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForAClientToken() throws Exception {
        mockMvc.perform(get(URL).param("from", period.from().toString()).param("to", period.to().toString()).with(CLIENT))
                .andExpect(status().isForbidden());

        verifyNoInteractions(workoutSessionQueryService);
    }
}
