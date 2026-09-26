package com.formai.iam.infrastructure.persistence.repositories;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.infrastructure.persistence.entities.UserJpaEntity;
import com.formai.iam.infrastructure.persistence.transform.UserJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    UserJpaRepository jpaRepository;

    @Mock
    UserJpaMapper mapper;

    @InjectMocks
    UserRepositoryImpl repository;

    @Test
    void shouldMapToEntitySaveAndMapBackWhenSaving() {
        // Arrange
        var user = new User();
        var entity = new UserJpaEntity();
        var savedEntity = new UserJpaEntity();
        var savedUser = new User();
        when(mapper.toEntity(user)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedUser);

        // Act & Assert
        assertThat(repository.save(user)).isSameAs(savedUser);
    }

    @Test
    void shouldReturnUserWhenIdExists() {
        var id = UUID.randomUUID();
        var entity = new UserJpaEntity();
        var user = new User();
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        assertThat(repository.findById(id)).contains(user);
    }

    @Test
    void shouldReturnUserWhenActivationCodeExists() {
        var entity = new UserJpaEntity();
        var user = new User();
        when(jpaRepository.findByActivationCode("ABCD2345")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        assertThat(repository.findByActivationCode("ABCD2345")).contains(user);
    }

    @Test
    void shouldReturnEmptyWhenPasswordResetTokenHashDoesNotExist() {
        when(jpaRepository.findByPasswordResetTokenHash("unknown")).thenReturn(Optional.empty());

        assertThat(repository.findByPasswordResetTokenHash("unknown")).isEmpty();
    }
}
