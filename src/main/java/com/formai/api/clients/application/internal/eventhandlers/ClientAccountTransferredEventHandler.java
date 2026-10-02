package com.formai.api.clients.application.internal.eventhandlers;

import com.formai.api.clients.domain.model.commands.TransferClientCommand;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.services.ClientCommandService;
import com.formai.api.iam.domain.model.events.ClientAccountTransferred;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// fallbackExecution: iam publishes its events outside a transaction; without it Spring drops them.
// Resilience: none, accepted. If this fails the account is active again but the client record
// stays with the previous trainer and the invited record stays INVITED. Nothing repairs it later:
// the new trainer renews the activation code and the client redeems it again.
@Component
public class ClientAccountTransferredEventHandler {

    private final ClientCommandService clientCommandService;

    public ClientAccountTransferredEventHandler(ClientCommandService clientCommandService) {
        this.clientCommandService = clientCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ClientAccountTransferred event) {
        clientCommandService.handle(new TransferClientCommand(new ClientId(event.invitedUserId()),
                new ClientId(event.existingUserId())));
    }
}
