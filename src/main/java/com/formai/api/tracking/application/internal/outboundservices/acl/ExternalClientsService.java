package com.formai.api.tracking.application.internal.outboundservices.acl;

import com.formai.api.clients.interfaces.acl.ClientsContextFacade;
import com.formai.api.shared.contracts.clients.ClientListRequest;
import com.formai.api.tracking.domain.model.valueobjects.ClientId;
import com.formai.api.tracking.domain.model.valueobjects.Pagination;
import com.formai.api.tracking.domain.model.valueobjects.TrainerClient;
import com.formai.api.tracking.domain.model.valueobjects.TrainerClientPage;
import org.springframework.stereotype.Service;

import java.util.Optional;

// Explicit bean name: planning has its own ExternalClientsService.
@Service("trackingExternalClientsService")
public class ExternalClientsService {

    private final ClientsContextFacade clientsContextFacade;

    public ExternalClientsService(ClientsContextFacade clientsContextFacade) {
        this.clientsContextFacade = clientsContextFacade;
    }

    public boolean isClientOfTrainer(ClientId clientId, String holderId) {
        return clientsContextFacade.fetchClientOfTrainer(clientId.value(), holderId).isPresent();
    }

    public TrainerClientPage fetchClientsOfTrainer(String holderId, Optional<String> search, Optional<String> status,
                                                   Pagination pagination) {
        var page = clientsContextFacade.fetchClientsOfTrainer(new ClientListRequest(holderId, search, status,
                pagination.page(), pagination.size()));
        return new TrainerClientPage(page.items().stream()
                .map(client -> new TrainerClient(new ClientId(client.clientId()), client.fullName(), client.status()))
                .toList(), page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
