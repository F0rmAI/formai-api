package com.formai.api.shared.contracts.clients;

import java.util.Optional;

// status is a ClientStatus name (INVITED, ACTIVE, INACTIVE); empty lists every status.
public record ClientListRequest(String trainerHolderId,
                                Optional<String> search,
                                Optional<String> status,
                                int page,
                                int size) { }
