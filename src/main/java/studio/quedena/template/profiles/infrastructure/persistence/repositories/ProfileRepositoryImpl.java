package studio.quedena.template.profiles.infrastructure.persistence.repositories;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.domain.repositories.ProfileRepository;
import studio.quedena.template.profiles.infrastructure.persistence.transform.ProfileJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProfileRepositoryImpl implements ProfileRepository {

    private final ProfileJpaRepository jpaRepository;
    private final ProfileJpaMapper mapper;

    public ProfileRepositoryImpl(ProfileJpaRepository jpaRepository, ProfileJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Profile save(Profile profile) {
        var entity = mapper.toEntity(profile);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Profile> findByHolderId(String holderId) {
        return jpaRepository.findByHolderId(holderId).map(mapper::toDomain);
    }
}
