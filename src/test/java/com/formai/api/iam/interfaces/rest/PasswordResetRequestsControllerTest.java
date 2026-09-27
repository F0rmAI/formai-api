package com.formai.api.iam.interfaces.rest;

import com.formai.api.iam.domain.model.commands.RequestPasswordResetCommand;
import com.formai.api.iam.domain.model.valueobjects.Email;
import com.formai.api.iam.domain.services.UserCommandService;
import com.formai.api.iam.interfaces.rest.resources.CreatePasswordResetRequestResource;
import com.formai.api.iam.interfaces.rest.resources.PasswordResetRequestResource;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetRequestsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class PasswordResetRequestsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserCommandService userCommandService;

    @MockitoBean
    UserAssembler assembler;

    @Test
    void shouldReturn201WithGenericMessageWhenResetIsRequested() throws Exception {
        // Arrange
        var command = new RequestPasswordResetCommand(new Email("trainer@formai.com"));
        when(assembler.toCommand(any(CreatePasswordResetRequestResource.class))).thenReturn(command);
        when(assembler.toPasswordResetRequestResource())
                .thenReturn(new PasswordResetRequestResource(UserAssembler.PASSWORD_RESET_REQUESTED_MESSAGE));

        // Act & Assert
        mockMvc.perform(post("/api/v1/password-reset-requests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"trainer@formai.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(UserAssembler.PASSWORD_RESET_REQUESTED_MESSAGE));
        verify(userCommandService).handle(command);
    }

    @Test
    void shouldReturn400WhenEmailIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/password-reset-requests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }
}
