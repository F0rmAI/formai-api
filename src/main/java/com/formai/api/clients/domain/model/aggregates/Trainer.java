package com.formai.api.clients.domain.model.aggregates;

import com.formai.api.clients.domain.model.commands.RegisterTrainerCommand;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.model.valueobjects.TrainerId;

import java.time.Instant;
import java.util.UUID;

// The trainer's profile in clients, created once their iam account is registered.
public class Trainer {

    private TrainerId id;
    private String holderId;
    private FullName fullName;
    private Email email;
    private Instant registeredAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Trainer() {
    }

    public static Trainer register(RegisterTrainerCommand command) {
        var trainer = new Trainer();
        trainer.id = new TrainerId(UUID.randomUUID());
        trainer.holderId = command.holderId();
        trainer.fullName = command.fullName();
        trainer.email = command.email();
        trainer.registeredAt = Instant.now();
        return trainer;
    }

    public TrainerId getId() {
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

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setId(TrainerId id) {
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

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
