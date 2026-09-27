package com.formai.api.tracking.infrastructure.persistence.repositories;

import com.formai.api.tracking.domain.model.aggregates.WorkoutSession;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.ReportPeriod;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionId;
import com.formai.api.tracking.domain.model.valueobjects.WorkoutSessionPage;
import com.formai.api.tracking.domain.repositories.WorkoutSessionRepository;
import com.formai.api.tracking.infrastructure.persistence.transform.WorkoutSessionJpaMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class WorkoutSessionRepositoryImpl implements WorkoutSessionRepository {

    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "scheduledFor");

    private final WorkoutSessionJpaRepository jpaRepository;
    private final WorkoutSessionJpaMapper mapper;

    public WorkoutSessionRepositoryImpl(WorkoutSessionJpaRepository jpaRepository, WorkoutSessionJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public WorkoutSession save(WorkoutSession session) {
        var entity = mapper.toEntity(session);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WorkoutSession> findByIdAndClientId(WorkoutSessionId id, ClientId clientId) {
        return jpaRepository.findByIdAndClientId(id.value(), clientId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<WorkoutSession> findByClientIdAndScheduledFor(ClientId clientId, LocalDate date) {
        return jpaRepository.findByClientIdAndScheduledFor(clientId.value(), date).map(mapper::toDomain);
    }

    @Override
    public WorkoutSessionPage findAllByClientId(ClientId clientId, Optional<ReportPeriod> period,
                                                Pagination pagination) {
        var pageRequest = PageRequest.of(pagination.page(), pagination.size(), MOST_RECENT_FIRST);
        var page = jpaRepository.findAllByClientId(clientId.value(), period, pageRequest);
        return new WorkoutSessionPage(page.getContent().stream().map(mapper::toDomain).toList(),
                pagination.page(), pagination.size(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public Optional<WorkoutSession> findLastFinishedByClientId(ClientId clientId) {
        return jpaRepository.findLastFinishedByClientId(clientId.value()).map(mapper::toDomain);
    }

    @Override
    public List<WorkoutSession> findAllPendingBefore(LocalDate date) {
        return jpaRepository.findAllPendingBefore(date).stream().map(mapper::toDomain).toList();
    }
}
