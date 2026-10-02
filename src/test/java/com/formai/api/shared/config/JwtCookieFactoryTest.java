package com.formai.api.shared.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtCookieFactoryTest {

    @Test
    void shouldIssueSecureHttpOnlyCookiesByDefault() {
        var factory = new JwtCookieFactory(30, 7, true);

        assertThat(factory.issue("jwt").toString()).contains("Secure").contains("HttpOnly").contains("SameSite=Lax");
        assertThat(factory.issueRefresh("refresh").toString()).contains("Secure").contains("HttpOnly");
    }

    @Test
    void shouldDropOnlyTheSecureFlagForLocalDevelopmentOverHttp() {
        var factory = new JwtCookieFactory(30, 7, false);

        assertThat(factory.issue("jwt").toString()).doesNotContain("Secure").contains("HttpOnly");
        assertThat(factory.issueRefresh("refresh").toString()).doesNotContain("Secure").contains("HttpOnly");
        assertThat(factory.clear().toString()).doesNotContain("Secure");
    }
}
