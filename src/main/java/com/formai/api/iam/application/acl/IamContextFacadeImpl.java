package com.formai.api.iam.application.acl;

import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.api.iam.domain.model.commands.DisableAccountCommand;
import com.formai.api.iam.domain.model.commands.ReissueActivationCodeCommand;
import com.formai.api.iam.domain.model.commands.RevokeAllRefreshTokensCommand;
import com.formai.api.iam.domain.model.queries.GetUserByIdQuery;
import com.formai.api.iam.domain.model.valueobjects.Email;
import com.formai.api.iam.domain.services.RefreshTokenCommandService;
import com.formai.api.iam.domain.services.UserCommandService;
import com.formai.api.iam.domain.services.UserQueryService;
import com.formai.api.iam.interfaces.acl.IamContextFacade;
import com.formai.api.shared.contracts.iam.AccountActivationSummary;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class IamContextFacadeImpl implements IamContextFacade {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final RefreshTokenCommandService refreshTokenCommandService;

    public IamContextFacadeImpl(UserCommandService userCommandService, UserQueryService userQueryService,
                                RefreshTokenCommandService refreshTokenCommandService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.refreshTokenCommandService = refreshTokenCommandService;
    }

    @Override
    public Optional<AccountActivationSummary> createClientAccount() {
        return userCommandService.handle(new CreateClientAccountCommand())
                .map(this::toActivationSummary);
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
        // A disabled account must not renew its session with a refresh token issued before.
        refreshTokenCommandService.handle(new RevokeAllRefreshTokensCommand(userId));
        return true;
    }

    @Override
    public Optional<String> fetchAccountStatus(UUID userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> user.getStatus().name());
    }

    // Empty while the account is pending: the client chooses the email on activation.
    @Override
    public Optional<String> fetchAccountEmail(UUID userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .flatMap(user -> Optional.ofNullable(user.getEmail()))
                .map(Email::value);
    }

    private AccountActivationSummary toActivationSummary(User user) {
        var activationCode = user.getActivationCode();
        return new AccountActivationSummary(user.getId(), activationCode.getCode(), activationCode.getExpiresAt());
    }
}
