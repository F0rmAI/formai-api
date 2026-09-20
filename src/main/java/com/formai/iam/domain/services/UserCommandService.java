package com.formai.iam.domain.services;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.commands.SignInCommand;
import com.formai.iam.domain.model.commands.SignUpCommand;

import java.util.Optional;

public interface UserCommandService {

    Optional<User> handle(SignUpCommand command);

    Optional<User> handle(SignInCommand command);
}
