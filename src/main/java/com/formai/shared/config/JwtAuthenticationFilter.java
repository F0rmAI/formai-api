package com.formai.shared.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${formai.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        extractToken(request).ifPresent(token -> {
            try {
                var claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                var holderId = claims.getSubject();
                var authorities = extractAuthorities(claims.get("roles", List.class));
                var authentication = new UsernamePasswordAuthenticationToken(holderId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception invalidToken) {
                SecurityContextHolder.clearContext();
            }
        });
        filterChain.doFilter(request, response);
    }

    // Each role in the "roles" claim becomes a "ROLE_<name>" authority, e.g. a token
    // with roles ["REGISTERED_USER", "ADMINISTRATOR"] grants both ROLE_REGISTERED_USER
    // and ROLE_ADMINISTRATOR — roles are additive, never mutually exclusive.
    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractAuthorities(List<?> rawRoles) {
        if (rawRoles == null) {
            return List.of();
        }
        return ((List<String>) rawRoles).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    // The JWT travels in an httpOnly cookie, not the Authorization header — JS (and
    // therefore XSS) can never read it. See shared/config/JwtCookieFactory.
    private Optional<String> extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> JwtCookieFactory.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
