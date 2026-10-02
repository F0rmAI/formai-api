package com.formai.api.iam.application.internal.queryservices;

import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.api.iam.domain.model.commands.SignUpCommand;
import com.formai.api.iam.domain.model.queries.GetUsableActivationCodeQuery;
import com.formai.api.iam.domain.model.queries.GetUserByIdQuery;
import com.formai.api.iam.domain.model.valueobjects.AccountStatus;
import com.formai.api.iam.domain.model.valueobjects.Email;
import com.formai.api.iam.domain.model.valueobjects.HashedPassword;
import com.formai.api.iam.domain.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserQueryServiceImpl queryService;

    @Test
    void shouldReturnUserWhenIdExists() {
        var user = User.registerTrainer(new SignUpCommand(new Email("trainer@formai.com"), "secret123", "Ana Trainer"),
                new HashedPassword("hashed"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(queryService.handle(new GetUserByIdQuery(user.getId()))).contains(user);
    }

    @Test
    void shouldReturnEmptyWhenIdDoesNotExist() {
        var id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(queryService.handle(new GetUserByIdQuery(id))).isEmpty();
    }

    private static User pendingClientCreatedAt(Instant createdAt) {
        return User.createPendingClient(new CreateClientAccountCommand(), createdAt);
    }

    @Test
    void shouldReturnActivationCodeWhenItExistsAndHasNotExpired() {
        var client = pendingClientCreatedAt(Instant.now());
        var code = client.getActivationCode().getCode();
        when(userRepository.findByActivationCode(code)).thenReturn(Optional.of(client));

        assertThat(queryService.handle(new GetUsableActivationCodeQuery(code))).contains(client.getActivationCode());
    }

    @Test
    void shouldReturnEmptyWhenActivationCodeDoesNotExist() {
        when(userRepository.findByActivationCode("ZZZZ9999")).thenReturn(Optional.empty());

        assertThat(queryService.handle(new GetUsableActivationCodeQuery("ZZZZ9999"))).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenActivationCodeHasExpired() {
        var issuedAt = Instant.now().minus(User.ACTIVATION_CODE_VALIDITY).minus(Duration.ofMinutes(1));
        var client = pendingClientCreatedAt(issuedAt);
        var code = client.getActivationCode().getCode();
        when(userRepository.findByActivationCode(code)).thenReturn(Optional.of(client));

        assertThat(queryService.handle(new GetUsableActivationCodeQuery(code))).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenAccountIsNoLongerPendingActivation() {
        var client = pendingClientCreatedAt(Instant.now());
        client.setStatus(AccountStatus.DISABLED);
        var code = client.getActivationCode().getCode();
        when(userRepository.findByActivationCode(code)).thenReturn(Optional.of(client));

        assertThat(queryService.handle(new GetUsableActivationCodeQuery(code))).isEmpty();
    }
}
