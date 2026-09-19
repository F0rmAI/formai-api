package studio.quedena.template.profiles.application.internal.queryservices;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.domain.model.queries.GetProfileByHolderIdQuery;
import studio.quedena.template.profiles.domain.repositories.ProfileRepository;
import studio.quedena.template.profiles.domain.services.ProfileQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileQueryServiceImpl implements ProfileQueryService {

    private final ProfileRepository profileRepository;

    public ProfileQueryServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Optional<Profile> handle(GetProfileByHolderIdQuery query) {
        return profileRepository.findByHolderId(query.holderId());
    }
}
