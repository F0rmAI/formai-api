package com.formai.api.tracking.application.internal.outboundservices.acl;

import com.formai.api.clients.interfaces.acl.ClientsContextFacade;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import org.springframework.stereotype.Service;

// ACL over clients' Open Host Service. Explicit bean name: planning has its own
// ExternalClientsService.
@Service("trackingExternalClientsService")
public class ExternalClientsService {

    private final ClientsContextFacade clientsContextFacade;

    public ExternalClientsService(ClientsContextFacade clientsContextFacade) {
        this.clientsContextFacade = clientsContextFacade;
    }

    public boolean isClientOfTrainer(ClientId clientId, String holderId) {
        return clientsContextFacade.fetchClientOfTrainer(clientId.value(), holderId).isPresent();
    }
}
