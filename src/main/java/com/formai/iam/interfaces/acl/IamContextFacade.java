package com.formai.iam.interfaces.acl;

import com.formai.shared.contracts.iam.AccountActivationSummary;

import java.util.Optional;
import java.util.UUID;

public interface IamContextFacade {

    Optional<AccountActivationSummary> createClientAccount(String email);

    Optional<AccountActivationSummary> reissueActivationCode(UUID userId);

    boolean disableAccount(UUID userId);

    Optional<String> fetchAccountStatus(UUID userId);
}
