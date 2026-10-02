package com.formai.api.clients.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No email: the client chooses it when activating the account from the mobile app.
public record RegisterClientResource(@NotBlank @Size(max = 120) String fullName) { }
