package studio.quedena.template.profiles.application.internal.commandservices;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.domain.model.commands.CreateProfileCommand;
import studio.quedena.template.profiles.domain.model.commands.UpdateProfileCommand;
import studio.quedena.template.profiles.domain.repositories.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCommandServiceImplTest {

    @Mock
    ProfileRepository profileRepository;

    @InjectMocks
    ProfileCommandServiceImpl service;

    @Test
    void shouldCreateEmptyProfileForHolder() {
        // Arrange
        var command = new CreateProfileCommand("holder-123");
        var captor = ArgumentCaptor.forClass(Profile.class);
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(command);

        // Assert
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getHolderId()).isEqualTo("holder-123");
        assertThat(result.getHolderId()).isEqualTo("holder-123");
    }

    // Regression test for the self-healing behavior: the UserRegistered handler may
    // fail after the iam commit (see UserRegisteredEventHandler), so updating a
    // holder without a profile yet must create it instead of failing.
    @Test
    void shouldSelfHealByCreatingProfileWhenUpdatingAMissingOne() {
        // Arrange
        var command = new UpdateProfileCommand("holder-123", "Ada", "Lovelace");
        when(profileRepository.findByHolderId("holder-123")).thenReturn(Optional.empty());
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(command);

        // Assert
        assertThat(result.getHolderId()).isEqualTo("holder-123");
        assertThat(result.getFirstName()).isEqualTo("Ada");
        assertThat(result.getLastName()).isEqualTo("Lovelace");
    }

    @Test
    void shouldUpdateAnExistingProfile() {
        // Arrange
        var existing = Profile.createEmptyFor("holder-123");
        var command = new UpdateProfileCommand("holder-123", "Ada", "Lovelace");
        when(profileRepository.findByHolderId("holder-123")).thenReturn(Optional.of(existing));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.handle(command);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("Ada");
        assertThat(result.getLastName()).isEqualTo("Lovelace");
    }
}
