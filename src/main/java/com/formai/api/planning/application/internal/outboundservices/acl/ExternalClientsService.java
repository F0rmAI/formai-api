package com.formai.api.planning.application.internal.outboundservices.acl;

import com.formai.api.clients.interfaces.acl.ClientsContextFacade;
import com.formai.api.planning.domain.model.valueobjects.ClientId;
import org.springframework.stereotype.Service;

import java.util.Optional;

// ACL over clients' Open Host Service. Explicit bean name: tracking has its own
// ExternalClientsService.
@Service("planningExternalClientsService")
public class ExternalClientsService {

    private static final String ACTIVE = "ACTIVE";

    private final ClientsContextFacade clientsContextFacade;

    public ExternalClientsService(ClientsContextFacade clientsContextFacade) {
        this.clientsContextFacade = clientsContextFacade;
    }

    // Empty when the client does not exist or belongs to another trainer; otherwise whether
    // the client is active and can therefore receive a routine.
    public Optional<Boolean> isActiveClientOfTrainer(ClientId clientId, String holderId) {
        return clientsContextFacade.fetchClientOfTrainer(clientId.value(), holderId)
                .map(client -> ACTIVE.equals(client.status()));
    }
}
