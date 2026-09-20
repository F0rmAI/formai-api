package studio.quedena.template.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain jwtFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                // CSRF token machinery stays disabled: the JWT now travels in a cookie the
                // browser attaches automatically, so SameSite=Lax on that cookie
                // (JwtCookieFactory) is the CSRF mitigation for this stateless JSON API,
                // not Spring's form-oriented CSRF token filter. See CorsConfig for the
                // cross-origin browser frontend scenario.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/authentication/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()   // healthchecks send no JWT
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // TEMPLATE NOTE: roles are additive (iam.domain.model.valueobjects.Role) and
                        // land as ROLE_<name> authorities via JwtAuthenticationFilter. To restrict a
                        // route to a specific role, add a matcher BEFORE anyRequest().authenticated():
                        // .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMINISTRATOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
