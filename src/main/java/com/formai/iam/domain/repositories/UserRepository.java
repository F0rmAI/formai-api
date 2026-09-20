package com.formai.iam.domain.repositories;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.valueobjects.Email;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
