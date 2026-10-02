package com.formai.api.iam.domain.services;

import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.entities.ActivationCode;
import com.formai.api.iam.domain.model.queries.GetUsableActivationCodeQuery;
import com.formai.api.iam.domain.model.queries.GetUserByIdQuery;

import java.util.Optional;

public interface UserQueryService {

    Optional<User> handle(GetUserByIdQuery query);

    Optional<ActivationCode> handle(GetUsableActivationCodeQuery query);
}
