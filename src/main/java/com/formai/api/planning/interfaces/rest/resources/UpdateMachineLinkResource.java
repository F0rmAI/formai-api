package com.formai.api.planning.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateMachineLinkResource(@NotNull UUID machineId) { }
