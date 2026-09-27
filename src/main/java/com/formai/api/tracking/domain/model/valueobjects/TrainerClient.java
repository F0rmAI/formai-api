package com.formai.api.tracking.domain.model.valueobjects;

// A trainer's client as clients publishes it.
public record TrainerClient(ClientId clientId, String fullName, String status) { }
