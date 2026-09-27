package com.formai.api.iam.domain.model.commands;

public record ResetPasswordCommand(String token, String rawPassword) { }
