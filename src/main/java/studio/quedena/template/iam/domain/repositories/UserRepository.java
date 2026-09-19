package studio.quedena.template.iam.domain.repositories;

import studio.quedena.template.iam.domain.model.aggregates.User;
import studio.quedena.template.iam.domain.model.valueobjects.Email;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
