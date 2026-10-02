package com.formai.api.iam.domain.model.aggregates;

import com.formai.api.iam.domain.model.commands.IssueRefreshTokenCommand;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-09-30T10:00:00Z");
    private static final Duration SEVEN_DAYS = Duration.ofDays(7);
    private static final UUID USER_ID = UUID.randomUUID();

    private static RefreshToken issued() {
        return RefreshToken.issue(new IssueRefreshTokenCommand(USER_ID), NOW, SEVEN_DAYS);
    }

    @Test
    void shouldStoreOnlyTheHashOfTheRawValue() {
        var token = issued();

        var raw = token.getRawValue().orElseThrow();
        assertThat(token.getTokenHash()).isEqualTo(RefreshToken.hashOf(raw)).isNotEqualTo(raw);
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(SEVEN_DAYS));
    }

    @Test
    void shouldIssueADifferentValueEachTime() {
        assertThat(issued().getRawValue()).isNotEqualTo(issued().getRawValue());
    }

    @Test
    void shouldBeUsableUntilItExpires() {
        var token = issued();

        assertThat(token.isUsableAt(NOW.plus(Duration.ofDays(6)))).isTrue();
        assertThat(token.isUsableAt(NOW.plus(SEVEN_DAYS))).isFalse();
    }

    @Test
    void shouldStopBeingUsableOnceRevokedAndKeepTheFirstRevocationDate() {
        var token = issued();

        token.revoke(NOW.plusSeconds(60));
        token.revoke(NOW.plusSeconds(120));

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isUsableAt(NOW.plusSeconds(90))).isFalse();
        assertThat(token.getRevokedAt()).isEqualTo(NOW.plusSeconds(60));
    }
}
