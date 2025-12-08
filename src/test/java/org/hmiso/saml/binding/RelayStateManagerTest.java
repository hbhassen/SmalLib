package org.hmiso.saml.binding;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RelayStateManagerTest {

    private static final class MutableClock extends Clock {
        private Instant current = Instant.parse("2024-01-01T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }
    }

    @Test
    void shouldStoreAndExpireRelayState_TC_BIND_03() {
        MutableClock clock = new MutableClock();
        RelayStateManager manager = new RelayStateManager(Duration.ofSeconds(5), clock);

        String relay = manager.generate("/protected");
        assertEquals("/protected", manager.get(relay));

        clock.advance(Duration.ofSeconds(10));
        assertNull(manager.get(relay));
    }
}
