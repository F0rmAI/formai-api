package studio.quedena.template.profiles.infrastructure.persistence.repositories;

import studio.quedena.template.profiles.infrastructure.persistence.entities.ProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileJpaRepository extends JpaRepository<ProfileJpaEntity, UUID> {

    Optional<ProfileJpaEntity> findByHolderId(String holderId);
}
