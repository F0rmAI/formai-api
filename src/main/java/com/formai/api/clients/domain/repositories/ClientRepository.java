package com.formai.api.clients.domain.repositories;

import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.ClientPage;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.Pagination;

import java.util.Optional;

public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findByIdAndHolderId(ClientId id, String holderId);

    Optional<Client> findById(ClientId id);

    boolean existsByHolderIdAndEmail(String holderId, Email email);

    // Sorted by full name.
    ClientPage findAllByHolderId(String holderId, Optional<String> search, Optional<ClientStatus> status,
                                 Pagination pagination);
}
