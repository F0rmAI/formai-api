package studio.quedena.template.profiles.domain.repositories;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;

import java.util.Optional;

public interface ProfileRepository {

    Profile save(Profile profile);

    Optional<Profile> findByHolderId(String holderId);
}
