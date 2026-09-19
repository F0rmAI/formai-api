package studio.quedena.template.profiles.domain.services;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.domain.model.commands.CreateProfileCommand;
import studio.quedena.template.profiles.domain.model.commands.UpdateProfileCommand;

public interface ProfileCommandService {

    Profile handle(CreateProfileCommand command);

    Profile handle(UpdateProfileCommand command);
}
