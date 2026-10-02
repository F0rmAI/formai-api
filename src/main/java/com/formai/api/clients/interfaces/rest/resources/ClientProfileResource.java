package com.formai.api.clients.interfaces.rest.resources;

import java.util.UUID;

public record ClientProfileResource(UUID id, String fullName, String email) { }
