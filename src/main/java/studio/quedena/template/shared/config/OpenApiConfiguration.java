package studio.quedena.template.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

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
