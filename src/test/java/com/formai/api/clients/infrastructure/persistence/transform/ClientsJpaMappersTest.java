package com.formai.api.clients.infrastructure.persistence.transform;

import com.formai.api.clients.domain.model.aggregates.Trainer;
import com.formai.api.clients.domain.model.commands.RegisterTrainerCommand;
import com.formai.api.clients.domain.model.valueobjects.ClientStatus;
import com.formai.api.clients.domain.model.valueobjects.Email;
import com.formai.api.clients.domain.model.valueobjects.FullName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.formai.api.clients.ClientsTestData.CLIENT_ID;
import static com.formai.api.clients.ClientsTestData.TODAY;
import static com.formai.api.clients.ClientsTestData.TRAINER_HOLDER_ID;
import static com.formai.api.clients.ClientsTestData.activeClient;
import static com.formai.api.clients.ClientsTestData.bodyProfileCommand;
import static com.formai.api.clients.ClientsTestData.invitedClient;
import static org.assertj.core.api.Assertions.assertThat;

class ClientsJpaMappersTest {

    private final ClientJpaMapper clientMapper = Mappers.getMapper(ClientJpaMapper.class);
    private final TrainerJpaMapper trainerMapper = Mappers.getMapper(TrainerJpaMapper.class);

    @Test
    void shouldRestoreAClientWithoutBodyProfile() {
        // Arrange
        var client = invitedClient();

        // Act
        var entity = clientMapper.toEntity(client);
        var restored = clientMapper.toDomain(entity);

        // Assert
        assertThat(entity.getStatus()).isEqualTo("INVITED");
        assertThat(entity.getBodyProfile()).isNull();
        assertThat(restored.getId()).isEqualTo(CLIENT_ID);
        assertThat(restored.getEmail()).isEqualTo(client.getEmail());
        assertThat(restored.getStatus()).isEqualTo(ClientStatus.INVITED);
        assertThat(restored.getBodyProfile()).isEmpty();
    }

    @Test
    void shouldRestoreTheBodyProfileWithItsWeightHistory() {
        var client = activeClient();
        client.updateBodyProfile(bodyProfileCommand("80.5"), TODAY);
        client.updateBodyProfile(bodyProfileCommand("79"), TODAY.plusDays(14));

        var entity = clientMapper.toEntity(client);
        var restored = clientMapper.toDomain(entity);

        assertThat(entity.getBodyProfile().getHeightCm()).isEqualTo(175);
        assertThat(entity.getWeightHistory()).hasSize(2);
        var profile = restored.getBodyProfile().orElseThrow();
        assertThat(profile.getGoal().value()).isEqualTo("Hypertrophy");
        assertThat(profile.getCurrentWeight().kilograms()).isEqualByComparingTo("79");
        assertThat(profile.getRestrictions()).isEqualTo("Left knee injury");
        assertThat(profile.getWeightHistory()).extracting(record -> record.recordedOn())
                .containsExactly(TODAY, TODAY.plusDays(14));
    }

    @Test
    void shouldRestoreATrainer() {
        var trainer = Trainer.register(new RegisterTrainerCommand(TRAINER_HOLDER_ID, new FullName("Ana Torres"),
                new Email("ana@formai.com")));

        var restored = trainerMapper.toDomain(trainerMapper.toEntity(trainer));

        assertThat(restored.getId()).isEqualTo(trainer.getId());
        assertThat(restored.getHolderId()).isEqualTo(TRAINER_HOLDER_ID);
        assertThat(restored.getFullName()).isEqualTo(trainer.getFullName());
        assertThat(restored.getEmail()).isEqualTo(trainer.getEmail());
        assertThat(restored.getRegisteredAt()).isEqualTo(trainer.getRegisteredAt());
    }
}
