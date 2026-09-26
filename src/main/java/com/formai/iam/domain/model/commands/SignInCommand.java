package com.formai.iam.domain.model.commands;

import com.formai.iam.domain.model.valueobjects.ClientApplication;
import com.formai.iam.domain.model.valueobjects.Email;

public record SignInCommand(Email email, String rawPassword, ClientApplication application) { }
