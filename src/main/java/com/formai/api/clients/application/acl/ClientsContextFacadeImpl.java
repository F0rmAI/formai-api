package com.formai.api.clients.application.acl;

import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.queries.GetClientByIdQuery;
import com.formai.api.clients.domain.model.queries.GetClientsQuery;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Pagination;
import com.formai.api.clients.domain.services.ClientQueryService;
import com.formai.api.clients.interfaces.acl.ClientsContextFacade;
import com.formai.api.shared.contracts.clients.ClientListRequest;
import com.formai.api.shared.contracts.clients.ClientSummary;
import com.formai.api.shared.contracts.clients.ClientSummaryPage;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientsContextFacadeImpl implements ClientsContextFacade {

    private final ClientQueryService clientQueryService;

    public ClientsContextFacadeImpl(ClientQueryService clientQueryService) {
        this.clientQueryService = clientQueryService;
    }

    @Override
    public Optional<ClientSummary> fetchClientOfTrainer(UUID clientId, String trainerHolderId) {
        return clientQueryService.handle(new GetClientByIdQuery(new ClientId(clientId), trainerHolderId))
                .map(this::toSummary);
    }

    @Override
    public ClientSummaryPage fetchClientsOfTrainer(ClientListRequest request) {
        var status = request.status().map(ClientsContextFacadeImpl::toStatus);
        if (status.isPresent() && status.get().isEmpty()) {
            return new ClientSummaryPage(List.of(), request.page(), request.size(), 0, 0);
        }
        var page = clientQueryService.handle(new GetClientsQuery(request.trainerHolderId(), request.search(),
                status.flatMap(value -> value), new Pagination(request.page(), request.size())));
        return new ClientSummaryPage(page.items().stream().map(this::toSummary).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    private ClientSummary toSummary(Client client) {
        // The email is empty until the client activates the account.
        var email = client.getEmail() == null ? null : client.getEmail().value();
        return new ClientSummary(client.getId().value(), client.getFullName().value(), email,
                client.getStatus().name());
    }

    private static Optional<ClientStatus> toStatus(String status) {
        return Arrays.stream(ClientStatus.values())
                .filter(value -> value.name().equalsIgnoreCase(status))
                .findFirst();
    }
}
