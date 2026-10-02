package com.formai.api.clients.application.internal.commandservices;

import com.formai.api.clients.application.internal.outboundservices.acl.ExternalIamService;
import com.formai.api.clients.domain.exceptions.ActivationCodeNotRenewableException;
import com.formai.api.clients.domain.exceptions.ClientNotFoundException;
import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.commands.ActivateClientCommand;
import com.formai.api.clients.domain.model.commands.DeactivateClientCommand;
import com.formai.api.clients.domain.model.commands.RenewActivationCodeCommand;
import com.formai.api.clients.domain.model.commands.UpdateClientCommand;
import com.formai.api.clients.domain.model.events.BodyWeightRecorded;
import com.formai.api.clients.domain.model.events.ClientActivated;
import com.formai.api.clients.domain.model.events.ClientDeactivated;
import com.formai.api.clients.domain.model.events.ClientRegistered;
import com.formai.api.clients.domain.model.valueobjects.ActivationTicket;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static com.formai.api.clients.ClientsTestData.CLIENT_EMAIL;
import static com.formai.api.clients.ClientsTestData.CLIENT_ID;
import static com.formai.api.clients.ClientsTestData.TRAINER_HOLDER_ID;
import static com.formai.api.clients.ClientsTestData.activeClient;
import static com.formai.api.clients.ClientsTestData.bodyProfileCommand;
import static com.formai.api.clients.ClientsTestData.invitedClient;
import static com.formai.api.clients.ClientsTestData.registerCommand;
import static com.formai.api.clients.ClientsTestData.ticket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCommandServiceImplTest {

    @Mock
    ClientRepository clientRepository;

    @Mock
    ExternalIamService externalIamService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ClientCommandServiceImpl commandService;

    private void savesReturnTheClient() {
        when(clientRepository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void trainerOwns(Client client) {
        when(clientRepository.findByIdAndHolderId(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(client));
    }

    @Test
    void shouldRegisterAnInvitedClientWithItsActivationCode() {
        // Arrange
        when(externalIamService.createClientAccount()).thenReturn(Optional.of(ticket()));
        savesReturnTheClient();

        // Act
        var registered = commandService.handle(registerCommand()).orElseThrow();

        // Assert
        assertThat(registered.client().getId()).isEqualTo(CLIENT_ID);
        assertThat(registered.client().getStatus()).isEqualTo(ClientStatus.INVITED);
        assertThat(registered.client().getEmail()).isNull();
        assertThat(registered.ticket().code()).isEqualTo("ABCD2345");
        verify(eventPublisher).publishEvent(new ClientRegistered(CLIENT_ID.value(), TRAINER_HOLDER_ID));
    }

    @Test
    void shouldRegisterTwoClientsWithTheSameNameSinceNoEmailTellsThemApart() {
        when(externalIamService.createClientAccount()).thenReturn(Optional.of(ticket()));
        savesReturnTheClient();

        commandService.handle(registerCommand());
        commandService.handle(registerCommand());

        verify(clientRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void shouldIssueANewCodeForAnInvitedClient() {
        trainerOwns(invitedClient());
        var renewed = new ActivationTicket(CLIENT_ID, "WXYZ6789", Instant.parse("2026-10-07T12:00:00Z"));
        when(externalIamService.renewActivationCode(CLIENT_ID)).thenReturn(Optional.of(renewed));

        var ticket = commandService.handle(new RenewActivationCodeCommand(CLIENT_ID, TRAINER_HOLDER_ID)).orElseThrow();

        assertThat(ticket.code()).isEqualTo("WXYZ6789");
    }

    @Test
    void shouldNotRenewTheCodeOfAnActiveClient() {
        trainerOwns(activeClient());

        assertThatThrownBy(() -> commandService.handle(new RenewActivationCodeCommand(CLIENT_ID, TRAINER_HOLDER_ID)))
                .isInstanceOf(ActivationCodeNotRenewableException.class);
        verify(externalIamService, never()).renewActivationCode(any());
    }

    @Test
    void shouldReturnNotFoundForAClientOfAnotherTrainer() {
        when(clientRepository.findByIdAndHolderId(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.handle(new RenewActivationCodeCommand(CLIENT_ID, TRAINER_HOLDER_ID)))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void shouldActivateAnInvitedClientAndPublishClientActivated() {
        var client = invitedClient();
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        savesReturnTheClient();

        commandService.handle(new ActivateClientCommand(CLIENT_ID, CLIENT_EMAIL));

        assertThat(client.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(client.getEmail()).isEqualTo(CLIENT_EMAIL);
        verify(eventPublisher).publishEvent(new ClientActivated(CLIENT_ID.value()));
    }

    @Test
    void shouldCatchUpWithALostActivationBeforeATrainerWrite() {
        var client = invitedClient();
        trainerOwns(client);
        when(externalIamService.fetchActivatedEmail(CLIENT_ID)).thenReturn(Optional.of(CLIENT_EMAIL));
        savesReturnTheClient();

        var renamed = commandService.handle(new UpdateClientCommand(CLIENT_ID, TRAINER_HOLDER_ID,
                new FullName("Luis R."))).orElseThrow();

        assertThat(renamed.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(renamed.getEmail()).isEqualTo(CLIENT_EMAIL);
        assertThat(renamed.getFullName().value()).isEqualTo("Luis R.");
        verify(eventPublisher).publishEvent(new ClientActivated(CLIENT_ID.value()));
    }

    @Test
    void shouldDeactivateDisableTheAccountAndPublishClientDeactivated() {
        trainerOwns(activeClient());
        savesReturnTheClient();

        var client = commandService.handle(new DeactivateClientCommand(CLIENT_ID, TRAINER_HOLDER_ID)).orElseThrow();

        assertThat(client.getStatus()).isEqualTo(ClientStatus.INACTIVE);
        verify(externalIamService).disableAccount(CLIENT_ID);
        verify(eventPublisher).publishEvent(new ClientDeactivated(CLIENT_ID.value(), TRAINER_HOLDER_ID));
    }

    @Test
    void shouldIgnoreDeactivatingAnInactiveClientTwice() {
        var client = activeClient();
        client.deactivate();
        trainerOwns(client);

        commandService.handle(new DeactivateClientCommand(CLIENT_ID, TRAINER_HOLDER_ID));

        verify(externalIamService, never()).disableAccount(any());
        verify(eventPublisher, never()).publishEvent(any(ClientDeactivated.class));
    }

    @Test
    void shouldSaveTheBodyProfileAndPublishTheNewWeight() {
        trainerOwns(activeClient());
        savesReturnTheClient();

        var client = commandService.handle(bodyProfileCommand("80.5")).orElseThrow();

        assertThat(client.getBodyProfile()).isPresent();
        verify(eventPublisher).publishEvent(any(BodyWeightRecorded.class));
    }

    @Test
    void shouldNotPublishAWeightThatDidNotChange() {
        var client = activeClient();
        client.updateBodyProfile(bodyProfileCommand("80.5"), LocalDate.now());
        trainerOwns(client);
        savesReturnTheClient();

        commandService.handle(bodyProfileCommand("80.5"));

        verify(eventPublisher, never()).publishEvent(any(BodyWeightRecorded.class));
    }
}
