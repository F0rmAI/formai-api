package com.formai.api.clients.infrastructure.persistence.transform;

import com.formai.api.clients.domain.model.aggregates.Trainer;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.model.valueobjects.TrainerId;
import com.formai.api.clients.infrastructure.persistence.entities.TrainerJpaEntity;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TrainerJpaMapper {

    TrainerJpaEntity toEntity(Trainer trainer);

    Trainer toDomain(TrainerJpaEntity entity);

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(TrainerId id) {
        return id == null ? null : id.value();
    }

    default TrainerId mapTrainerId(UUID value) {
        return value == null ? null : new TrainerId(value);
    }

    default String map(FullName fullName) {
        return fullName == null ? null : fullName.value();
    }

    default FullName mapFullName(String value) {
        return value == null ? null : new FullName(value);
    }

    default String map(Email email) {
        return email == null ? null : email.value();
    }

    default Email mapEmail(String value) {
        return value == null ? null : new Email(value);
    }
}
