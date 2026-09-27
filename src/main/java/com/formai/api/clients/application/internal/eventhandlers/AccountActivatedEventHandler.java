package com.formai.api.clients.application.internal.eventhandlers;

import com.formai.api.clients.domain.model.commands.ActivateClientCommand;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.services.ClientCommandService;
import com.formai.api.iam.domain.model.events.AccountActivated;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// The client activated their iam account in the app: the Client becomes ACTIVE. If this
// fails, ClientQueryService and the trainer's next write catch up with it. fallbackExecution:
// iam publishes its events without a transaction, and without it they would be dropped.
@Component
public class AccountActivatedEventHandler {

    private final ClientCommandService clientCommandService;

    public AccountActivatedEventHandler(ClientCommandService clientCommandService) {
        this.clientCommandService = clientCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AccountActivated event) {
        clientCommandService.handle(new ActivateClientCommand(new ClientId(event.userId())));
    }
}
