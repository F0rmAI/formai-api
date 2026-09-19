package studio.quedena.template.profiles.infrastructure.persistence.transform;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.infrastructure.persistence.entities.ProfileJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileJpaMapper {

    ProfileJpaEntity toEntity(Profile profile);

    Profile toDomain(ProfileJpaEntity entity);
}
