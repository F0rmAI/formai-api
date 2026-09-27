package com.formai.api.iam.domain.services;

import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.SignInCommand;
import com.formai.api.iam.domain.model.commands.SignUpCommand;

import java.util.Optional;

public interface UserCommandService {

    Optional<User> handle(SignUpCommand command);

    Optional<User> handle(SignInCommand command);
}
