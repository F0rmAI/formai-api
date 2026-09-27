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
