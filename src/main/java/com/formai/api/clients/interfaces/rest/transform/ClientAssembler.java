package com.formai.api.clients.interfaces.rest.transform;

import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.commands.RegisterClientCommand;
import com.formai.api.clients.domain.model.commands.UpdateBodyProfileCommand;
import com.formai.api.clients.domain.model.commands.UpdateClientCommand;
import com.formai.api.clients.domain.model.entities.BodyProfile;
import com.formai.api.clients.domain.model.queries.GetClientsQuery;
import com.formai.api.clients.domain.model.valueobjects.ActivationTicket;
import com.formai.api.clients.domain.model.valueobjects.BodyWeight;
import com.formai.api.clients.domain.model.valueobjects.BodyWeightRecord;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.ClientPage;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.model.valueobjects.Height;
import com.formai.api.clients.domain.model.valueobjects.Pagination;
import com.formai.api.clients.domain.model.valueobjects.RegisteredClient;
import com.formai.api.clients.domain.model.valueobjects.TrainingGoal;
import com.formai.api.clients.interfaces.rest.resources.ActivationCodeResource;
import com.formai.api.clients.interfaces.rest.resources.BodyProfileResource;
import com.formai.api.clients.interfaces.rest.resources.BodyWeightRecordResource;
import com.formai.api.clients.interfaces.rest.resources.ClientPageResource;
import com.formai.api.clients.interfaces.rest.resources.ClientResource;
import com.formai.api.clients.interfaces.rest.resources.RegisterClientResource;
import com.formai.api.clients.interfaces.rest.resources.RegisteredClientResource;
import com.formai.api.clients.interfaces.rest.resources.UpdateBodyProfileResource;
import com.formai.api.clients.interfaces.rest.resources.UpdateClientResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientAssembler {

    ClientResource toResource(Client client);

    @Mapping(target = "content", source = "items")
    ClientPageResource toResource(ClientPage page);

    @Mapping(target = "id", source = "client.id")
    @Mapping(target = "fullName", source = "client.fullName")
    @Mapping(target = "status", source = "client.status")
    @Mapping(target = "activationCode", source = "ticket.code")
    @Mapping(target = "activationCodeExpiresAt", source = "ticket.expiresAt")
    RegisteredClientResource toResource(RegisteredClient registered);

    @Mapping(target = "activationCode", source = "code")
    ActivationCodeResource toResource(ActivationTicket ticket);

    @Mapping(target = "heightCm", source = "height.centimeters")
    @Mapping(target = "weightKg", source = "currentWeight.kilograms")
    BodyProfileResource toResource(BodyProfile profile);

    @Mapping(target = "weightKg", source = "weight.kilograms")
    BodyWeightRecordResource toResource(BodyWeightRecord record);

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "fullName", source = "resource.fullName")
    RegisterClientCommand toCommand(String holderId, RegisterClientResource resource);

    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "fullName", source = "resource.fullName")
    UpdateClientCommand toCommand(UUID clientId, String holderId, UpdateClientResource resource);

    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "goal", source = "resource.goal")
    @Mapping(target = "height", source = "resource.heightCm")
    @Mapping(target = "weight", source = "resource.weightKg")
    @Mapping(target = "restrictions", source = "resource.restrictions", qualifiedByName = "optionalText")
    UpdateBodyProfileCommand toCommand(UUID clientId, String holderId, UpdateBodyProfileResource resource);

    default GetClientsQuery toQuery(String holderId, String search, String status, int page, int size) {
        return new GetClientsQuery(holderId, Optional.ofNullable(search).filter(value -> !value.isBlank()),
                toStatus(status), new Pagination(page, size));
    }

    default Optional<ClientStatus> toStatus(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(ClientStatus.values())
                .filter(value -> value.name().equalsIgnoreCase(status))
                .findFirst()
                .map(Optional::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Status must be INVITED, ACTIVE or INACTIVE"));
    }

    @Named("optionalText")
    default String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
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

    default String map(TrainingGoal goal) {
        return goal == null ? null : goal.value();
    }

    default TrainingGoal mapTrainingGoal(String value) {
        return new TrainingGoal(value);
    }

    default Height mapHeight(Integer centimeters) {
        return new Height(centimeters);
    }

    default BodyWeight mapBodyWeight(BigDecimal kilograms) {
        return new BodyWeight(kilograms);
    }
}
