package studio.quedena.template.iam.interfaces.rest;

import studio.quedena.template.iam.application.internal.outboundservices.tokens.TokenService;
import studio.quedena.template.iam.domain.exceptions.InvalidCredentialsException;
import studio.quedena.template.iam.domain.services.UserCommandService;
import studio.quedena.template.iam.interfaces.rest.resources.SignInResource;
import studio.quedena.template.iam.interfaces.rest.resources.SignUpResource;
import studio.quedena.template.iam.interfaces.rest.resources.UserResource;
import studio.quedena.template.iam.interfaces.rest.transform.UserAssembler;
import studio.quedena.template.shared.config.JwtCookieFactory;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Every operation here is permitAll() in SecurityConfig, so each one carries an empty
// @SecurityRequirements to override the global cookieAuth requirement declared in
// OpenApiConfiguration — otherwise Swagger UI would wrongly show these as needing the
// JWT cookie that they're the ones issuing in the first place.
@Tag(name = "Authentication", description = "Sign-up, sign-in and sign-out. The issued JWT " +
        "travels only in an httpOnly cookie (see JwtCookieFactory), never in the response body.")
@RestController
@RequestMapping("/api/v1/authentication")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final TokenService tokenService;
    private final UserAssembler assembler;
    private final JwtCookieFactory cookieFactory;

    public AuthenticationController(UserCommandService userCommandService,
                                     TokenService tokenService,
                                     UserAssembler assembler,
                                     JwtCookieFactory cookieFactory) {
        this.userCommandService = userCommandService;
        this.tokenService = tokenService;
        this.assembler = assembler;
        this.cookieFactory = cookieFactory;
    }

    @Operation(summary = "Register a new account",
            description = "Creates a user with a securely hashed password and the default " +
                    "REGISTERED_USER role. Does not sign the account in — call sign-in " +
                    "afterwards to obtain the JWT cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "400", description = "Malformed email or password shorter than 8 characters",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "An account with this email already exists",
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
            description = "Verifies the credentials and sets the JWT as an httpOnly, " +
                    "SameSite=Lax cookie on the response. A browser client on a different " +
                    "origin must send the request with credentials included for the cookie " +
                    "to be stored (see CorsConfig).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated — JWT cookie set",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = "httpOnly, Secure, SameSite=Lax JWT cookie (see JwtCookieFactory)",
                            schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "400", description = "Malformed email or blank password",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials — same generic message " +
                    "whether the email doesn't exist or the password is wrong, to prevent user enumeration",
                    content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/sign-in")
    public ResponseEntity<UserResource> signIn(@Valid @RequestBody SignInResource resource, HttpServletResponse response) {
        var command = assembler.toCommand(resource);
        var user = userCommandService.handle(command)
                .orElseThrow(InvalidCredentialsException::new);
        var token = tokenService.issueFor(user.getId().toString(), user.getRoles());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.issue(token).toString());
        return ResponseEntity.ok(assembler.toResource(user));
    }

    // JS cannot delete an httpOnly cookie itself, so ending a session server-side
    // is required even for plain logout (not just for forced/admin revocation).
    @Operation(summary = "Sign out",
            description = "Clears the JWT cookie server-side, since client-side JavaScript " +
                    "cannot delete an httpOnly cookie itself.")
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Signed out — JWT cookie cleared",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = "Expired JWT cookie (see JwtCookieFactory.clear())",
                            schema = @Schema(type = "string")))
    )
    @SecurityRequirements
    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return ResponseEntity.noContent().build();
    }
}
