package com.formai.api.clients.domain.model.aggregates;

import com.formai.api.clients.domain.model.commands.RegisterClientCommand;
import com.formai.api.clients.domain.model.commands.UpdateBodyProfileCommand;
import com.formai.api.clients.domain.model.commands.UpdateClientCommand;
import com.formai.api.clients.domain.model.entities.BodyProfile;
import com.formai.api.clients.domain.model.valueobjects.ActivationTicket;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public class Client {

    private ClientId id;
    private String holderId;
    private FullName fullName;
    private Email email;
    private ClientStatus status;
    private BodyProfile bodyProfile;
    private Instant registeredAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Client() {
    }

    public static Client register(RegisterClientCommand command, ActivationTicket ticket) {
        var client = new Client();
        client.id = ticket.clientId();
        client.holderId = command.holderId();
        client.fullName = command.fullName();
        client.email = command.email();
        client.status = ClientStatus.INVITED;
        client.registeredAt = Instant.now();
        return client;
    }

    public void rename(UpdateClientCommand command) {
        this.fullName = command.fullName();
    }

    public boolean canRenewActivationCode() {
        return status == ClientStatus.INVITED;
    }

    public void activate() {
        if (status == ClientStatus.INVITED) {
            this.status = ClientStatus.ACTIVE;
        }
    }

    public void deactivate() {
        this.status = ClientStatus.INACTIVE;
    }

    public void updateBodyProfile(UpdateBodyProfileCommand command, LocalDate today) {
        if (bodyProfile == null) {
            bodyProfile = new BodyProfile(command.goal(), command.height(), command.restrictions());
        } else {
            bodyProfile.update(command.goal(), command.height(), command.restrictions());
        }
        bodyProfile.recordWeight(command.weight(), today);
    }

    public boolean isActive() {
        return status == ClientStatus.ACTIVE;
    }

    public ClientId getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public FullName getFullName() {
        return fullName;
    }

    public Email getEmail() {
        return email;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public Optional<BodyProfile> getBodyProfile() {
        return Optional.ofNullable(bodyProfile);
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setId(ClientId id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setFullName(FullName fullName) {
        this.fullName = fullName;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    public void setBodyProfile(BodyProfile bodyProfile) {
        this.bodyProfile = bodyProfile;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
