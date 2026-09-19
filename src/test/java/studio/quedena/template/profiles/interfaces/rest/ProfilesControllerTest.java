package studio.quedena.template.profiles.interfaces.rest;

import studio.quedena.template.profiles.domain.model.aggregates.Profile;
import studio.quedena.template.profiles.domain.model.commands.UpdateProfileCommand;
import studio.quedena.template.profiles.domain.model.queries.GetProfileByHolderIdQuery;
import studio.quedena.template.profiles.domain.services.ProfileCommandService;
import studio.quedena.template.profiles.domain.services.ProfileQueryService;
import studio.quedena.template.profiles.interfaces.rest.resources.ProfileResource;
import studio.quedena.template.profiles.interfaces.rest.resources.UpdateProfileResource;
import studio.quedena.template.profiles.interfaces.rest.transform.ProfileAssembler;
import studio.quedena.template.shared.config.JwtAuthenticationFilter;
import studio.quedena.template.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @Import brings in the REAL SecurityConfig + JwtAuthenticationFilter — not a slice
// default — so this test exercises the actual filter chain (JWT-cookie auth, CSRF
// disabled), not whatever Spring Boot would auto-configure if left unconfigured.
@WebMvcTest(ProfilesController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "template.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class ProfilesControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProfileCommandService commandService;

    @MockitoBean
    ProfileQueryService queryService;

    @MockitoBean
    ProfileAssembler assembler;

    @Test
    void shouldReturnProfileWhenAuthenticated() throws Exception {
        // Arrange
        var profile = Profile.createEmptyFor("holder-123");
        when(queryService.handle(any(GetProfileByHolderIdQuery.class))).thenReturn(Optional.of(profile));
        when(assembler.toResource(profile)).thenReturn(new ProfileResource(profile.getId(), null, null));

        // Act & Assert
        mockMvc.perform(get("/api/v1/profiles/me").with(user("holder-123")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        // Act & Assert — mirrors the real app: no JWT cookie means anonymous, and
        // anonymous fails anyRequest().authenticated() with 403, not a raw connection error.
        mockMvc.perform(get("/api/v1/profiles/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateProfileWithoutACsrfTokenSinceCsrfIsDisabled() throws Exception {
        // Arrange
        var command = new UpdateProfileCommand("holder-123", "Ada", "Lovelace");
        var updated = Profile.createEmptyFor("holder-123");
        updated.updatePersonalData("Ada", "Lovelace");
        when(assembler.toCommand(any(UpdateProfileResource.class), eq("holder-123"))).thenReturn(command);
        when(commandService.handle(command)).thenReturn(updated);
        when(assembler.toResource(updated)).thenReturn(new ProfileResource(updated.getId(), "Ada", "Lovelace"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/profiles/me")
                        .with(user("holder-123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
                .andExpect(status().isOk());
    }
}
