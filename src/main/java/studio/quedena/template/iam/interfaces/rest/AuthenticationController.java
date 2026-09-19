package studio.quedena.template.iam.interfaces.rest;

import studio.quedena.template.iam.application.internal.outboundservices.tokens.TokenService;
import studio.quedena.template.iam.domain.exceptions.InvalidCredentialsException;
import studio.quedena.template.iam.domain.services.UserCommandService;
import studio.quedena.template.iam.interfaces.rest.resources.SignInResource;
import studio.quedena.template.iam.interfaces.rest.resources.SignUpResource;
import studio.quedena.template.iam.interfaces.rest.resources.UserResource;
import studio.quedena.template.iam.interfaces.rest.transform.UserAssembler;
import studio.quedena.template.shared.config.JwtCookieFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return ResponseEntity.noContent().build();
    }
}
