package com.formai.api.iam.domain.model.commands;

import java.util.UUID;

public record IssueRefreshTokenCommand(UUID userId) { }
