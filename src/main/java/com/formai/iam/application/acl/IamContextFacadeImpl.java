package com.formai.iam.application.acl;

import com.formai.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.iam.domain.model.commands.DisableAccountCommand;
import com.formai.iam.domain.model.commands.ReissueActivationCodeCommand;
import com.formai.iam.domain.model.queries.GetUserByIdQuery;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.services.UserCommandService;
import com.formai.iam.domain.services.UserQueryService;
import com.formai.iam.interfaces.acl.IamContextFacade;
import com.formai.shared.contracts.iam.AccountActivationSummary;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class IamContextFacadeImpl implements IamContextFacade {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public IamContextFacadeImpl(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    @Override
    public Optional<AccountActivationSummary> createClientAccount(String email) {
        try {
            return userCommandService.handle(new CreateClientAccountCommand(new Email(email)))
                    .map(this::toActivationSummary);
        } catch (EmailAlreadyRegisteredException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AccountActivationSummary> reissueActivationCode(UUID userId) {
        return userCommandService.handle(new ReissueActivationCodeCommand(userId))
                .map(this::toActivationSummary);
    }

    @Override
    public boolean disableAccount(UUID userId) {
        if (userQueryService.handle(new GetUserByIdQuery(userId)).isEmpty()) {
            return false;
        }
        userCommandService.handle(new DisableAccountCommand(userId));
        return true;
    }

    @Override
    public Optional<String> fetchAccountStatus(UUID userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getStatus().name());
    }

    private AccountActivationSummary toActivationSummary(User user) {
        var activationCode = user.getActivationCode();
        return new AccountActivationSummary(user.getId(), activationCode.getCode(), activationCode.getExpiresAt());
    }
}
