package com.formai.api.clients.application.internal.outboundservices.acl;

import com.formai.api.clients.domain.model.valueobjects.ActivationTicket;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.iam.interfaces.acl.IamContextFacade;
import com.formai.api.shared.contracts.iam.AccountActivationSummary;
import org.springframework.stereotype.Service;

import java.util.Optional;

// ACL over iam's Open Host Service: a client's id is the id of their iam account.
@Service
public class ExternalIamService {

    private static final String ACTIVE = "ACTIVE";

    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    // Empty when the email already belongs to another account.
    public Optional<ActivationTicket> createClientAccount(Email email) {
        return iamContextFacade.createClientAccount(email.value()).map(this::toTicket);
    }

    // Empty when the account no longer waits for activation.
    public Optional<ActivationTicket> renewActivationCode(ClientId clientId) {
        return iamContextFacade.reissueActivationCode(clientId.value()).map(this::toTicket);
    }

    public boolean disableAccount(ClientId clientId) {
        return iamContextFacade.disableAccount(clientId.value());
    }

    public boolean isAccountActive(ClientId clientId) {
        return iamContextFacade.fetchAccountStatus(clientId.value()).map(ACTIVE::equals).orElse(false);
    }

    private ActivationTicket toTicket(AccountActivationSummary summary) {
        return new ActivationTicket(new ClientId(summary.userId()), summary.activationCode(), summary.expiresAt());
    }
}
