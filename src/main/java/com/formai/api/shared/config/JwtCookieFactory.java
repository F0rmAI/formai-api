package com.formai.api.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JwtCookieFactory {

    public static final String COOKIE_NAME = "token";
    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    // The refresh cookie only travels to the authentication endpoints (sign-in, refresh,
    // sign-out), never with the rest of the API calls.
    private static final String REFRESH_COOKIE_PATH = "/api/v1/authentication";

    private final long expirationMinutes;
    private final long refreshExpirationDays;
    private final boolean secure;

    // secure is true everywhere except local development over plain HTTP: the mobile app's
    // network stack (iOS and Android) never sends a Secure cookie to http://localhost, so
    // without this switch a client could sign in but every later call would arrive without a session.
    public JwtCookieFactory(@Value("${formai.jwt.expiration-minutes:30}") long expirationMinutes,
                            @Value("${formai.jwt.refresh-expiration-days:7}") long refreshExpirationDays,
                            @Value("${formai.jwt.cookie-secure:true}") boolean secure) {
        this.expirationMinutes = expirationMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
        this.secure = secure;
    }

    public ResponseCookie issue(String token) {
        return build(COOKIE_NAME, token, "/", Duration.ofMinutes(expirationMinutes));
    }

    public ResponseCookie clear() {
        return build(COOKIE_NAME, "", "/", Duration.ZERO);
    }

    public ResponseCookie issueRefresh(String refreshToken) {
        return build(REFRESH_COOKIE_NAME, refreshToken, REFRESH_COOKIE_PATH, Duration.ofDays(refreshExpirationDays));
    }

    public ResponseCookie clearRefresh() {
        return build(REFRESH_COOKIE_NAME, "", REFRESH_COOKIE_PATH, Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
