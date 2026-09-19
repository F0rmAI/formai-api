package studio.quedena.template.iam.infrastructure.persistence.transform;

import studio.quedena.template.iam.domain.model.aggregates.User;
import studio.quedena.template.iam.domain.model.valueobjects.Email;
import studio.quedena.template.iam.domain.model.valueobjects.HashedPassword;
import studio.quedena.template.iam.domain.model.valueobjects.Role;
import studio.quedena.template.iam.infrastructure.persistence.entities.UserJpaEntity;
import org.mapstruct.Mapper;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserJpaMapper {

    UserJpaEntity toEntity(User user);

    User toDomain(UserJpaEntity entity);

    // required by MapStruct: single-field VOs need an explicit converter.
    default String map(Email email) {
        return email == null ? null : email.value();
    }

    default Email mapEmail(String value) {
        return value == null ? null : new Email(value);
    }

    default String map(HashedPassword hashedPassword) {
        return hashedPassword == null ? null : hashedPassword.value();
    }

    default HashedPassword mapHashedPassword(String value) {
        return value == null ? null : new HashedPassword(value);
    }

    default Set<String> mapRoles(Set<Role> roles) {
        return roles == null ? null : roles.stream().map(Enum::name).collect(Collectors.toSet());
    }

    default Set<Role> mapRoleNames(Set<String> roles) {
        return roles == null ? null : roles.stream().map(Role::valueOf).collect(Collectors.toSet());
    }
}
