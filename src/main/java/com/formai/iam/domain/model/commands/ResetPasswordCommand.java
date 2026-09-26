package com.formai.iam.domain.model.commands;

public record ResetPasswordCommand(String token, String rawPassword) { }
