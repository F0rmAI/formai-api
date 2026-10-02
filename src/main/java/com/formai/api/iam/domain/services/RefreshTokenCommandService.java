package com.formai.api.iam.domain.services;

import com.formai.api.iam.domain.model.aggregates.RefreshToken;
import com.formai.api.iam.domain.model.commands.IssueRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RevokeAllRefreshTokensCommand;
import com.formai.api.iam.domain.model.commands.RevokeRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RotateRefreshTokenCommand;
import com.formai.api.iam.domain.model.valueobjects.RefreshedSession;

import java.util.Optional;

public interface RefreshTokenCommandService {

    Optional<RefreshToken> handle(IssueRefreshTokenCommand command);

    Optional<RefreshedSession> handle(RotateRefreshTokenCommand command);

    void handle(RevokeRefreshTokenCommand command);

    void handle(RevokeAllRefreshTokensCommand command);
}
