package com.formai.api.clients.interfaces.acl;

import com.formai.api.shared.contracts.clients.ClientListRequest;
import com.formai.api.shared.contracts.clients.ClientSummary;
import com.formai.api.shared.contracts.clients.ClientSummaryPage;

import java.util.Optional;
import java.util.UUID;

public interface ClientsContextFacade {

    Optional<ClientSummary> fetchClientOfTrainer(UUID clientId, String trainerHolderId);

    ClientSummaryPage fetchClientsOfTrainer(ClientListRequest request);
}
