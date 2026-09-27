package com.formai.api.clients.application.internal.commandservices;

import com.formai.api.clients.application.internal.outboundservices.acl.ExternalIamService;
import com.formai.api.clients.domain.exceptions.ActivationCodeNotRenewableException;
import com.formai.api.clients.domain.exceptions.ClientAlreadyRegisteredException;
import com.formai.api.clients.domain.exceptions.ClientEmailUnavailableException;
import com.formai.api.clients.domain.exceptions.ClientNotFoundException;
import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.commands.ActivateClientCommand;
import com.formai.api.clients.domain.model.commands.DeactivateClientCommand;
import com.formai.api.clients.domain.model.commands.RegisterClientCommand;
import com.formai.api.clients.domain.model.commands.RenewActivationCodeCommand;
import com.formai.api.clients.domain.model.commands.UpdateBodyProfileCommand;
import com.formai.api.clients.domain.model.commands.UpdateClientCommand;
import com.formai.api.clients.domain.model.entities.BodyProfile;
import com.formai.api.clients.domain.model.events.BodyWeightRecorded;
import com.formai.api.clients.domain.model.events.ClientActivated;
import com.formai.api.clients.domain.model.events.ClientDeactivated;
import com.formai.api.clients.domain.model.events.ClientRegistered;
import com.formai.api.clients.domain.model.valueobjects.ActivationTicket;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.RegisteredClient;
import com.formai.api.clients.domain.repositories.ClientRepository;
import com.formai.api.clients.domain.services.ClientCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ClientCommandServiceImpl implements ClientCommandService {

    private final ClientRepository clientRepository;
    private final ExternalIamService externalIamService;
    private final ApplicationEventPublisher eventPublisher;

    public ClientCommandServiceImpl(ClientRepository clientRepository,
                                    ExternalIamService externalIamService,
                                    ApplicationEventPublisher eventPublisher) {
        this.clientRepository = clientRepository;
        this.externalIamService = externalIamService;
        this.eventPublisher = eventPublisher;
    }

    // Creates the client's iam account (pending activation) and shows its code on screen (FR-003).
    @Override
    @Transactional
    public Optional<RegisteredClient> handle(RegisterClientCommand command) {
        if (clientRepository.existsByHolderIdAndEmail(command.holderId(), command.email())) {
            throw new ClientAlreadyRegisteredException(command.email().value());
        }
        var ticket = externalIamService.createClientAccount(command.email())
                .orElseThrow(ClientEmailUnavailableException::new);
        var saved = clientRepository.save(Client.register(command, ticket));

        eventPublisher.publishEvent(new ClientRegistered(saved.getId().value(), saved.getHolderId()));
        return Optional.of(new RegisteredClient(saved, ticket));
    }

    @Override
    @Transactional
    public Optional<Client> handle(UpdateClientCommand command) {
        var client = findOwnClient(command.clientId(), command.holderId());
        client.rename(command);
        return Optional.of(clientRepository.save(client));
    }

    // The previous code stops working as soon as iam issues the new one.
    @Override
    @Transactional
    public Optional<ActivationTicket> handle(RenewActivationCodeCommand command) {
        var client = findOwnClient(command.clientId(), command.holderId());
        if (!client.canRenewActivationCode()) {
            throw new ActivationCodeNotRenewableException();
        }
        var ticket = externalIamService.renewActivationCode(client.getId())
                .orElseThrow(ActivationCodeNotRenewableException::new);
        return Optional.of(ticket);
    }

    @Override
    @Transactional
    public void handle(ActivateClientCommand command) {
        clientRepository.findById(command.clientId())
                .filter(client -> client.getStatus() == ClientStatus.INVITED)
                .ifPresent(this::activate);
    }

    // The client can no longer sign in, but nothing is deleted; planning closes the
    // current assignment when it hears ClientDeactivated (FR-006).
    @Override
    @Transactional
    public Optional<Client> handle(DeactivateClientCommand command) {
        var client = findOwnClient(command.clientId(), command.holderId());
        if (client.getStatus() == ClientStatus.INACTIVE) {
            return Optional.of(client);
        }
        client.deactivate();
        var saved = clientRepository.save(client);
        externalIamService.disableAccount(saved.getId());

        eventPublisher.publishEvent(new ClientDeactivated(saved.getId().value(), saved.getHolderId()));
        return Optional.of(saved);
    }

    @Override
    @Transactional
    public Optional<Client> handle(UpdateBodyProfileCommand command) {
        var client = findOwnClient(command.clientId(), command.holderId());
        var recordsBefore = client.getBodyProfile().map(profile -> profile.getWeightHistory().size()).orElse(0);
        var today = LocalDate.now();
        client.updateBodyProfile(command, today);
        var saved = clientRepository.save(client);

        var weightRecorded = saved.getBodyProfile().map(BodyProfile::getWeightHistory)
                .filter(history -> history.size() > recordsBefore);
        weightRecorded.ifPresent(history -> eventPublisher.publishEvent(new BodyWeightRecorded(
                saved.getId().value(), command.weight().kilograms(), today)));
        return Optional.of(saved);
    }

    // Every trainer write on a client first catches up with an activation whose event was
    // lost: an INVITED client whose iam account is already active becomes ACTIVE.
    private Client findOwnClient(ClientId clientId, String holderId) {
        var client = clientRepository.findByIdAndHolderId(clientId, holderId)
                .orElseThrow(ClientNotFoundException::new);
        if (client.canRenewActivationCode() && externalIamService.isAccountActive(client.getId())) {
            activate(client);
        }
        return client;
    }

    private void activate(Client client) {
        client.activate();
        clientRepository.save(client);
        eventPublisher.publishEvent(new ClientActivated(client.getId().value()));
    }
}
