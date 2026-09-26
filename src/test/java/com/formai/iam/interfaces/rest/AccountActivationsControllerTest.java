package com.formai.iam.interfaces.rest;

import com.formai.iam.domain.exceptions.ConsentRequiredException;
import com.formai.iam.domain.exceptions.InvalidActivationCodeException;
import com.formai.iam.domain.model.aggregates.User;
import com.formai.iam.domain.model.commands.ActivateAccountCommand;
import com.formai.iam.domain.model.commands.CreateClientAccountCommand;
import com.formai.iam.domain.model.valueobjects.Email;
import com.formai.iam.domain.services.UserCommandService;
import com.formai.iam.interfaces.rest.resources.AccountActivationResource;
import com.formai.iam.interfaces.rest.resources.CreateAccountActivationResource;
import com.formai.iam.interfaces.rest.transform.UserAssembler;
import com.formai.shared.config.JwtAuthenticationFilter;
import com.formai.shared.config.SecurityConfig;
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
            "{\"activationCode\":\"ABCD2345\",\"password\":\"secret123\",\"consentAccepted\":true}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserCommandService userCommandService;

    @MockitoBean
    UserAssembler assembler;

    private void commandIsAssembled() {
        when(assembler.toCommand(any(CreateAccountActivationResource.class)))
                .thenReturn(new ActivateAccountCommand("ABCD2345", "secret123", true));
    }

    @Test
    void shouldReturn201WhenAccountIsActivated() throws Exception {
        // Arrange
        var client = User.createPendingClient(new CreateClientAccountCommand(new Email("client@formai.com")), Instant.now());
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
                        .content("{\"activationCode\":\"\",\"password\":\"secret123\",\"consentAccepted\":true}"))
                .andExpect(status().isBadRequest());
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
