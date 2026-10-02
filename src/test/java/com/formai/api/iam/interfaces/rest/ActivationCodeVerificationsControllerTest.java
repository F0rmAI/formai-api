package com.formai.api.iam.interfaces.rest;

import com.formai.api.iam.domain.model.entities.ActivationCode;
import com.formai.api.iam.domain.model.queries.GetUsableActivationCodeQuery;
import com.formai.api.iam.domain.services.UserQueryService;
import com.formai.api.iam.interfaces.rest.resources.ActivationCodeVerificationResource;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivationCodeVerificationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class ActivationCodeVerificationsControllerTest {

    private static final String BODY = "{\"activationCode\":\"ABCD2345\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserQueryService userQueryService;

    @MockitoBean
    UserAssembler assembler;

    @Test
    void shouldReturn201WhenActivationCodeIsUsable() throws Exception {
        // Arrange
        var expiresAt = Instant.parse("2026-10-04T12:00:00Z");
        var activationCode = new ActivationCode("ABCD2345", expiresAt);
        when(userQueryService.handle(new GetUsableActivationCodeQuery("ABCD2345")))
                .thenReturn(Optional.of(activationCode));
        when(assembler.toVerificationResource(activationCode))
                .thenReturn(new ActivationCodeVerificationResource(expiresAt));

        // Act & Assert (no JWT: the client has no account session yet)
        mockMvc.perform(post("/api/v1/activation-code-verifications").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").value("2026-10-04T12:00:00Z"));
    }

    @Test
    void shouldReturn400WhenActivationCodeIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/activation-code-verifications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activationCode\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn422WhenActivationCodeIsUnknownOrExpired() throws Exception {
        when(userQueryService.handle(new GetUsableActivationCodeQuery("ABCD2345"))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/activation-code-verifications").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }
}
