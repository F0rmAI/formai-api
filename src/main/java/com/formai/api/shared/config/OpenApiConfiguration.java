package com.formai.api.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.formai.api.shared.interfaces.rest.ApiTags;
import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfiguration {

    private static final String COOKIE_AUTH_SCHEME = "cookieAuth";

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${documentation.application.description}")
    private String applicationDescription;

    @Value("${documentation.application.version}")
    private String applicationVersion;

    @Bean 
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .description(applicationDescription)
                        .version(applicationVersion)
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH_SCHEME, cookieAuthScheme()))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH_SCHEME));
    }

    // Swagger UI lists the tags in the order of the spec's tags array, which springdoc fills in the
    // order it discovers the controllers. This customizer runs once the spec is built and replaces
    // that array with the journey order, from signing in to checking progress.
    @Bean
    public OpenApiCustomizer tagsInJourneyOrderCustomizer() {
        return openApi -> openApi.setTags(new ArrayList<>(tagsInJourneyOrder()));
    }

    private static List<Tag> tagsInJourneyOrder() {
        return List.of(
                tag(ApiTags.ACCOUNT_ACCESS, ApiTags.ACCOUNT_ACCESS_DESCRIPTION),
                tag(ApiTags.CLIENTS, ApiTags.CLIENTS_DESCRIPTION),
                tag(ApiTags.EXERCISES, ApiTags.EXERCISES_DESCRIPTION),
                tag(ApiTags.ROUTINES, ApiTags.ROUTINES_DESCRIPTION),
                tag(ApiTags.WORKOUTS, ApiTags.WORKOUTS_DESCRIPTION),
                tag(ApiTags.PROGRESS, ApiTags.PROGRESS_DESCRIPTION));
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }

    // The JWT travels in an httpOnly cookie (JwtCookieFactory), never a Bearer header, so
    // Swagger UI's padlock has nothing to paste — this scheme documents the real transport.
    // Authenticate via "Try it out" on sign-in first; the browser stores the Set-Cookie and
    // every later "Try it out" call on a protected endpoint sends it automatically.
    private SecurityScheme cookieAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(JwtCookieFactory.COOKIE_NAME);
    }
}
