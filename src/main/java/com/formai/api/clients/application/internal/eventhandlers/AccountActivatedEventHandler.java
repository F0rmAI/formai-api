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

// fallbackExecution: iam publishes its events outside a transaction; without it Spring drops them.
// Resilience: self-healing. If this fails the Client stays INVITED with an account that is already
// active: ClientQueryService notices it when reading (iam's account status) and the trainer's next
// write on that client runs ActivateClient first.
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
