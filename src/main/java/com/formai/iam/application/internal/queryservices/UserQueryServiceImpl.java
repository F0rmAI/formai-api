package com.formai.iam.application.internal.queryservices;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.queries.GetUserByIdQuery;
import com.formai.iam.domain.repositories.UserRepository;
import com.formai.iam.domain.services.UserQueryService;
import org.springframework.stereotype.Service;

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
}
