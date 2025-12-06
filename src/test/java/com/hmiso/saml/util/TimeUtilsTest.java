package com.hmiso.saml.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeUtilsTest {

    @Test
    void shouldApplyClockSkewWindow_TC_UTIL_02() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        assertTrue(TimeUtils.isWithinClockSkew(now, now.minusSeconds(30), now.plusSeconds(30), Duration.ofMinutes(1)));
        assertFalse(TimeUtils.isWithinClockSkew(now, now.plusSeconds(10), now.plusSeconds(20), Duration.ZERO));
    }
}
