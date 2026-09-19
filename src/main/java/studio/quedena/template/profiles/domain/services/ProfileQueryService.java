package studio.quedena.template.profiles.domain.services;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.domain.model.queries.GetProfileByHolderIdQuery;

import java.util.Optional;

public interface ProfileQueryService {

    Optional<Profile> handle(GetProfileByHolderIdQuery query);
}
