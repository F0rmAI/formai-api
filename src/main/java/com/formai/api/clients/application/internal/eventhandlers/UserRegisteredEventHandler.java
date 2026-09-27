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

// One transaction, one aggregate: the sign-up only creates the iam User; once that is
// committed, this creates the Trainer with the name given at sign-up. fallbackExecution:
// iam publishes its events without a transaction, and without it they would be dropped.
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
