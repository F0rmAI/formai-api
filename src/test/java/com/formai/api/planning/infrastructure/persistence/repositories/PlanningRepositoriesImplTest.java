package com.formai.api.planning.infrastructure.persistence.repositories;

import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.aggregates.Exercise;
import com.formai.api.planning.domain.model.aggregates.Routine;
import com.formai.api.planning.domain.model.valueobjects.ExerciseStatus;
import com.formai.api.planning.domain.model.valueobjects.Pagination;
import com.formai.api.planning.infrastructure.persistence.entities.ClientPlanJpaEntity;
import com.formai.api.planning.infrastructure.persistence.entities.ExerciseJpaEntity;
import com.formai.api.planning.infrastructure.persistence.entities.RoutineJpaEntity;
import com.formai.api.planning.infrastructure.persistence.transform.ClientPlanJpaMapper;
import com.formai.api.planning.infrastructure.persistence.transform.ExerciseJpaMapper;
import com.formai.api.planning.infrastructure.persistence.transform.RoutineJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static com.formai.api.planning.PlanningTestData.CLIENT_ID;
import static com.formai.api.planning.PlanningTestData.SQUAT_ID;
import static com.formai.api.planning.PlanningTestData.TRAINER_HOLDER_ID;
import static com.formai.api.planning.PlanningTestData.routine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The three planning repositories: each one maps to the JPA entity, delegates to Spring Data
// and maps back, always scoping trainer data by holderId.
@ExtendWith(MockitoExtension.class)
class PlanningRepositoriesImplTest {

    @Mock
    ExerciseJpaRepository exerciseJpaRepository;

    @Mock
    ExerciseJpaMapper exerciseMapper;

    @Mock
    RoutineJpaRepository routineJpaRepository;

    @Mock
    RoutineJpaMapper routineMapper;

    @Mock
    ClientPlanJpaRepository clientPlanJpaRepository;

    @Mock
    ClientPlanJpaMapper clientPlanMapper;

    @Test
    void shouldMapToEntitySaveAndMapBackWhenSavingAnExercise() {
        // Arrange
        var exercise = new Exercise();
        var entity = new ExerciseJpaEntity();
        var savedEntity = new ExerciseJpaEntity();
        var saved = new Exercise();
        when(exerciseMapper.toEntity(exercise)).thenReturn(entity);
        when(exerciseJpaRepository.save(entity)).thenReturn(savedEntity);
        when(exerciseMapper.toDomain(savedEntity)).thenReturn(saved);

        // Act & Assert
        assertThat(new ExerciseRepositoryImpl(exerciseJpaRepository, exerciseMapper).save(exercise)).isSameAs(saved);
    }

    @Test
    void shouldSearchTheTrainersCatalogSortedByName() {
        var entity = new ExerciseJpaEntity();
        var exercise = new Exercise();
        when(exerciseJpaRepository.findAllByHolderId(eq(TRAINER_HOLDER_ID), eq(Optional.of("squ")),
                eq(Optional.of("ACTIVE")), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(exerciseMapper.toDomain(entity)).thenReturn(exercise);

        var page = new ExerciseRepositoryImpl(exerciseJpaRepository, exerciseMapper).findAllByHolderId(
                TRAINER_HOLDER_ID, Optional.of("squ"), Optional.of(ExerciseStatus.ACTIVE), new Pagination(0, 20));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(exerciseJpaRepository).findAllByHolderId(eq(TRAINER_HOLDER_ID), eq(Optional.of("squ")),
                eq(Optional.of("ACTIVE")), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
        assertThat(page.items()).containsExactly(exercise);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldTellWhetherARoutineOfTheTrainerUsesAnExercise() {
        when(routineJpaRepository.existsByHolderIdAndExerciseId(TRAINER_HOLDER_ID, SQUAT_ID.value())).thenReturn(true);

        assertThat(new RoutineRepositoryImpl(routineJpaRepository, routineMapper)
                .existsByHolderIdAndExerciseId(TRAINER_HOLDER_ID, SQUAT_ID)).isTrue();
    }

    @Test
    void shouldFindOnlyTheTrainersRoutine() {
        var entity = new RoutineJpaEntity();
        var routine = new Routine();
        var id = routine().getId();
        when(routineJpaRepository.findByIdAndHolderId(id.value(), TRAINER_HOLDER_ID)).thenReturn(Optional.of(entity));
        when(routineMapper.toDomain(entity)).thenReturn(routine);

        assertThat(new RoutineRepositoryImpl(routineJpaRepository, routineMapper).findByIdAndHolderId(id, TRAINER_HOLDER_ID))
                .containsSame(routine);
    }

    @Test
    void shouldFindTheClientsPlan() {
        var entity = new ClientPlanJpaEntity();
        var plan = new ClientPlan();
        when(clientPlanJpaRepository.findByClientId(CLIENT_ID.value())).thenReturn(Optional.of(entity));
        when(clientPlanMapper.toDomain(entity)).thenReturn(plan);

        assertThat(new ClientPlanRepositoryImpl(clientPlanJpaRepository, clientPlanMapper).findByClientId(CLIENT_ID))
                .containsSame(plan);
    }
}
