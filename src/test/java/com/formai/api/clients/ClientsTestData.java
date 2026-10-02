package com.formai.api.clients;

import com.formai.api.clients.domain.model.aggregates.Client;
import com.formai.api.clients.domain.model.commands.RegisterClientCommand;
import com.formai.api.clients.domain.model.commands.UpdateBodyProfileCommand;
import com.formai.api.clients.domain.model.valueobjects.ActivationTicket;
import com.formai.api.clients.domain.model.valueobjects.BodyWeight;
import com.formai.api.clients.domain.model.valueobjects.ClientId;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.model.valueobjects.Height;
import com.formai.api.clients.domain.model.valueobjects.TrainingGoal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ClientsTestData {

    public static final String TRAINER_HOLDER_ID = "22222222-2222-2222-2222-222222222222";
    public static final ClientId CLIENT_ID = new ClientId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    public static final Email CLIENT_EMAIL = new Email("luis@formai.com");
    public static final Instant CODE_EXPIRES_AT = Instant.parse("2026-10-04T12:00:00Z");
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    private ClientsTestData() {
    }

    public static RegisterClientCommand registerCommand() {
        return new RegisterClientCommand(TRAINER_HOLDER_ID, new FullName("Luis Ramos"));
    }

    public static ActivationTicket ticket() {
        return new ActivationTicket(CLIENT_ID, "ABCD2345", CODE_EXPIRES_AT);
    }

    public static Client invitedClient() {
        return Client.register(registerCommand(), ticket());
    }

    public static Client activeClient() {
        var client = invitedClient();
        client.activate(CLIENT_EMAIL);
        return client;
    }

    public static UpdateBodyProfileCommand bodyProfileCommand(String weightKg) {
        return new UpdateBodyProfileCommand(CLIENT_ID, TRAINER_HOLDER_ID, new TrainingGoal("Hypertrophy"),
                new Height(175), new BodyWeight(new BigDecimal(weightKg)), "Left knee injury");
    }
}
