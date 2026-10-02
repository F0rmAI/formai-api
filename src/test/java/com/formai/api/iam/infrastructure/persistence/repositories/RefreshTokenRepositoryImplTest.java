package com.formai.api.iam.infrastructure.persistence.repositories;

import com.formai.api.iam.domain.model.aggregates.RefreshToken;
import com.formai.api.iam.domain.model.commands.IssueRefreshTokenCommand;
import com.formai.api.iam.infrastructure.persistence.entities.RefreshTokenJpaEntity;
import com.formai.api.iam.infrastructure.persistence.transform.RefreshTokenJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryImplTest {

    @Mock
    RefreshTokenJpaRepository jpaRepository;

    @Mock
    RefreshTokenJpaMapper mapper;

    @InjectMocks
    RefreshTokenRepositoryImpl repository;

    @Test
    void shouldPersistTheEntityAndKeepTheRawValueOnTheReturnedToken() {
        var token = RefreshToken.issue(new IssueRefreshTokenCommand(UUID.randomUUID()), Instant.now(), Duration.ofDays(7));
        var entity = new RefreshTokenJpaEntity();
        when(mapper.toEntity(token)).thenReturn(entity);

        var saved = repository.save(token);

        verify(jpaRepository).save(entity);
        assertThat(saved.getRawValue()).isPresent();
    }

    @Test
    void shouldFindATokenByItsHash() {
        var entity = new RefreshTokenJpaEntity();
        var token = new RefreshToken();
        when(jpaRepository.findByTokenHash("hash")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(token);

        assertThat(repository.findByTokenHash("hash")).contains(token);
    }

    @Test
    void shouldListTheTokensNotRevokedOfAUser() {
        var userId = UUID.randomUUID();
        var entity = new RefreshTokenJpaEntity();
        var token = new RefreshToken();
        when(jpaRepository.findAllByUserIdAndRevokedAtIsNull(userId)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(token);

        assertThat(repository.findAllNotRevokedByUserId(userId)).containsExactly(token);
    }
}
