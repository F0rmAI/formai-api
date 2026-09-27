package com.formai.api.clients.infrastructure.persistence.repositories;

import com.formai.api.clients.infrastructure.persistence.entities.ClientJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, UUID> {

    List<String> ALL_STATUSES = List.of("INVITED", "ACTIVE", "INACTIVE");

    Optional<ClientJpaEntity> findByIdAndHolderId(UUID id, String holderId);

    boolean existsByHolderIdAndEmail(String holderId, String email);

    @Query("select c from ClientJpaEntity c where c.holderId = :holderId "
            + "and lower(c.fullName) like lower(concat('%', :search, '%')) and c.status in :statuses")
    Page<ClientJpaEntity> search(@Param("holderId") String holderId, @Param("search") String search,
                                 @Param("statuses") Collection<String> statuses, Pageable pagination);

    // No search matches every name, and no status matches all of them.
    default Page<ClientJpaEntity> findAllByHolderId(String holderId, Optional<String> search,
                                                   Optional<String> status, Pageable pagination) {
        return search(holderId, search.orElse(""), status.map(List::of).orElse(ALL_STATUSES), pagination);
    }
}
