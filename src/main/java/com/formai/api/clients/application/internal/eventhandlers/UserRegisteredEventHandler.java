package com.formai.api.clients.application.internal.eventhandlers;

import com.formai.api.clients.domain.model.commands.RegisterTrainerCommand;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.services.TrainerCommandService;
import com.formai.api.iam.domain.model.events.UserRegistered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// fallbackExecution: iam publishes its events outside a transaction; without it Spring drops them.
// Resilience: none, accepted. If this fails the Trainer is never created: the trainer's name only
// travels in this event (iam does not keep it), so it cannot be rebuilt later. Nothing reads
// Trainer yet, so there is no functional effect; revisit it when a feature needs the Trainer.
@Component
public class UserRegisteredEventHandler {

    private final TrainerCommandService trainerCommandService;

    public UserRegisteredEventHandler(TrainerCommandService trainerCommandService) {
        this.trainerCommandService = trainerCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(UserRegistered event) {
        trainerCommandService.handle(new RegisterTrainerCommand(event.holderId(), new FullName(event.fullName()),
                new Email(event.email())));
    }
}
