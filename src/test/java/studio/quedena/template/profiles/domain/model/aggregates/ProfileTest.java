package studio.quedena.template.profiles.domain.model.aggregates;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileTest {

    @Test
    void shouldCreateEmptyProfileForHolder() {
        // Act
        var profile = Profile.createEmptyFor("holder-123");

        // Assert
        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getHolderId()).isEqualTo("holder-123");
        assertThat(profile.getFirstName()).isNull();
        assertThat(profile.getLastName()).isNull();
        assertThat(profile.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldGenerateADifferentIdPerInstance() {
        // Act
        var first = Profile.createEmptyFor("holder-1");
        var second = Profile.createEmptyFor("holder-2");

        // Assert
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void shouldUpdatePersonalData() {
        // Arrange
        var profile = Profile.createEmptyFor("holder-123");

        // Act
        profile.updatePersonalData("Ada", "Lovelace");

        // Assert
        assertThat(profile.getFirstName()).isEqualTo("Ada");
        assertThat(profile.getLastName()).isEqualTo("Lovelace");
    }
}
