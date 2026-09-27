package com.formai.api.planning.infrastructure.persistence.transform;

import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.valueobjects.AssignmentPeriod;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.ClientPlanId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.infrastructure.persistence.entities.AssignmentEmbeddable;
import com.formai.api.planning.infrastructure.persistence.entities.ClientPlanJpaEntity;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientPlanJpaMapper {

    ClientPlanJpaEntity toEntity(ClientPlan clientPlan);

    ClientPlan toDomain(ClientPlanJpaEntity entity);

    default AssignmentEmbeddable toEmbeddable(Assignment assignment) {
        var embeddable = new AssignmentEmbeddable();
        embeddable.setRoutineId(assignment.getRoutineId().value());
        embeddable.setStartDate(assignment.getPeriod().startDate());
        embeddable.setEndDate(assignment.getPeriod().endDate());
        return embeddable;
    }

    default Assignment toAssignment(AssignmentEmbeddable embeddable) {
        return new Assignment(new RoutineId(embeddable.getRoutineId()),
                new AssignmentPeriod(embeddable.getStartDate(), embeddable.getEndDate()));
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(ClientPlanId id) {
        return id == null ? null : id.value();
    }

    default ClientPlanId mapClientPlanId(UUID value) {
        return value == null ? null : new ClientPlanId(value);
    }

    default UUID map(ClientId clientId) {
        return clientId == null ? null : clientId.value();
    }

    default ClientId mapClientId(UUID value) {
        return value == null ? null : new ClientId(value);
    }
}
