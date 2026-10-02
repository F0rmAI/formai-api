package com.formai.api.clients.domain.model.commands;

import com.formai.api.clients.domain.model.valueobjects.FullName;

public record RegisterClientCommand(String holderId, FullName fullName) { }
