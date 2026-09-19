package studio.quedena.template.profiles.infrastructure.persistence.repositories;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.infrastructure.persistence.entities.ProfileJpaEntity;
import studio.quedena.template.profiles.infrastructure.persistence.transform.ProfileJpaMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileRepositoryImplTest {

    @Mock
    ProfileJpaRepository jpaRepository;

    @Mock
    ProfileJpaMapper mapper;

    @InjectMocks
    ProfileRepositoryImpl repository;

    @Test
    void shouldSaveThroughMapperAndJpaRepository() {
        // Arrange
        var profile = Profile.createEmptyFor("holder-123");
        var entity = new ProfileJpaEntity();
        when(mapper.toEntity(profile)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(profile);

        // Act
        var result = repository.save(profile);

        // Assert
        assertThat(result).isEqualTo(profile);
    }

    @Test
    void shouldFindByHolderIdWhenPresent() {
        // Arrange
        var entity = new ProfileJpaEntity();
        var profile = Profile.createEmptyFor("holder-123");
        when(jpaRepository.findByHolderId("holder-123")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        // Act
        var result = repository.findByHolderId("holder-123");

        // Assert
        assertThat(result).contains(profile);
    }

    @Test
    void shouldReturnEmptyWhenHolderHasNoProfile() {
        // Arrange
        when(jpaRepository.findByHolderId("missing-holder")).thenReturn(Optional.empty());

        // Act
        var result = repository.findByHolderId("missing-holder");

        // Assert
        assertThat(result).isEmpty();
    }
}
