package com.formai.api.tracking.infrastructure.persistence.repositories;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.infrastructure.persistence.entities.WorkoutSessionJpaEntity;
import com.formai.api.tracking.infrastructure.persistence.transform.WorkoutSessionJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.tracking.TrackingTestData.CLIENT_ID;
import static com.formai.api.tracking.TrackingTestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionRepositoryImplTest {

    @Mock
    WorkoutSessionJpaRepository jpaRepository;

    @Mock
    WorkoutSessionJpaMapper mapper;

    @InjectMocks
    WorkoutSessionRepositoryImpl repository;

    @Test
    void shouldMapToEntitySaveAndMapBackWhenSaving() {
        // Arrange
        var session = new WorkoutSession();
        var entity = new WorkoutSessionJpaEntity();
        var savedEntity = new WorkoutSessionJpaEntity();
        var savedSession = new WorkoutSession();
        when(mapper.toEntity(session)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedSession);

        // Act & Assert
        assertThat(repository.save(session)).isSameAs(savedSession);
    }

    @Test
    void shouldPageTheHistoryMostRecentFirst() {
        var period = Optional.of(new ReportPeriod(TODAY.minusDays(30), TODAY));
        var entity = new WorkoutSessionJpaEntity();
        var session = new WorkoutSession();
        when(jpaRepository.findAllByClientId(eq(CLIENT_ID.value()), eq(period), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 10), 11));
        when(mapper.toDomain(entity)).thenReturn(session);

        var page = repository.findAllByClientId(CLIENT_ID, period, new Pagination(1, 10));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findAllByClientId(eq(CLIENT_ID.value()), eq(period), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "scheduledFor"));
        assertThat(page.items()).containsExactly(session);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldReturnTheSessionOnlyWhenItBelongsToTheClient() {
        var entity = new WorkoutSessionJpaEntity();
        var session = new WorkoutSession();
        var id = new WorkoutSessionId(UUID.randomUUID());
        when(jpaRepository.findByIdAndClientId(id.value(), CLIENT_ID.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(session);

        assertThat(repository.findByIdAndClientId(id, CLIENT_ID)).containsSame(session);
    }
}
