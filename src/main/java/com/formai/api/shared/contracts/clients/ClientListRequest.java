package com.formai.api.shared.contracts.clients;

import java.util.Optional;

public record ClientListRequest(String trainerHolderId,
                                Optional<String> search,
                                Optional<String> status,
                                int page,
                                int size) { }
