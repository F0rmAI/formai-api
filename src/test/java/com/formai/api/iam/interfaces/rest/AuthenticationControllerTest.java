package com.formai.api.iam.interfaces.rest;

import com.formai.api.iam.application.internal.outboundservices.tokens.TokenService;
import com.formai.api.iam.domain.exceptions.AccountLockedException;
import com.formai.api.iam.domain.exceptions.ApplicationNotAllowedException;
import com.formai.api.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.formai.api.iam.domain.exceptions.PasswordPolicyViolationException;
import com.formai.api.iam.domain.model.aggregates.RefreshToken;
import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.IssueRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RevokeRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RotateRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.SignInCommand;
import com.formai.api.iam.domain.model.commands.SignUpCommand;
import com.formai.api.iam.domain.model.valueobjects.ClientApplication;
import com.formai.api.iam.domain.model.valueobjects.Email;
import com.formai.api.iam.domain.model.valueobjects.HashedPassword;
import com.formai.api.iam.domain.model.valueobjects.RefreshedSession;
import com.formai.api.iam.domain.services.RefreshTokenCommandService;
import com.formai.api.iam.domain.services.UserCommandService;
import com.formai.api.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.formai.api.iam.interfaces.rest.resources.SignInResource;
import com.formai.api.iam.interfaces.rest.resources.SignUpResource;
import com.formai.api.iam.interfaces.rest.resources.UserResource;
import com.formai.api.iam.interfaces.rest.transform.UserAssembler;
import com.formai.api.shared.config.JwtAuthenticationFilter;
import com.formai.api.shared.config.JwtCookieFactory;
import com.formai.api.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtCookieFactory.class})
@TestPropertySource(properties = "formai.jwt.secret=test-secret-only-for-wiring-not-a-real-value")
class AuthenticationControllerTest {

    private static final String SIGN_UP_BODY =
            "{\"fullName\":\"Ana Trainer\",\"email\":\"trainer@formai.com\",\"password\":\"secret123\"}";
    private static final String SIGN_IN_BODY =
            "{\"email\":\"trainer@formai.com\",\"password\":\"secret123\",\"application\":\"WEB_PLATFORM\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserCommandService userCommandService;

    @MockitoBean
    RefreshTokenCommandService refreshTokenCommandService;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    UserAssembler assembler;

    private static User trainer() {
        return User.registerTrainer(new SignUpCommand(new Email("trainer@formai.com"), "secret123", "Ana Trainer"),
                new HashedPassword("hashed"));
    }

    private static RefreshToken refreshTokenOf(User user) {
        return RefreshToken.issue(new IssueRefreshTokenCommand(user.getId()), Instant.now(), Duration.ofDays(7));
    }

    private void signUpCommandIsAssembled() {
        when(assembler.toCommand(any(SignUpResource.class)))
                .thenReturn(new SignUpCommand(new Email("trainer@formai.com"), "secret123", "Ana Trainer"));
    }

    private void signInCommandIsAssembled() {
        when(assembler.toCommand(any(SignInResource.class)))
                .thenReturn(new SignInCommand(new Email("trainer@formai.com"), "secret123", ClientApplication.WEB_PLATFORM));
    }

    @Test
    void shouldReturn201WhenTrainerSignsUp() throws Exception {
        // Arrange
        var user = trainer();
        signUpCommandIsAssembled();
        when(userCommandService.handle(any(SignUpCommand.class))).thenReturn(Optional.of(user));
        when(assembler.toResource(user)).thenReturn(new UserResource(user.getId(), "trainer@formai.com",
                Set.of("REGISTERED_USER", "TRAINER")));

        // Act & Assert
        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_UP_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("trainer@formai.com"));
    }

    @Test
    void shouldReturn400WhenFullNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"trainer@formai.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenEmailIsAlreadyRegistered() throws Exception {
        signUpCommandIsAssembled();
        when(userCommandService.handle(any(SignUpCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException("trainer@formai.com"));

        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_UP_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn422WhenPasswordViolatesPolicy() throws Exception {
        signUpCommandIsAssembled();
        when(userCommandService.handle(any(SignUpCommand.class))).thenThrow(new PasswordPolicyViolationException());

        mockMvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_UP_BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturn200AndSetJwtCookieWhenCredentialsAreValid() throws Exception {
        // Arrange
        var user = trainer();
        signInCommandIsAssembled();
        when(userCommandService.handle(any(SignInCommand.class))).thenReturn(Optional.of(user));
        var refreshToken = refreshTokenOf(user);
        when(refreshTokenCommandService.handle(any(IssueRefreshTokenCommand.class))).thenReturn(Optional.of(refreshToken));
        when(tokenService.issueFor(anyString(), anySet())).thenReturn("signed-jwt");
        when(assembler.toAuthenticatedResource(user)).thenReturn(new AuthenticatedUserResource(user.getId(),
                "trainer@formai.com", Set.of("REGISTERED_USER", "TRAINER"), "ACTIVE"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_IN_BODY))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItems(
                        containsString(JwtCookieFactory.COOKIE_NAME + "=signed-jwt"),
                        containsString(JwtCookieFactory.REFRESH_COOKIE_NAME + "=" + refreshToken.getRawValue().orElseThrow()))))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn200AndRenewBothCookiesWhenRefreshing() throws Exception {
        // Arrange
        var user = trainer();
        var next = refreshTokenOf(user);
        when(refreshTokenCommandService.handle(new RotateRefreshTokenCommand("current-refresh")))
                .thenReturn(Optional.of(new RefreshedSession(user, next.getRawValue().orElseThrow())));
        when(tokenService.issueFor(anyString(), anySet())).thenReturn("renewed-jwt");
        when(assembler.toAuthenticatedResource(user)).thenReturn(new AuthenticatedUserResource(user.getId(),
                "trainer@formai.com", Set.of("REGISTERED_USER", "TRAINER"), "ACTIVE"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/authentication/refresh")
                        .cookie(new Cookie(JwtCookieFactory.REFRESH_COOKIE_NAME, "current-refresh")))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItems(
                        containsString(JwtCookieFactory.COOKIE_NAME + "=renewed-jwt"),
                        containsString(JwtCookieFactory.REFRESH_COOKIE_NAME + "=" + next.getRawValue().orElseThrow()))))
                .andExpect(jsonPath("$.email").value("trainer@formai.com"));
    }

    @Test
    void shouldReturn401WhenRefreshingWithoutACookie() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenTheRefreshTokenIsNotUsable() throws Exception {
        when(refreshTokenCommandService.handle(new RotateRefreshTokenCommand("revoked-refresh")))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/authentication/refresh")
                        .cookie(new Cookie(JwtCookieFactory.REFRESH_COOKIE_NAME, "revoked-refresh")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenApplicationIsUnknown() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"trainer@formai.com\",\"password\":\"secret123\",\"application\":\"DESKTOP\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        signInCommandIsAssembled();
        when(userCommandService.handle(any(SignInCommand.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_IN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenApplicationIsNotAllowed() throws Exception {
        signInCommandIsAssembled();
        when(userCommandService.handle(any(SignInCommand.class))).thenThrow(new ApplicationNotAllowedException());

        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_IN_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn429WhenAccountIsLocked() throws Exception {
        signInCommandIsAssembled();
        when(userCommandService.handle(any(SignInCommand.class)))
                .thenThrow(new AccountLockedException(Instant.parse("2026-09-25T10:15:00Z")));

        mockMvc.perform(post("/api/v1/authentication/sign-in")
                        .contentType(MediaType.APPLICATION_JSON).content(SIGN_IN_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.lockedUntil").exists());
    }

    @Test
    void shouldReturn204AndClearCookieWhenSigningOut() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-out"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void shouldRevokeTheRefreshTokenAndClearBothCookiesWhenSigningOut() throws Exception {
        mockMvc.perform(post("/api/v1/authentication/sign-out")
                        .cookie(new Cookie(JwtCookieFactory.REFRESH_COOKIE_NAME, "current-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItems(
                        containsString(JwtCookieFactory.COOKIE_NAME + "=;"),
                        containsString(JwtCookieFactory.REFRESH_COOKIE_NAME + "=;"))));

        verify(refreshTokenCommandService).handle(new RevokeRefreshTokenCommand("current-refresh"));
    }
}
