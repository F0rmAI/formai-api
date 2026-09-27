package com.formai.api.tracking.infrastructure.persistence.repositories;

import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.infrastructure.persistence.entities.ActiveRoutineJpaEntity;
import com.formai.api.tracking.infrastructure.persistence.transform.ActiveRoutineJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.ROUTINE_ID;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveRoutineRepositoryImplTest {

    @Mock
    ActiveRoutineJpaRepository jpaRepository;

    @Mock
    ActiveRoutineJpaMapper mapper;

    @InjectMocks
    ActiveRoutineRepositoryImpl repository;

    @Test
    void shouldMapToEntitySaveAndMapBackWhenSaving() {
        // Arrange
        var routine = new ActiveRoutine();
        var entity = new ActiveRoutineJpaEntity();
        var savedEntity = new ActiveRoutineJpaEntity();
        var savedRoutine = new ActiveRoutine();
        when(mapper.toEntity(routine)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedRoutine);

        // Act & Assert
        assertThat(repository.save(routine)).isSameAs(savedRoutine);
    }

    @Test
    void shouldFindTheClientsRoutine() {
        var entity = new ActiveRoutineJpaEntity();
        var routine = new ActiveRoutine();
        when(jpaRepository.findByClientId(CLIENT_ID.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(routine);

        assertThat(repository.findByClientId(CLIENT_ID)).containsSame(routine);
    }

    @Test
    void shouldFindEveryClientFollowingARoutine() {
        var entity = new ActiveRoutineJpaEntity();
        var routine = new ActiveRoutine();
        when(jpaRepository.findAllByRoutineId(ROUTINE_ID.value())).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(routine);

        assertThat(repository.findAllByRoutineId(ROUTINE_ID)).containsExactly(routine);
    }

    @Test
    void shouldFindTheRoutinesActiveOnADate() {
        var entity = new ActiveRoutineJpaEntity();
        var routine = new ActiveRoutine();
        when(jpaRepository.findAllActiveOn(TODAY)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(routine);

        assertThat(repository.findAllActiveOn(TODAY)).containsExactly(routine);
    }
}
