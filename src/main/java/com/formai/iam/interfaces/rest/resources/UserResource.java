package com.formai.iam.interfaces.rest.resources;

import java.util.Set;
import java.util.UUID;

public record UserResource(UUID id, String email, Set<String> roles) { }
