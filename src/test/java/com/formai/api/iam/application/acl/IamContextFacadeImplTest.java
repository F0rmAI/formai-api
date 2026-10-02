package com.formai.api.iam.application.acl;

import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.ActivateAccountCommand;
import com.formai.api.iam.domain.model.valueobjects.HashedPassword;
import com.formai.api.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.api.iam.domain.model.commands.DisableAccountCommand;
import com.formai.api.iam.domain.model.commands.ReissueActivationCodeCommand;
import com.formai.api.iam.domain.model.commands.RevokeAllRefreshTokensCommand;
import com.formai.api.iam.domain.model.queries.GetUserByIdQuery;
import com.formai.api.iam.domain.model.valueobjects.Email;
import com.formai.api.iam.domain.services.RefreshTokenCommandService;
import com.formai.api.iam.domain.services.UserCommandService;
import com.formai.api.iam.domain.services.UserQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IamContextFacadeImplTest {

    @Mock
    UserCommandService userCommandService;

    @Mock
    UserQueryService userQueryService;

    @Mock
    RefreshTokenCommandService refreshTokenCommandService;

    @InjectMocks
    IamContextFacadeImpl facade;

    private static User pendingClient() {
        return User.createPendingClient(new CreateClientAccountCommand(), Instant.now());
    }

    @Test
    void shouldReturnActivationSummaryWhenClientAccountIsCreated() {
        // Arrange
        var client = pendingClient();
        when(userCommandService.handle(new CreateClientAccountCommand())).thenReturn(Optional.of(client));

        // Act
        var summary = facade.createClientAccount();

        // Assert
        assertThat(summary).isPresent();
        assertThat(summary.get().userId()).isEqualTo(client.getId());
        assertThat(summary.get().activationCode()).isEqualTo(client.getActivationCode().getCode());
        assertThat(summary.get().expiresAt()).isEqualTo(client.getActivationCode().getExpiresAt());
    }

    @Test
    void shouldReturnActivationSummaryWhenCodeIsReissued() {
        var client = pendingClient();
        when(userCommandService.handle(new ReissueActivationCodeCommand(client.getId())))
                .thenReturn(Optional.of(client));

        assertThat(facade.reissueActivationCode(client.getId()))
                .hasValueSatisfying(summary -> assertThat(summary.activationCode())
                        .isEqualTo(client.getActivationCode().getCode()));
    }

    @Test
    void shouldReturnEmptyWhenCodeCannotBeReissued() {
        var id = UUID.randomUUID();
        when(userCommandService.handle(new ReissueActivationCodeCommand(id))).thenReturn(Optional.empty());

        assertThat(facade.reissueActivationCode(id)).isEmpty();
    }

    @Test
    void shouldDisableAccountWhenUserExists() {
        var client = pendingClient();
        when(userQueryService.handle(new GetUserByIdQuery(client.getId()))).thenReturn(Optional.of(client));

        assertThat(facade.disableAccount(client.getId())).isTrue();
        verify(userCommandService).handle(new DisableAccountCommand(client.getId()));
        verify(refreshTokenCommandService).handle(new RevokeAllRefreshTokensCommand(client.getId()));
    }

    @Test
    void shouldReturnFalseWhenDisablingUnknownAccount() {
        var id = UUID.randomUUID();
        when(userQueryService.handle(new GetUserByIdQuery(id))).thenReturn(Optional.empty());

        assertThat(facade.disableAccount(id)).isFalse();
        verify(userCommandService, never()).handle(any(DisableAccountCommand.class));
    }

    @Test
    void shouldReturnStatusNameWhenAccountExists() {
        var client = pendingClient();
        when(userQueryService.handle(new GetUserByIdQuery(client.getId()))).thenReturn(Optional.of(client));

        assertThat(facade.fetchAccountStatus(client.getId())).contains("PENDING_ACTIVATION");
    }

    @Test
    void shouldReturnEmptyStatusWhenAccountDoesNotExist() {
        var id = UUID.randomUUID();
        when(userQueryService.handle(new GetUserByIdQuery(id))).thenReturn(Optional.empty());

        assertThat(facade.fetchAccountStatus(id)).isEmpty();
    }

    @Test
    void shouldReturnNoEmailWhileTheClientAccountIsPending() {
        var client = pendingClient();
        when(userQueryService.handle(new GetUserByIdQuery(client.getId()))).thenReturn(Optional.of(client));

        assertThat(facade.fetchAccountEmail(client.getId())).isEmpty();
    }

    @Test
    void shouldReturnTheEmailTheClientChoseOnActivation() {
        var client = pendingClient();
        client.activate(new ActivateAccountCommand(client.getActivationCode().getCode(), new Email("client@formai.com"),
                "secret123", true, "1.0"), new HashedPassword("hashed"), Instant.now());
        when(userQueryService.handle(new GetUserByIdQuery(client.getId()))).thenReturn(Optional.of(client));

        assertThat(facade.fetchAccountEmail(client.getId())).contains("client@formai.com");
    }
}
