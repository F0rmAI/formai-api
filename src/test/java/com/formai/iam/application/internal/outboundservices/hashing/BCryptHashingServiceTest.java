package com.formai.iam.application.internal.outboundservices.hashing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptHashingServiceTest {

    private final BCryptHashingService hashingService = new BCryptHashingService();

    @Test
    void shouldMatchPasswordWhenHashedWithSameService() {
        var hashed = hashingService.hash("secret123");

        assertThat(hashingService.matches("secret123", hashed)).isTrue();
        assertThat(hashingService.matches("secret124", hashed)).isFalse();
    }

    @Test
    void shouldHashPasswordWhenLongerThanBcryptLimit() {
        var longPassword = "a".repeat(128);

        var hashed = hashingService.hash(longPassword);

        assertThat(hashingService.matches(longPassword, hashed)).isTrue();
        assertThat(hashingService.matches("a".repeat(127), hashed)).isFalse();
    }
}
