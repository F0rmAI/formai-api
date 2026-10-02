package com.formai.api.iam.application.internal.queryservices;

import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.entities.ActivationCode;
import com.formai.api.iam.domain.model.queries.GetUsableActivationCodeQuery;
import com.formai.api.iam.domain.model.queries.GetUserByIdQuery;
import com.formai.api.iam.domain.model.valueobjects.AccountStatus;
import com.formai.api.iam.domain.repositories.UserRepository;
import com.formai.api.iam.domain.services.UserQueryService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public Optional<ActivationCode> handle(GetUsableActivationCodeQuery query) {
        var now = Instant.now();
        return userRepository.findByActivationCode(query.code())
                .filter(user -> user.getStatus() == AccountStatus.PENDING_ACTIVATION)
                .map(User::getActivationCode)
                .filter(activationCode -> activationCode.isUsable(now));
    }
}
