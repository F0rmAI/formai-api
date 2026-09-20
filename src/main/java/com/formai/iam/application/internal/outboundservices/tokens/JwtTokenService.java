package com.formai.iam.application.internal.outboundservices.tokens;

import com.formai.iam.domain.model.valueobjects.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

@Service
public class JwtTokenService implements TokenService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtTokenService(@Value("${formai.jwt.secret}") String secret,
                            @Value("${formai.jwt.expiration-minutes:30}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String issueFor(String holderId, Set<Role> roles) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(holderId)
                .claim("roles", roles.stream().map(Enum::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }
}
