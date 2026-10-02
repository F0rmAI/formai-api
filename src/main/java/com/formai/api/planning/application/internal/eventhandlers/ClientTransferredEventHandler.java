package com.formai.api.planning.application.internal.eventhandlers;

import com.formai.api.clients.domain.model.events.ClientTransferred;
import com.formai.api.planning.domain.model.commands.TransferClientPlanCommand;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.services.ClientPlanCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

// Resilience: self-healing. If this fails the plan keeps the previous trainer and its open
// assignment. The first routine the new trainer assigns closes that assignment and takes the plan
// (ClientPlan.assign); until then the new trainer does not see the assignment history.
@Component
public class ClientTransferredEventHandler {

    private final ClientPlanCommandService clientPlanCommandService;

    public ClientTransferredEventHandler(ClientPlanCommandService clientPlanCommandService) {
        this.clientPlanCommandService = clientPlanCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ClientTransferred event) {
        clientPlanCommandService.handle(new TransferClientPlanCommand(new ClientId(event.clientId()),
                event.newHolderId(), LocalDate.now()));
    }
}
