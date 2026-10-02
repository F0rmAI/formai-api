package com.formai.api.clients.domain.model.commands;

import com.formai.api.clients.domain.model.valueobjects.ClientId;

// invitedClientId: the record the new trainer just registered (it only carries the invitation).
// existingClientId: the client's own record, which moves to that trainer with all its history.
public record TransferClientCommand(ClientId invitedClientId, ClientId existingClientId) { }
