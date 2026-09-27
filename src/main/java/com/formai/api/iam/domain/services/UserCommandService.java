package com.formai.api.iam.domain.services;

import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.ActivateAccountCommand;
import com.formai.api.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.api.iam.domain.model.commands.DisableAccountCommand;
import com.formai.api.iam.domain.model.commands.ReissueActivationCodeCommand;
import com.formai.api.iam.domain.model.commands.RequestPasswordResetCommand;
import com.formai.api.iam.domain.model.commands.ResetPasswordCommand;
import com.formai.api.iam.domain.model.commands.SignInCommand;
import com.formai.api.iam.domain.model.commands.SignUpCommand;

import java.util.Optional;

public interface UserCommandService {

    Optional<User> handle(SignUpCommand command);

    Optional<User> handle(SignInCommand command);

    Optional<User> handle(CreateClientAccountCommand command);

    Optional<User> handle(ReissueActivationCodeCommand command);

    Optional<User> handle(ActivateAccountCommand command);

    void handle(RequestPasswordResetCommand command);

    void handle(ResetPasswordCommand command);

    void handle(DisableAccountCommand command);
}
