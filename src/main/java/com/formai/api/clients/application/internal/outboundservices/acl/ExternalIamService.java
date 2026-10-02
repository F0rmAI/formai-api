package com.formai.api.clients.application.internal.outboundservices.acl;

import com.formai.api.clients.domain.model.valueobjects.ActivationTicket;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.iam.interfaces.acl.IamContextFacade;
import com.formai.api.shared.contracts.iam.AccountActivationSummary;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExternalIamService {

    private static final String ACTIVE = "ACTIVE";

    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    public Optional<ActivationTicket> createClientAccount() {
        return iamContextFacade.createClientAccount().map(this::toTicket);
    }

    public Optional<ActivationTicket> renewActivationCode(ClientId clientId) {
        return iamContextFacade.reissueActivationCode(clientId.value()).map(this::toTicket);
    }

    public boolean disableAccount(ClientId clientId) {
        return iamContextFacade.disableAccount(clientId.value());
    }

    // The email of the account once the client has activated it; empty while it is still pending.
    public Optional<Email> fetchActivatedEmail(ClientId clientId) {
        var active = iamContextFacade.fetchAccountStatus(clientId.value()).map(ACTIVE::equals).orElse(false);
        return active ? iamContextFacade.fetchAccountEmail(clientId.value()).map(Email::new) : Optional.empty();
    }

    private ActivationTicket toTicket(AccountActivationSummary summary) {
        return new ActivationTicket(new ClientId(summary.userId()), summary.activationCode(), summary.expiresAt());
    }
}
