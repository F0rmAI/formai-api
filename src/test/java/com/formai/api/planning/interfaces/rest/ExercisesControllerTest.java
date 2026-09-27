package com.formai.api.planning.interfaces.rest;

import com.formai.api.planning.domain.exceptions.ExerciseAlreadyExistsException;
import com.formai.api.planning.domain.exceptions.ExerciseInUseException;
import com.formai.api.planning.domain.model.commands.ArchiveExerciseCommand;
import com.formai.api.planning.domain.model.commands.CreateExerciseCommand;
import com.formai.api.planning.domain.model.commands.DeleteExerciseCommand;
import com.formai.api.planning.domain.model.commands.RestoreExerciseCommand;
import com.formai.api.planning.domain.model.queries.GetExerciseByIdQuery;
import com.formai.api.planning.domain.model.queries.GetExercisesQuery;
import com.formai.api.planning.domain.model.valueobjects.ExercisePage;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.services.ExerciseCommandService;
import com.formai.api.planning.domain.services.ExerciseQueryService;
import com.formai.api.planning.interfaces.rest.transform.ExerciseAssemblerImpl;
import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.squat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExercisesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ExerciseAssemblerImpl.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
@WithMockUser(username = TRAINER_HOLDER_ID)
class ExercisesControllerTest {

    private static final String SQUAT_BODY = "{\"name\":\"Squat\",\"muscleGroup\":\"Legs\",\"equipment\":\"Barbell\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExerciseCommandService exerciseCommandService;

    @MockitoBean
    ExerciseQueryService exerciseQueryService;

    @Test
    void shouldReturn201WhenAnExerciseIsCreated() throws Exception {
        when(exerciseCommandService.handle(argThat((CreateExerciseCommand command) ->
                command.holderId().equals(TRAINER_HOLDER_ID) && command.name().value().equals("Squat"))))
                .thenReturn(Optional.of(squat()));

        mockMvc.perform(post("/api/v1/exercises").contentType(MediaType.APPLICATION_JSON).content(SQUAT_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SQUAT_ID.value().toString()))
                .andExpect(jsonPath("$.muscleGroup").value("Legs"))
                .andExpect(jsonPath("$.equipment").value("Barbell"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn400WhenTheNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/exercises").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"muscleGroup\":\"Legs\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(exerciseCommandService);
    }

    @Test
    void shouldReturn409ForADuplicateName() throws Exception {
        when(exerciseCommandService.handle(any(CreateExerciseCommand.class)))
                .thenThrow(new ExerciseAlreadyExistsException("Squat"));

        mockMvc.perform(post("/api/v1/exercises").contentType(MediaType.APPLICATION_JSON).content(SQUAT_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListTheCatalogFilteredBySearchAndStatus() throws Exception {
        when(exerciseQueryService.handle(argThat((GetExercisesQuery query) ->
                query.search().equals(Optional.of("squ")) && query.status().equals(Optional.of(ExerciseStatus.ACTIVE)))))
                .thenReturn(new ExercisePage(List.of(squat()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/exercises").param("search", "squ").param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Squat"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturn400ForAnUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/v1/exercises").param("status", "DELETED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForAnExerciseOfAnotherTrainer() throws Exception {
        when(exerciseQueryService.handle(any(GetExerciseByIdQuery.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/exercises/" + SQUAT_ID.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn204WhenAnUnusedExerciseIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/v1/exercises/" + SQUAT_ID.value()))
                .andExpect(status().isNoContent());

        verify(exerciseCommandService).handle(new DeleteExerciseCommand(SQUAT_ID, TRAINER_HOLDER_ID));
    }

    @Test
    void shouldReturn409WhenDeletingAnExerciseInUse() throws Exception {
        doThrow(new ExerciseInUseException()).when(exerciseCommandService).handle(any(DeleteExerciseCommand.class));

        mockMvc.perform(delete("/api/v1/exercises/" + SQUAT_ID.value()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldArchiveAndRestore() throws Exception {
        var archived = squat();
        archived.archive();
        when(exerciseCommandService.handle(any(ArchiveExerciseCommand.class))).thenReturn(Optional.of(archived));
        when(exerciseCommandService.handle(any(RestoreExerciseCommand.class))).thenReturn(Optional.of(squat()));

        mockMvc.perform(post("/api/v1/exercises/" + SQUAT_ID.value() + "/archivals"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mockMvc.perform(post("/api/v1/exercises/" + SQUAT_ID.value() + "/restorations"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithAnonymousUser
    void shouldRejectARequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/exercises"))
                .andExpect(status().isForbidden());
    }
}
