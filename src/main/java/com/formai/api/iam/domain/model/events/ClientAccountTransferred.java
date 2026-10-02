package com.formai.api.iam.domain.model.events;

import java.time.Instant;
import java.util.UUID;

// A client who already had an account redeemed the invitation of a new trainer. invitedUserId is
// the pending account that carried the code (now discarded); existingUserId is the account that
// stays and moves to that trainer. The clients context moves the client record accordingly.
public record ClientAccountTransferred(UUID invitedUserId, UUID existingUserId, Instant transferredAt) { }
