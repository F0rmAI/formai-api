package com.formai.api.clients.application;

import com.formai.api.clients.application.acl.ClientsContextFacadeImpl;
import com.formai.api.clients.application.internal.commandservices.TrainerCommandServiceImpl;
import com.formai.api.clients.application.internal.eventhandlers.AccountActivatedEventHandler;
import com.formai.api.clients.application.internal.eventhandlers.UserRegisteredEventHandler;
import com.formai.api.clients.application.internal.outboundservices.acl.ExternalIamService;
import com.formai.api.clients.application.internal.queryservices.ClientQueryServiceImpl;
import com.formai.api.clients.domain.model.aggregates.Trainer;
import com.formai.api.clients.domain.model.commands.ActivateClientCommand;
import com.formai.api.clients.domain.model.commands.RegisterTrainerCommand;
import com.formai.api.clients.domain.model.queries.GetClientByIdQuery;
import com.formai.api.clients.domain.model.queries.GetClientsQuery;
import com.formai.api.clients.domain.model.valueobjects.ClientPage;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.model.valueobjects.Pagination;
import com.formai.api.clients.domain.repositories.ClientRepository;
import com.formai.api.clients.domain.repositories.TrainerRepository;
import com.formai.api.clients.domain.services.ClientCommandService;
import com.formai.api.clients.domain.services.ClientQueryService;
import com.formai.api.clients.domain.services.TrainerCommandService;
import com.formai.api.iam.domain.model.events.AccountActivated;
import com.formai.api.iam.domain.model.events.UserRegistered;
import com.formai.api.iam.interfaces.acl.IamContextFacade;
import com.formai.api.shared.contracts.clients.ClientListRequest;
import com.formai.api.shared.contracts.iam.AccountActivationSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formai.api.clients.ClientsTestData.CLIENT_EMAIL;
import static com.formai.api.clients.ClientsTestData.CLIENT_ID;
import static com.formai.api.clients.ClientsTestData.CODE_EXPIRES_AT;
import static com.formai.api.clients.ClientsTestData.TRAINER_HOLDER_ID;
import static com.formai.api.clients.ClientsTestData.activeClient;
import static com.formai.api.clients.ClientsTestData.invitedClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Everything that crosses clients' boundary: its Open Host Service, its ACL over iam, the
// iam events it listens to and the read-side catch-up with a lost activation.
@ExtendWith(MockitoExtension.class)
class ClientsIntegrationPointsTest {

    @Mock
    ClientQueryService clientQueryService;

    @Mock
    ClientCommandService clientCommandService;

    @Mock
    TrainerCommandService trainerCommandService;

    @Mock
    ClientRepository clientRepository;

    @Mock
    TrainerRepository trainerRepository;

    @Mock
    IamContextFacade iamContextFacade;

    @Mock
    ExternalIamService externalIamService;

    // --- Open Host Service ---------------------------------------------------------------

    @Test
    void shouldPublishOneOfTheTrainersClients() {
        // Arrange
        when(clientQueryService.handle(new GetClientByIdQuery(CLIENT_ID, TRAINER_HOLDER_ID)))
                .thenReturn(Optional.of(activeClient()));

        // Act
        var summary = new ClientsContextFacadeImpl(clientQueryService)
                .fetchClientOfTrainer(CLIENT_ID.value(), TRAINER_HOLDER_ID).orElseThrow();

        // Assert
        assertThat(summary.clientId()).isEqualTo(CLIENT_ID.value());
        assertThat(summary.fullName()).isEqualTo("Luis Ramos");
        assertThat(summary.email()).isEqualTo("luis@formai.com");
        assertThat(summary.status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldPublishAPageOfTheTrainersClientsFilteredByStatus() {
        when(clientQueryService.handle(new GetClientsQuery(TRAINER_HOLDER_ID, Optional.of("lu"),
                Optional.of(ClientStatus.ACTIVE), new Pagination(0, 20))))
                .thenReturn(new ClientPage(List.of(activeClient()), 0, 20, 1, 1));

        var page = new ClientsContextFacadeImpl(clientQueryService).fetchClientsOfTrainer(
                new ClientListRequest(TRAINER_HOLDER_ID, Optional.of("lu"), Optional.of("active"), 0, 20));

        assertThat(page.items()).extracting(summary -> summary.status()).containsExactly("ACTIVE");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldPublishNoClientsForAnUnknownStatus() {
        var page = new ClientsContextFacadeImpl(clientQueryService).fetchClientsOfTrainer(
                new ClientListRequest(TRAINER_HOLDER_ID, Optional.empty(), Optional.of("DELETED"), 0, 20));

        assertThat(page.items()).isEmpty();
        verifyNoInteractions(clientQueryService);
    }

    // --- ACL over iam --------------------------------------------------------------------

    @Test
    void shouldTurnTheNewIamAccountIntoAnActivationTicket() {
        when(iamContextFacade.createClientAccount("luis@formai.com"))
                .thenReturn(Optional.of(new AccountActivationSummary(CLIENT_ID.value(), "ABCD2345", CODE_EXPIRES_AT)));

        var ticket = new ExternalIamService(iamContextFacade).createClientAccount(CLIENT_EMAIL).orElseThrow();

        assertThat(ticket.clientId()).isEqualTo(CLIENT_ID);
        assertThat(ticket.code()).isEqualTo("ABCD2345");
        assertThat(ticket.expiresAt()).isEqualTo(CODE_EXPIRES_AT);
    }

    @Test
    void shouldTellWhetherTheClientsAccountIsActive() {
        var acl = new ExternalIamService(iamContextFacade);
        when(iamContextFacade.fetchAccountStatus(CLIENT_ID.value()))
                .thenReturn(Optional.of("ACTIVE"))
                .thenReturn(Optional.of("PENDING_ACTIVATION"))
                .thenReturn(Optional.empty());

        assertThat(acl.isAccountActive(CLIENT_ID)).isTrue();
        assertThat(acl.isAccountActive(CLIENT_ID)).isFalse();
        assertThat(acl.isAccountActive(CLIENT_ID)).isFalse();
    }

    // --- iam events ----------------------------------------------------------------------

    @Test
    void shouldRegisterTheTrainerWhenTheirAccountIsRegistered() {
        new UserRegisteredEventHandler(trainerCommandService).on(new UserRegistered(UUID.fromString(TRAINER_HOLDER_ID),
                TRAINER_HOLDER_ID, "Ana Torres", "ana@formai.com"));

        verify(trainerCommandService).handle(new RegisterTrainerCommand(TRAINER_HOLDER_ID, new FullName("Ana Torres"),
                new Email("ana@formai.com")));
    }

    @Test
    void shouldNotRegisterTheSameTrainerTwice() {
        var existing = Trainer.register(new RegisterTrainerCommand(TRAINER_HOLDER_ID, new FullName("Ana Torres"),
                new Email("ana@formai.com")));
        when(trainerRepository.existsByHolderId(TRAINER_HOLDER_ID)).thenReturn(true);
        when(trainerRepository.findByHolderId(TRAINER_HOLDER_ID)).thenReturn(Optional.of(existing));

        var trainer = new TrainerCommandServiceImpl(trainerRepository).handle(new RegisterTrainerCommand(
                TRAINER_HOLDER_ID, new FullName("Ana Torres"), new Email("ana@formai.com")));

        assertThat(trainer).containsSame(existing);
        verify(trainerRepository, never()).save(any());
    }

    @Test
    void shouldActivateTheClientWhenTheirAccountIsActivated() {
        new AccountActivatedEventHandler(clientCommandService).on(new AccountActivated(CLIENT_ID.value(), Instant.now()));

        verify(clientCommandService).handle(new ActivateClientCommand(CLIENT_ID));
    }

    // --- Read-side catch-up ----------------------------------------------------------------

    @Test
    void shouldShowAnInvitedClientAsActiveOnceItsAccountIsActive() {
        when(clientRepository.findByIdAndHolderId(CLIENT_ID, TRAINER_HOLDER_ID)).thenReturn(Optional.of(invitedClient()));
        when(externalIamService.isAccountActive(CLIENT_ID)).thenReturn(true);

        var client = new ClientQueryServiceImpl(clientRepository, externalIamService)
                .handle(new GetClientByIdQuery(CLIENT_ID, TRAINER_HOLDER_ID)).orElseThrow();

        assertThat(client.getStatus()).isEqualTo(ClientStatus.ACTIVE);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void shouldNotAskIamAboutClientsThatAreNotInvited() {
        when(clientRepository.findAllByHolderId(TRAINER_HOLDER_ID, Optional.empty(), Optional.empty(),
                new Pagination(0, 20))).thenReturn(new ClientPage(List.of(activeClient()), 0, 20, 1, 1));

        var page = new ClientQueryServiceImpl(clientRepository, externalIamService).handle(
                new GetClientsQuery(TRAINER_HOLDER_ID, Optional.empty(), Optional.empty(), new Pagination(0, 20)));

        assertThat(page.items()).hasSize(1);
        verifyNoInteractions(externalIamService);
    }
}
