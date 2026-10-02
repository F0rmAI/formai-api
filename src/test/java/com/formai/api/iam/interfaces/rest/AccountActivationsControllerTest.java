package com.formai.api.iam.interfaces.rest;

import com.formai.api.iam.domain.exceptions.ConsentRequiredException;
import com.formai.api.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.formai.api.iam.domain.exceptions.InvalidActivationCodeException;
import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.ActivateAccountCommand;
import com.formai.api.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.api.iam.domain.model.valueobjects.Email;
import com.formai.api.iam.domain.services.UserCommandService;
import com.formai.api.iam.interfaces.rest.resources.AccountActivationResource;
import com.formai.api.iam.interfaces.rest.resources.CreateAccountActivationResource;
import com.formai.api.iam.interfaces.rest.transform.UserAssembler;
import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountActivationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class AccountActivationsControllerTest {

    private static final String BODY =
            "{\"activationCode\":\"ABCD2345\",\"email\":\"Client@FormAI.com\",\"password\":\"secret123\","
                    + "\"consentAccepted\":true,\"consentVersion\":\"1.0\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserCommandService userCommandService;

    @MockitoBean
    UserAssembler assembler;

    private void commandIsAssembled() {
        when(assembler.toCommand(any(CreateAccountActivationResource.class)))
                .thenReturn(new ActivateAccountCommand("ABCD2345", new Email("client@formai.com"), "secret123", true, "1.0"));
    }

    @Test
    void shouldReturn201WhenAccountIsActivated() throws Exception {
        // Arrange
        var client = User.createPendingClient(new CreateClientAccountCommand(), Instant.now());
        commandIsAssembled();
        when(userCommandService.handle(any(ActivateAccountCommand.class))).thenReturn(Optional.of(client));
        when(assembler.toActivationResource(client))
                .thenReturn(new AccountActivationResource(client.getId(), "ACTIVE", Instant.now()));

        // Act & Assert
        mockMvc.perform(post("/api/v1/account-activations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn400WhenActivationCodeIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/account-activations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activationCode\":\"\",\"email\":\"client@formai.com\",\"password\":\"secret123\","
                                + "\"consentAccepted\":true,\"consentVersion\":\"1.0\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenConsentVersionIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/account-activations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activationCode\":\"ABCD2345\",\"email\":\"client@formai.com\","
                                + "\"password\":\"secret123\",\"consentAccepted\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenEmailIsMissingOrMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/account-activations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activationCode\":\"ABCD2345\",\"password\":\"secret123\","
                                + "\"consentAccepted\":true,\"consentVersion\":\"1.0\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/account-activations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activationCode\":\"ABCD2345\",\"email\":\"client@formai\","
                                + "\"password\":\"secret123\",\"consentAccepted\":true,\"consentVersion\":\"1.0\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenEmailBelongsToAnotherAccount() throws Exception {
        commandIsAssembled();
        when(userCommandService.handle(any(ActivateAccountCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException("client@formai.com"));

        mockMvc.perform(post("/api/v1/account-activations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn422WhenActivationCodeIsInvalid() throws Exception {
        commandIsAssembled();
        when(userCommandService.handle(any(ActivateAccountCommand.class)))
                .thenThrow(new InvalidActivationCodeException());

        mockMvc.perform(post("/api/v1/account-activations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturn422WhenConsentIsNotAccepted() throws Exception {
        commandIsAssembled();
        when(userCommandService.handle(any(ActivateAccountCommand.class))).thenThrow(new ConsentRequiredException());

        mockMvc.perform(post("/api/v1/account-activations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }
}
