package com.formai.iam.interfaces.rest;

import com.formai.iam.domain.exceptions.InvalidPasswordResetTokenException;
import com.formai.iam.domain.exceptions.PasswordPolicyViolationException;
import com.formai.iam.domain.model.commands.ResetPasswordCommand;
import com.formai.iam.domain.services.UserCommandService;
import com.formai.iam.interfaces.rest.resources.CreatePasswordResetResource;
import com.formai.iam.interfaces.rest.resources.PasswordResetResource;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class PasswordResetsControllerTest {

    private static final String BODY = "{\"token\":\"raw-token\",\"password\":\"newSecret123\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserCommandService userCommandService;

    @MockitoBean
    UserAssembler assembler;

    private void commandIsAssembled() {
        when(assembler.toCommand(any(CreatePasswordResetResource.class)))
                .thenReturn(new ResetPasswordCommand("raw-token", "newSecret123"));
    }

    @Test
    void shouldReturn201WhenPasswordIsReset() throws Exception {
        commandIsAssembled();
        when(assembler.toPasswordResetResource())
                .thenReturn(new PasswordResetResource(UserAssembler.PASSWORD_RESET_MESSAGE));

        mockMvc.perform(post("/api/v1/password-resets").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn422WhenTokenIsInvalid() throws Exception {
        commandIsAssembled();
        doThrow(new InvalidPasswordResetTokenException())
                .when(userCommandService).handle(any(ResetPasswordCommand.class));

        mockMvc.perform(post("/api/v1/password-resets").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturn422WhenNewPasswordViolatesPolicy() throws Exception {
        commandIsAssembled();
        doThrow(new PasswordPolicyViolationException())
                .when(userCommandService).handle(any(ResetPasswordCommand.class));

        mockMvc.perform(post("/api/v1/password-resets").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }
}
