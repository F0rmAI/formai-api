package com.formai.api.clients.application.internal.queryservices;

import com.formai.api.clients.application.internal.outboundservices.acl.ExternalIamService;
import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.queries.GetClientByIdQuery;
import com.formai.api.clients.domain.model.queries.GetClientProfileQuery;
import com.formai.api.clients.domain.model.queries.GetClientsQuery;
import com.formai.api.clients.domain.model.valueobjects.ClientPage;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.repositories.ClientRepository;
import com.formai.api.clients.domain.services.ClientQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClientQueryServiceImpl implements ClientQueryService {

    private final ClientRepository clientRepository;
    private final ExternalIamService externalIamService;

    public ClientQueryServiceImpl(ClientRepository clientRepository, ExternalIamService externalIamService) {
        this.clientRepository = clientRepository;
        this.externalIamService = externalIamService;
    }

    @Override
    public ClientPage handle(GetClientsQuery query) {
        var page = clientRepository.findAllByHolderId(query.holderId(), query.search(), query.status(),
                query.pagination());
        page.items().forEach(this::showActivation);
        return page;
    }

    @Override
    public Optional<Client> handle(GetClientByIdQuery query) {
        return clientRepository.findByIdAndHolderId(query.clientId(), query.holderId())
                .map(client -> {
                    showActivation(client);
                    return client;
                });
    }

    @Override
    public Optional<Client> handle(GetClientProfileQuery query) {
        return clientRepository.findById(query.clientId());
    }

    private void showActivation(Client client) {
        if (client.getStatus() == ClientStatus.INVITED) {
            externalIamService.fetchActivatedEmail(client.getId()).ifPresent(client::activate);
        }
    }
}
