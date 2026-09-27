package com.formai.api.iam.domain.model.commands;

import com.formai.api.iam.domain.model.valueobjects.Email;

public record SignInCommand(Email email, String rawPassword) { }
