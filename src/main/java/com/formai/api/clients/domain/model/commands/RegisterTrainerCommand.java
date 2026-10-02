package com.formai.api.clients.domain.model.commands;

import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;

public record RegisterTrainerCommand(String holderId, FullName fullName, Email email) { }
