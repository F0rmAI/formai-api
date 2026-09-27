package com.formai.api.clients.domain.services;

import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.commands.ActivateClientCommand;
import com.formai.api.clients.domain.model.commands.DeactivateClientCommand;
import com.formai.api.clients.domain.model.commands.RegisterClientCommand;
import com.formai.api.clients.domain.model.commands.RenewActivationCodeCommand;
import com.formai.api.clients.domain.model.commands.UpdateBodyProfileCommand;
import com.formai.api.clients.domain.model.commands.UpdateClientCommand;
import com.formai.api.clients.domain.model.valueobjects.ActivationTicket;
import com.formai.api.clients.domain.model.valueobjects.RegisteredClient;

import java.util.Optional;

public interface ClientCommandService {

    Optional<RegisteredClient> handle(RegisterClientCommand command);

    Optional<Client> handle(UpdateClientCommand command);

    Optional<ActivationTicket> handle(RenewActivationCodeCommand command);

    void handle(ActivateClientCommand command);

    Optional<Client> handle(DeactivateClientCommand command);

    Optional<Client> handle(UpdateBodyProfileCommand command);
}
