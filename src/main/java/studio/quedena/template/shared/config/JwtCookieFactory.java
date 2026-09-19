package studio.quedena.template.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JwtCookieFactory {

    public static final String COOKIE_NAME = "token";

    private final long expirationMinutes;

    public JwtCookieFactory(@Value("${template.jwt.expiration-minutes:30}") long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public ResponseCookie issue(String token) {
        return build(token, Duration.ofMinutes(expirationMinutes));
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
