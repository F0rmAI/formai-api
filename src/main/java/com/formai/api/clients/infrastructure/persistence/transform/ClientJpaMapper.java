package com.formai.api.clients.infrastructure.persistence.transform;

import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.entities.BodyProfile;
import com.formai.api.clients.domain.model.valueobjects.BodyWeight;
import com.formai.api.clients.domain.model.valueobjects.BodyWeightRecord;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.model.valueobjects.Height;
import com.formai.api.clients.domain.model.valueobjects.TrainingGoal;
import com.formai.api.clients.infrastructure.persistence.entities.BodyProfileEmbeddable;
import com.formai.api.clients.infrastructure.persistence.entities.BodyWeightRecordEmbeddable;
import com.formai.api.clients.infrastructure.persistence.entities.ClientJpaEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.UUID;

// The body profile is optional in the domain and split in two on the table (the profile
// columns plus the weight history rows), so it is mapped by hand after the rest.
@Mapper(componentModel = "spring")
public interface ClientJpaMapper {

    @Mapping(target = "bodyProfile", ignore = true)
    @Mapping(target = "weightHistory", ignore = true)
    ClientJpaEntity toEntity(Client client);

    @Mapping(target = "bodyProfile", ignore = true)
    Client toDomain(ClientJpaEntity entity);

    @AfterMapping
    default void mapBodyProfile(Client client, @MappingTarget ClientJpaEntity entity) {
        client.getBodyProfile().ifPresent(profile -> {
            var embeddable = new BodyProfileEmbeddable();
            embeddable.setGoal(profile.getGoal().value());
            embeddable.setHeightCm(profile.getHeight().centimeters());
            embeddable.setWeightKg(profile.getCurrentWeight().kilograms());
            embeddable.setRestrictions(profile.getRestrictions());
            entity.setBodyProfile(embeddable);
            var history = new ArrayList<BodyWeightRecordEmbeddable>();
            profile.getWeightHistory().forEach(record -> {
                var row = new BodyWeightRecordEmbeddable();
                row.setWeightKg(record.weight().kilograms());
                row.setRecordedOn(record.recordedOn());
                history.add(row);
            });
            entity.setWeightHistory(history);
        });
    }

    @AfterMapping
    default void mapBodyProfile(ClientJpaEntity entity, @MappingTarget Client client) {
        var embeddable = entity.getBodyProfile();
        if (embeddable == null || embeddable.getGoal() == null) {
            return;
        }
        var history = entity.getWeightHistory().stream()
                .map(row -> new BodyWeightRecord(new BodyWeight(row.getWeightKg()), row.getRecordedOn()))
                .toList();
        client.setBodyProfile(new BodyProfile(new TrainingGoal(embeddable.getGoal()),
                new Height(embeddable.getHeightCm()), new BodyWeight(embeddable.getWeightKg()),
                embeddable.getRestrictions(), history));
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(ClientId id) {
        return id == null ? null : id.value();
    }

    default ClientId mapClientId(UUID value) {
        return value == null ? null : new ClientId(value);
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
