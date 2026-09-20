package com.formai.iam.application.internal.outboundservices.tokens;

import com.formai.iam.domain.model.valueobjects.Role;

import java.util.Set;

public interface TokenService {

    String issueFor(String holderId, Set<Role> roles);
}
