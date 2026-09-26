package com.formai.iam.domain.model.commands;

import com.formai.iam.domain.model.valueobjects.Email;

public record CreateClientAccountCommand(Email email) { }
