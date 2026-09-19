package studio.quedena.template.profiles.interfaces.rest.transform;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.domain.model.commands.UpdateProfileCommand;
import studio.quedena.template.profiles.interfaces.rest.resources.ProfileResource;
import studio.quedena.template.profiles.interfaces.rest.resources.UpdateProfileResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileAssembler {

    default UpdateProfileCommand toCommand(UpdateProfileResource resource, String holderId) {
        return new UpdateProfileCommand(holderId, resource.firstName(), resource.lastName());
    }

    ProfileResource toResource(Profile profile);
}
