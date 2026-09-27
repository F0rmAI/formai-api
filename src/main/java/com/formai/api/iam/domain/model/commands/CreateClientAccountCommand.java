package com.formai.api.iam.domain.model.commands;

import com.formai.api.iam.domain.model.valueobjects.Email;

public record CreateClientAccountCommand(Email email) { }
