package com.formai.api.iam.interfaces.rest;

import com.formai.api.iam.application.internal.outboundservices.tokens.TokenService;
import com.formai.api.iam.domain.exceptions.InvalidCredentialsException;
import com.formai.api.iam.domain.exceptions.InvalidRefreshTokenException;
import com.formai.api.iam.domain.model.aggregates.RefreshToken;
import com.formai.api.iam.domain.model.aggregates.User;
import com.formai.api.iam.domain.model.commands.IssueRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RevokeRefreshTokenCommand;
import com.formai.api.iam.domain.model.commands.RotateRefreshTokenCommand;
import com.formai.api.iam.domain.services.RefreshTokenCommandService;
import com.formai.api.iam.domain.services.UserCommandService;
import com.formai.api.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.formai.api.iam.interfaces.rest.resources.SignInResource;
import com.formai.api.iam.interfaces.rest.resources.SignUpResource;
import com.formai.api.iam.interfaces.rest.resources.UserResource;
import com.formai.api.iam.interfaces.rest.transform.UserAssembler;
import com.formai.api.shared.config.JwtCookieFactory;
import com.formai.api.shared.interfaces.rest.ApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Every operation here is permitAll() in SecurityConfig, so each one carries an empty
// @SecurityRequirements to override the global cookieAuth requirement declared in
// OpenApiConfiguration — otherwise Swagger UI would wrongly show these as needing the
// JWT cookie that they're the ones issuing in the first place.
@Tag(name = ApiTags.ACCOUNT_ACCESS, description = ApiTags.ACCOUNT_ACCESS_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/authentication")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final RefreshTokenCommandService refreshTokenCommandService;
    private final TokenService tokenService;
    private final UserAssembler assembler;
    private final JwtCookieFactory cookieFactory;

    public AuthenticationController(UserCommandService userCommandService,
                                     RefreshTokenCommandService refreshTokenCommandService,
                                     TokenService tokenService,
                                     UserAssembler assembler,
                                     JwtCookieFactory cookieFactory) {
        this.userCommandService = userCommandService;
        this.refreshTokenCommandService = refreshTokenCommandService;
        this.tokenService = tokenService;
        this.assembler = assembler;
        this.cookieFactory = cookieFactory;
    }

    @Operation(summary = "Register a new trainer account",
            description = "Creates an active trainer account (REGISTERED_USER and TRAINER roles) " +
                    "with a securely hashed password. Does not sign the account in — call " +
                    "sign-in afterwards to obtain the JWT cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing full name, malformed email or blank password",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "An account with this email already exists",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "Password is not between 8 and 128 characters long",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/sign-up")
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpResource resource) {
        var command = assembler.toCommand(resource);
        var user = userCommandService.handle(command)
                .orElseThrow(() -> new IllegalStateException("Sign-up should never return empty"));
        return new ResponseEntity<>(assembler.toResource(user), HttpStatus.CREATED);
    }

    // The JWT never travels in the response body: it goes in an httpOnly cookie
    // (see JwtCookieFactory) so client-side JavaScript — and therefore XSS — can
    // never read it. The browser attaches it automatically on later requests.
    @Operation(summary = "Authenticate and issue a JWT",
            description = "Verifies the credentials and the client application, and sets the JWT as an httpOnly, " +
                    "SameSite=Lax cookie on the response, plus a refresh_token cookie that renews it through " +
                    "POST /refresh. A browser client on a different origin must send the request with " +
                    "credentials included for the cookies to be stored (see CorsConfig).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated — JWT and refresh cookies set",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = "httpOnly, Secure, SameSite=Lax JWT and refresh_token cookies " +
                                    "(see JwtCookieFactory)",
                            schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))),
            @ApiResponse(responseCode = "400", description = "Malformed email, blank password or unknown application",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials — same generic message " +
                    "whether the email doesn't exist or the password is wrong, to prevent user enumeration",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "The account cannot sign in from this application " +
                    "(clients use the mobile app; trainers and administrators use the web platform)",
                    content = @Content),
            @ApiResponse(responseCode = "429", description = "Account locked for 15 minutes after 5 failed attempts",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInResource resource,
                                                            HttpServletResponse response) {
        var command = assembler.toCommand(resource);
        var user = userCommandService.handle(command)
                .orElseThrow(InvalidCredentialsException::new);
        var refreshToken = refreshTokenCommandService.handle(new IssueRefreshTokenCommand(user.getId()))
                .flatMap(RefreshToken::getRawValue)
                .orElseThrow(() -> new IllegalStateException("Issuing a refresh token should never return empty"));
        setSessionCookies(response, user, refreshToken);
        return ResponseEntity.ok(assembler.toAuthenticatedResource(user));
    }

    // The access JWT lasts minutes; when it expires the app calls this endpoint instead of asking
    // for the password again. Each refresh token works once: it is replaced by a new one.
    @Operation(summary = "Renew the session",
            description = "Uses the refresh_token cookie to issue a new JWT cookie and a new refresh_token " +
                    "cookie. The refresh token used stops working; reusing it revokes every session of the account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session renewed — JWT and refresh cookies replaced",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = "New JWT and refresh_token cookies", schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))),
            @ApiResponse(responseCode = "401", description = "Missing, expired, revoked or reused refresh token, " +
                    "or a disabled account — sign in again", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticatedUserResource> refresh(
            @CookieValue(name = JwtCookieFactory.REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        var session = refreshTokenCommandService.handle(new RotateRefreshTokenCommand(refreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        setSessionCookies(response, session.user(), session.refreshToken());
        return ResponseEntity.ok(assembler.toAuthenticatedResource(session.user()));
    }

    // JS cannot delete an httpOnly cookie itself, so ending a session server-side
    // is required even for plain logout (not just for forced/admin revocation).
    @Operation(summary = "Sign out",
            description = "Revokes the refresh token and clears both cookies server-side, since client-side " +
                    "JavaScript cannot delete an httpOnly cookie itself.")
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Signed out — cookies cleared",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = "Expired JWT and refresh_token cookies (see JwtCookieFactory)",
                            schema = @Schema(type = "string")))
    )
    @SecurityRequirements
    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(
            @CookieValue(name = JwtCookieFactory.REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenCommandService.handle(new RevokeRefreshTokenCommand(refreshToken));
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearRefresh().toString());
        return ResponseEntity.noContent().build();
    }

    private void setSessionCookies(HttpServletResponse response, User user, String refreshToken) {
        var token = tokenService.issueFor(user.getId().toString(), user.getRoles());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.issue(token).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.issueRefresh(refreshToken).toString());
    }
}
