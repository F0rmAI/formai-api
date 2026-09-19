package studio.quedena.template.profiles.application.internal.commandservices;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.domain.model.commands.CreateProfileCommand;
import studio.quedena.template.profiles.domain.model.commands.UpdateProfileCommand;
import studio.quedena.template.profiles.domain.repositories.ProfileRepository;
import studio.quedena.template.profiles.domain.services.ProfileCommandService;
import org.springframework.stereotype.Service;

@Service
public class ProfileCommandServiceImpl implements ProfileCommandService {

    private final ProfileRepository profileRepository;

    public ProfileCommandServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Profile handle(CreateProfileCommand command) {
        var profile = Profile.createEmptyFor(command.holderId());
        return profileRepository.save(profile);
    }

    // Self-heals: if the UserRegistered handler hasn't created the profile yet
    // (or failed after the iam commit), updating still creates it here instead of 404-ing.
    @Override
    public Profile handle(UpdateProfileCommand command) {
        var profile = profileRepository.findByHolderId(command.holderId())
                .orElseGet(() -> Profile.createEmptyFor(command.holderId()));
        profile.updatePersonalData(command.firstName(), command.lastName());
        return profileRepository.save(profile);
    }
}
