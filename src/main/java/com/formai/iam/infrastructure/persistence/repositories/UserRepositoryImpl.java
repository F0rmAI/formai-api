package com.formai.iam.infrastructure.persistence.repositories;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.repositories.UserRepository;
import com.formai.iam.infrastructure.persistence.transform.UserJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserJpaMapper mapper;

    public UserRepositoryImpl(UserJpaRepository jpaRepository, UserJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        var entity = mapper.toEntity(user);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByActivationCode(String code) {
        return jpaRepository.findByActivationCode(code).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByPasswordResetTokenHash(String tokenHash) {
        return jpaRepository.findByPasswordResetTokenHash(tokenHash).map(mapper::toDomain);
    }
}
