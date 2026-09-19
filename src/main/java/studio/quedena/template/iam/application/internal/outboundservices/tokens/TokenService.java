package studio.quedena.template.iam.application.internal.outboundservices.tokens;

import studio.quedena.template.iam.domain.model.valueobjects.Role;

import java.util.Set;

public interface TokenService {

    String issueFor(String holderId, Set<Role> roles);
}
