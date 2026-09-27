package com.formai.api.tracking.infrastructure.persistence.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formai.api.tracking.domain.model.aggregates.ActiveRoutine;
import com.formai.api.tracking.domain.model.valueobjects.ActiveRoutineId;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.RoutineDay;
import com.formai.api.tracking.domain.model.valueobjects.RoutineId;
import com.formai.api.tracking.infrastructure.persistence.entities.ActiveRoutineJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ActiveRoutineJpaMapper {

    ObjectMapper JSON = new ObjectMapper();
    TypeReference<List<RoutineDay>> ROUTINE_DAYS = new TypeReference<>() { };

    @Mapping(target = "daysJson", source = "days")
    ActiveRoutineJpaEntity toEntity(ActiveRoutine activeRoutine);

    @Mapping(target = "days", source = "daysJson")
    ActiveRoutine toDomain(ActiveRoutineJpaEntity entity);

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(ActiveRoutineId id) {
        return id == null ? null : id.value();
    }

    default ActiveRoutineId mapActiveRoutineId(UUID value) {
        return value == null ? null : new ActiveRoutineId(value);
    }

    default UUID map(ClientId clientId) {
        return clientId == null ? null : clientId.value();
    }

    default ClientId mapClientId(UUID value) {
        return value == null ? null : new ClientId(value);
    }

    default UUID map(RoutineId routineId) {
        return routineId == null ? null : routineId.value();
    }

    default RoutineId mapRoutineId(UUID value) {
        return value == null ? null : new RoutineId(value);
    }

    default String mapDays(List<RoutineDay> days) {
        try {
            return JSON.writeValueAsString(days);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the routine days", e);
        }
    }

    default List<RoutineDay> mapDaysJson(String daysJson) {
        try {
            return JSON.readValue(daysJson, ROUTINE_DAYS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read the stored routine days", e);
        }
    }
}
