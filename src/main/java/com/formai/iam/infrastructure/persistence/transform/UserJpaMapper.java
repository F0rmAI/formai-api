package com.formai.iam.infrastructure.persistence.transform;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.valueobjects.ConsentAcceptance;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.model.valueobjects.HashedPassword;
import com.formai.iam.domain.model.valueobjects.Role;
import com.formai.iam.infrastructure.persistence.entities.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserJpaMapper {

    @Mapping(target = "failedSignInCount", source = "failedSignIns.count")
    @Mapping(target = "lockedUntil", source = "failedSignIns.lockedUntil")
    @Mapping(target = "dataConsentAcceptedAt", source = "dataConsent")
    UserJpaEntity toEntity(User user);

    @Mapping(target = "failedSignIns.count", source = "failedSignInCount")
    @Mapping(target = "failedSignIns.lockedUntil", source = "lockedUntil")
    @Mapping(target = "dataConsent", source = "dataConsentAcceptedAt")
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

    default Instant map(ConsentAcceptance consentAcceptance) {
        return consentAcceptance == null ? null : consentAcceptance.acceptedAt();
    }

    default ConsentAcceptance mapConsentAcceptance(Instant acceptedAt) {
        return acceptedAt == null ? null : new ConsentAcceptance(acceptedAt);
    }

    default Set<String> mapRoles(Set<Role> roles) {
        return roles == null ? null : roles.stream().map(Enum::name).collect(Collectors.toSet());
    }

    default Set<Role> mapRoleNames(Set<String> roles) {
        return roles == null ? null : roles.stream().map(Role::valueOf).collect(Collectors.toSet());
    }
}
