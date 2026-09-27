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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ClientAssembler {

    ClientResource toResource(Client client);

    @Mapping(target = "content", source = "items")
    ClientPageResource toResource(ClientPage page);

    default RegisteredClientResource toResource(RegisteredClient registered) {
        var client = registered.client();
        var ticket = registered.ticket();
        return new RegisteredClientResource(client.getId().value(), client.getFullName().value(),
                client.getEmail().value(), client.getStatus().name(), ticket.code(), ticket.expiresAt());
    }

    default ActivationCodeResource toResource(ActivationTicket ticket) {
        return new ActivationCodeResource(ticket.clientId().value(), ticket.code(), ticket.expiresAt());
    }

    default BodyProfileResource toResource(BodyProfile profile) {
        return new BodyProfileResource(profile.getGoal().value(), profile.getHeight().centimeters(),
                profile.getCurrentWeight().kilograms(), profile.getRestrictions(),
                profile.getWeightHistory().stream().map(this::toResource).toList());
    }

    default BodyWeightRecordResource toResource(BodyWeightRecord record) {
        return new BodyWeightRecordResource(record.weight().kilograms(), record.recordedOn());
    }

    default RegisterClientCommand toCommand(String holderId, RegisterClientResource resource) {
        return new RegisterClientCommand(holderId, new FullName(resource.fullName()), new Email(resource.email()));
    }

    default UpdateClientCommand toCommand(UUID clientId, String holderId, UpdateClientResource resource) {
        return new UpdateClientCommand(new ClientId(clientId), holderId, new FullName(resource.fullName()));
    }

    // Height and BodyWeight check their own ranges and answer 422 naming the field.
    default UpdateBodyProfileCommand toCommand(UUID clientId, String holderId, UpdateBodyProfileResource resource) {
        var restrictions = Optional.ofNullable(resource.restrictions()).filter(value -> !value.isBlank())
                .map(String::strip).orElse(null);
        return new UpdateBodyProfileCommand(new ClientId(clientId), holderId, new TrainingGoal(resource.goal()),
                new Height(resource.heightCm()), new BodyWeight(resource.weightKg()), restrictions);
    }

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

    // required by MapStruct: single-field VOs need an explicit converter.
    default UUID map(ClientId id) {
        return id == null ? null : id.value();
    }

    default String map(FullName fullName) {
        return fullName == null ? null : fullName.value();
    }

    default String map(Email email) {
        return email == null ? null : email.value();
    }
}
