package com.formai.api.planning.infrastructure.persistence.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formai.api.planning.domain.model.valueobjects.AssignmentNote;
import com.formai.api.planning.domain.model.valueobjects.ExerciseId;
import com.formai.api.planning.domain.model.valueobjects.PrescriptionAdjustment;
import com.formai.api.planning.domain.model.aggregates.ClientPlan;
import com.formai.api.planning.domain.model.entities.Assignment;
import com.formai.api.planning.domain.model.valueobjects.AssignmentPeriod;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.model.valueobjects.ClientPlanId;
import com.formai.api.planning.domain.model.valueobjects.RoutineId;
import com.formai.api.planning.domain.model.valueobjects.TrainingDays;
import com.formai.api.planning.infrastructure.persistence.entities.AssignmentEmbeddable;
import com.formai.api.planning.infrastructure.persistence.entities.ClientPlanJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientPlanJpaMapper {

    ObjectMapper JSON = new ObjectMapper();
    TypeReference<List<AdjustmentJson>> ADJUSTMENTS = new TypeReference<>() { };

    record AdjustmentJson(int sessionOrder, UUID exerciseId, BigDecimal targetLoadKg, Integer restSeconds) { }

    ClientPlanJpaEntity toEntity(ClientPlan clientPlan);

    ClientPlan toDomain(ClientPlanJpaEntity entity);

    @Mapping(target = "startDate", source = "period.startDate")
    @Mapping(target = "endDate", source = "period.endDate")
    @Mapping(target = "adjustmentsJson", source = "adjustments")
    AssignmentEmbeddable toEmbeddable(Assignment assignment);

    @Mapping(target = "period", source = ".", qualifiedByName = "toPeriod")
    @Mapping(target = "adjustments", source = "adjustmentsJson")
    Assignment toAssignment(AssignmentEmbeddable embeddable);

    default String writeAdjustments(List<PrescriptionAdjustment> adjustments) {
        var json = (adjustments == null ? List.<PrescriptionAdjustment>of() : adjustments).stream()
                .map(adjustment -> new AdjustmentJson(adjustment.sessionOrder(), adjustment.exerciseId().value(),
                        adjustment.targetLoadKg(), adjustment.restSeconds()))
                .toList();
        try {
            return JSON.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RoutineSessionsStorageException("Could not serialize the assignment adjustments", e);
        }
    }

    default List<PrescriptionAdjustment> readAdjustments(String adjustmentsJson) {
        if (adjustmentsJson == null || adjustmentsJson.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(adjustmentsJson, ADJUSTMENTS).stream()
                    .map(adjustment -> new PrescriptionAdjustment(adjustment.sessionOrder(),
                            new ExerciseId(adjustment.exerciseId()), adjustment.targetLoadKg(),
                            adjustment.restSeconds()))
                    .toList();
        } catch (JsonProcessingException e) {
            throw new RoutineSessionsStorageException("Could not read the stored assignment adjustments", e);
        }
    }

    default String map(AssignmentNote note) {
        return note == null ? null : note.value();
    }

    default AssignmentNote mapAssignmentNote(String value) {
        return value == null || value.isBlank() ? null : new AssignmentNote(value);
    }

    @Named("toPeriod")
    default AssignmentPeriod toPeriod(AssignmentEmbeddable embeddable) {
        return new AssignmentPeriod(embeddable.getStartDate(), embeddable.getEndDate());
    }

    default String map(TrainingDays trainingDays) {
        return trainingDays == null ? null : String.join(",", trainingDays.names());
    }

    default TrainingDays mapTrainingDays(String value) {
        return value == null ? null : TrainingDays.ofNames(List.of(value.split(",")));
    }

    default UUID map(RoutineId id) {
        return id == null ? null : id.value();
    }

    default RoutineId mapRoutineId(UUID value) {
        return value == null ? null : new RoutineId(value);
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
