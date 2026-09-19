package studio.quedena.template.iam.domain.services;

import studio.quedena.template.iam.domain.model.aggregates.User;
import studio.quedena.template.iam.domain.model.commands.SignInCommand;
import studio.quedena.template.iam.domain.model.commands.SignUpCommand;

import java.util.Optional;

public interface UserCommandService {

    Optional<User> handle(SignUpCommand command);

    Optional<User> handle(SignInCommand command);
}
