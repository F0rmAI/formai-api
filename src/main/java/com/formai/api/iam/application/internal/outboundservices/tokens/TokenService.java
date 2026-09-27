package com.formai.api.iam.application.internal.outboundservices.tokens;

import com.formai.api.iam.domain.model.valueobjects.Role;

import java.util.Set;

public interface TokenService {

    String issueFor(String holderId, Set<Role> roles);
}
