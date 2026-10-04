package com.formai.api.tracking.domain.model.commands;

import com.formai.api.tracking.domain.model.valueobjects.ClientId;

import java.time.ZoneId;

public record ChangeTimeZoneCommand(ClientId clientId, ZoneId timeZone) { }
