package com.formai.api.clients.infrastructure.persistence.repositories;

import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Pagination;
import com.formai.api.clients.infrastructure.persistence.entities.ClientJpaEntity;
import com.formai.api.clients.infrastructure.persistence.transform.ClientJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static com.formai.api.clients.ClientsTestData.CLIENT_EMAIL;
import static com.formai.api.clients.ClientsTestData.CLIENT_ID;
import static com.formai.api.clients.ClientsTestData.TRAINER_HOLDER_ID;
import static com.formai.api.clients.ClientsTestData.activeClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The client repository maps to the JPA entity, delegates to Spring Data and maps back,
// always scoping the trainer's clients by holderId.
@ExtendWith(MockitoExtension.class)
class ClientRepositoryImplTest {

    @Mock
    ClientJpaRepository jpaRepository;

    private final ClientJpaMapper mapper = Mappers.getMapper(ClientJpaMapper.class);

    @Test
    void shouldFindAClientOnlyWithinTheTrainersClients() {
        // Arrange
        var entity = mapper.toEntity(activeClient());
        when(jpaRepository.findByIdAndHolderId(CLIENT_ID.value(), TRAINER_HOLDER_ID)).thenReturn(Optional.of(entity));

        // Act
        var client = new ClientRepositoryImpl(jpaRepository, mapper).findByIdAndHolderId(CLIENT_ID, TRAINER_HOLDER_ID);

        // Assert
        assertThat(client).hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(CLIENT_ID));
    }

    @Test
    void shouldCheckDuplicatesByTheNormalizedEmail() {
        when(jpaRepository.existsByHolderIdAndEmail(TRAINER_HOLDER_ID, "luis@formai.com")).thenReturn(true);

        assertThat(new ClientRepositoryImpl(jpaRepository, mapper).existsByHolderIdAndEmail(TRAINER_HOLDER_ID,
                CLIENT_EMAIL)).isTrue();
    }

    @Test
    void shouldListTheTrainersClientsSortedByName() {
        var entity = mapper.toEntity(activeClient());
        when(jpaRepository.findAllByHolderId(eq(TRAINER_HOLDER_ID), eq(Optional.of("lu")), eq(Optional.of("ACTIVE")),
                any(Pageable.class)))
                .thenReturn(new PageImpl<ClientJpaEntity>(List.of(entity), PageRequest.of(0, 20), 1));

        var page = new ClientRepositoryImpl(jpaRepository, mapper).findAllByHolderId(TRAINER_HOLDER_ID,
                Optional.of("lu"), Optional.of(ClientStatus.ACTIVE), new Pagination(0, 20));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findAllByHolderId(eq(TRAINER_HOLDER_ID), eq(Optional.of("lu")),
                eq(Optional.of("ACTIVE")), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "fullName"));
        assertThat(page.items()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
    }
}
