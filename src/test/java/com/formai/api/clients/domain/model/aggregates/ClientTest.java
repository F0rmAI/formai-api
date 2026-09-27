package com.formai.api.clients.domain.model.aggregates;

import com.formai.api.clients.domain.exceptions.InvalidBodyProfileException;
import com.formai.api.clients.domain.model.commands.RegisterTrainerCommand;
import com.formai.api.clients.domain.model.commands.UpdateClientCommand;
import com.formai.api.clients.domain.model.valueobjects.BodyWeight;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import com.formai.api.clients.domain.model.valueobjects.Height;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.formai.api.clients.ClientsTestData.CLIENT_ID;
import static com.formai.api.clients.ClientsTestData.TODAY;
import static com.formai.api.clients.ClientsTestData.TRAINER_HOLDER_ID;
import static com.formai.api.clients.ClientsTestData.activeClient;
import static com.formai.api.clients.ClientsTestData.bodyProfileCommand;
import static com.formai.api.clients.ClientsTestData.invitedClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientTest {

    @Test
    void shouldRegisterAnInvitedClientWithTheIdOfItsIamAccount() {
        // Act
        var client = invitedClient();

        // Assert
        assertThat(client.getId()).isEqualTo(CLIENT_ID);
        assertThat(client.getHolderId()).isEqualTo(TRAINER_HOLDER_ID);
        assertThat(client.getStatus()).isEqualTo(ClientStatus.INVITED);
        assertThat(client.getBodyProfile()).isEmpty();
        assertThat(client.canRenewActivationCode()).isTrue();
        assertThat(client.isActive()).isFalse();
    }

    @Test
    void shouldOnlyRenewTheCodeWhileInvited() {
        var client = invitedClient();
        client.activate();

        assertThat(client.isActive()).isTrue();
        assertThat(client.canRenewActivationCode()).isFalse();
    }

    @Test
    void shouldKeepADeactivatedClientInactiveEvenIfItsAccountIsActivatedLater() {
        var client = activeClient();
        client.deactivate();
        client.activate();

        assertThat(client.getStatus()).isEqualTo(ClientStatus.INACTIVE);
    }

    @Test
    void shouldRename() {
        var client = invitedClient();
        client.rename(new UpdateClientCommand(CLIENT_ID, TRAINER_HOLDER_ID, new FullName("  Luis R. ")));

        assertThat(client.getFullName().value()).isEqualTo("Luis R.");
    }

    @Test
    void shouldCreateTheBodyProfileWithItsFirstWeight() {
        var client = activeClient();
        client.updateBodyProfile(bodyProfileCommand("80.5"), TODAY);

        var profile = client.getBodyProfile().orElseThrow();
        assertThat(profile.getGoal().value()).isEqualTo("Hypertrophy");
        assertThat(profile.getHeight().centimeters()).isEqualTo(175);
        assertThat(profile.getCurrentWeight().kilograms()).isEqualByComparingTo("80.5");
        assertThat(profile.getRestrictions()).isEqualTo("Left knee injury");
        assertThat(profile.getWeightHistory()).hasSize(1);
    }

    @Test
    void shouldKeepThePreviousWeightWithItsDate() {
        var client = activeClient();
        client.updateBodyProfile(bodyProfileCommand("80.5"), TODAY);
        client.updateBodyProfile(bodyProfileCommand("79"), TODAY.plusDays(14));

        var history = client.getBodyProfile().orElseThrow().getWeightHistory();
        assertThat(history).hasSize(2);
        assertThat(history.getFirst().weight().kilograms()).isEqualByComparingTo("80.5");
        assertThat(history.getFirst().recordedOn()).isEqualTo(TODAY);
        assertThat(history.getLast().recordedOn()).isEqualTo(TODAY.plusDays(14));
    }

    @Test
    void shouldNotRecordTheSameWeightTwice() {
        var client = activeClient();
        client.updateBodyProfile(bodyProfileCommand("80.5"), TODAY);
        client.updateBodyProfile(bodyProfileCommand("80.50"), TODAY.plusDays(1));

        assertThat(client.getBodyProfile().orElseThrow().getWeightHistory()).hasSize(1);
    }

    @Test
    void shouldRejectAHeightOutOfRangeNamingTheField() {
        assertThatThrownBy(() -> new Height(99))
                .isInstanceOf(InvalidBodyProfileException.class)
                .extracting("field").isEqualTo("heightCm");
        assertThatThrownBy(() -> new Height(251)).isInstanceOf(InvalidBodyProfileException.class);
        assertThat(new Height(100).centimeters()).isEqualTo(100);
        assertThat(new Height(250).centimeters()).isEqualTo(250);
    }

    @Test
    void shouldRejectAWeightOfZeroOrLess() {
        assertThatThrownBy(() -> new BodyWeight(BigDecimal.ZERO))
                .isInstanceOf(InvalidBodyProfileException.class)
                .extracting("field").isEqualTo("weightKg");
        assertThatThrownBy(() -> new BodyWeight(new BigDecimal("-1"))).isInstanceOf(InvalidBodyProfileException.class);
    }

    @Test
    void shouldNormalizeTheEmail() {
        assertThat(new Email("Luis@FormAI.com").value()).isEqualTo("luis@formai.com");
        assertThatThrownBy(() -> new Email("luis@formai")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRegisterATrainer() {
        var trainer = Trainer.register(new RegisterTrainerCommand(TRAINER_HOLDER_ID, new FullName("Ana Torres"),
                new Email("ana@formai.com")));

        assertThat(trainer.getId()).isNotNull();
        assertThat(trainer.getHolderId()).isEqualTo(TRAINER_HOLDER_ID);
        assertThat(trainer.getRegisteredAt()).isNotNull();
    }
}
