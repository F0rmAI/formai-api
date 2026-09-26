package com.formai.iam.interfaces.rest.transform;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.commands.ActivateAccountCommand;
import com.formai.iam.domain.model.commands.RequestPasswordResetCommand;
import com.formai.iam.domain.model.commands.ResetPasswordCommand;
import com.formai.iam.domain.model.commands.SignInCommand;
import com.formai.iam.domain.model.commands.SignUpCommand;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.model.valueobjects.Role;
import com.formai.iam.interfaces.rest.resources.AccountActivationResource;
import com.formai.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.formai.iam.interfaces.rest.resources.CreateAccountActivationResource;
import com.formai.iam.interfaces.rest.resources.CreatePasswordResetRequestResource;
import com.formai.iam.interfaces.rest.resources.CreatePasswordResetResource;
import com.formai.iam.interfaces.rest.resources.PasswordResetRequestResource;
import com.formai.iam.interfaces.rest.resources.PasswordResetResource;
import com.formai.iam.interfaces.rest.resources.SignInResource;
import com.formai.iam.interfaces.rest.resources.SignUpResource;
import com.formai.iam.interfaces.rest.resources.UserResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserAssembler {

    String PASSWORD_RESET_REQUESTED_MESSAGE =
            "If an account exists for this email, a password reset link has been sent to it";
    String PASSWORD_RESET_MESSAGE = "Your password has been reset. You can now sign in with your new password";

    @Mapping(target = "email", source = "resource.email")
    @Mapping(target = "rawPassword", source = "resource.password")
    @Mapping(target = "fullName", source = "resource.fullName")
    SignUpCommand toCommand(SignUpResource resource);

    @Mapping(target = "email", source = "resource.email")
    @Mapping(target = "rawPassword", source = "resource.password")
    @Mapping(target = "application", source = "resource.application")
    SignInCommand toCommand(SignInResource resource);

    @Mapping(target = "activationCode", source = "resource.activationCode")
    @Mapping(target = "rawPassword", source = "resource.password")
    @Mapping(target = "consentAccepted", source = "resource.consentAccepted")
    ActivateAccountCommand toCommand(CreateAccountActivationResource resource);

    @Mapping(target = "email", source = "resource.email")
    RequestPasswordResetCommand toCommand(CreatePasswordResetRequestResource resource);

    @Mapping(target = "token", source = "resource.token")
    @Mapping(target = "rawPassword", source = "resource.password")
    ResetPasswordCommand toCommand(CreatePasswordResetResource resource);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "roles", source = "user.roles")
    UserResource toResource(User user);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "roles", source = "user.roles")
    @Mapping(target = "status", source = "user.status")
    AuthenticatedUserResource toAuthenticatedResource(User user);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "activatedAt", source = "user.activationCode.usedAt")
    AccountActivationResource toActivationResource(User user);

    default PasswordResetRequestResource toPasswordResetRequestResource() {
        return new PasswordResetRequestResource(PASSWORD_RESET_REQUESTED_MESSAGE);
    }

    default PasswordResetResource toPasswordResetResource() {
        return new PasswordResetResource(PASSWORD_RESET_MESSAGE);
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default Email map(String value) {
        return value == null ? null : new Email(value);
    }

    default String map(Email email) {
        return email == null ? null : email.value();
    }

    default Set<String> mapRoles(Set<Role> roles) {
        return roles == null ? null : roles.stream().map(Enum::name).collect(Collectors.toSet());
    }
}
