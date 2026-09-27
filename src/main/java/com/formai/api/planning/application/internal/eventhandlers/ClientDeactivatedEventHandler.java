package com.formai.api.planning.application.internal.eventhandlers;

import com.formai.api.clients.domain.model.events.ClientDeactivated;
import com.formai.api.planning.domain.model.commands.CloseAssignmentCommand;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import com.formai.api.planning.domain.services.ClientPlanCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

// A deactivated client keeps its history, but its current assignment is closed today;
// tracking then ends the client's active routine through AssignmentClosed.
@Component
public class ClientDeactivatedEventHandler {

    private final ClientPlanCommandService clientPlanCommandService;

    public ClientDeactivatedEventHandler(ClientPlanCommandService clientPlanCommandService) {
        this.clientPlanCommandService = clientPlanCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ClientDeactivated event) {
        clientPlanCommandService.handle(new CloseAssignmentCommand(new ClientId(event.clientId()), LocalDate.now()));
    }
}
