package com.formai.api.clients.infrastructure.persistence.repositories;

import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.ClientPage;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Pagination;
import com.formai.api.clients.domain.repositories.ClientRepository;
import com.formai.api.clients.infrastructure.persistence.transform.ClientJpaMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ClientRepositoryImpl implements ClientRepository {

    private static final Sort BY_NAME = Sort.by(Sort.Direction.ASC, "fullName");

    private final ClientJpaRepository jpaRepository;
    private final ClientJpaMapper mapper;

    public ClientRepositoryImpl(ClientJpaRepository jpaRepository, ClientJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Client save(Client client) {
        var entity = mapper.toEntity(client);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Client> findByIdAndHolderId(ClientId id, String holderId) {
        return jpaRepository.findByIdAndHolderId(id.value(), holderId).map(mapper::toDomain);
    }

    @Override
    public Optional<Client> findById(ClientId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public ClientPage findAllByHolderId(String holderId, Optional<String> search, Optional<ClientStatus> status,
                                       Pagination pagination) {
        var pageRequest = PageRequest.of(pagination.page(), pagination.size(), BY_NAME);
        var page = jpaRepository.findAllByHolderId(holderId, search, status.map(Enum::name), pageRequest);
        return new ClientPage(page.getContent().stream().map(mapper::toDomain).toList(),
                pagination.page(), pagination.size(), page.getTotalElements(), page.getTotalPages());
    }
}
