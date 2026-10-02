package com.formai.api.iam.interfaces.acl;

import com.formai.api.shared.contracts.iam.AccountActivationSummary;

import java.util.Optional;
import java.util.UUID;

public interface IamContextFacade {

    Optional<AccountActivationSummary> createClientAccount();

    Optional<AccountActivationSummary> reissueActivationCode(UUID userId);

    boolean disableAccount(UUID userId);

    Optional<String> fetchAccountStatus(UUID userId);

    Optional<String> fetchAccountEmail(UUID userId);
}
