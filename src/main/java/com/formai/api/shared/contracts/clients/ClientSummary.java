package com.formai.api.shared.contracts.clients;

import java.util.UUID;

public record ClientSummary(UUID clientId, String fullName, String email, String status) { }
