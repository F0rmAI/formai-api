package com.formai.iam.domain.services;

import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.queries.GetUserByIdQuery;

import java.util.Optional;

public interface UserQueryService {

    Optional<User> handle(GetUserByIdQuery query);
}
