package org.hmiso.saml.binding;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Génère et valide des RelayState opaques avec expiration simple en mémoire.
 */
public class RelayStateManager implements RelayStateStore {
    private final Map<String, StoredRelayState> store = new ConcurrentHashMap<>();
    private final Duration timeToLive;
    private final Clock clock;

    public RelayStateManager(Duration timeToLive, Clock clock) {
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    public String generate(String originalRequestUrl) {
        // E13 / TC-BIND-03 : générer un identifiant opaque persistant pour relayer l'URL d'origine avec TTL.
        String relayState = UUID.randomUUID().toString();
        save(relayState, originalRequestUrl);
        return relayState;
    }

    @Override
    public void save(String relayState, String originalRequestUrl) {
        store.put(relayState, new StoredRelayState(originalRequestUrl, clock.instant().plus(timeToLive)));
    }

    @Override
    public String get(String relayState) {
        StoredRelayState stored = store.get(relayState);
        if (stored == null || stored.expiresAt.isBefore(clock.instant())) {
            invalidate(relayState);
            return null;
        }
        return stored.originalUrl;
    }

    @Override
    public void invalidate(String relayState) {
        store.remove(relayState);
    }

    private record StoredRelayState(String originalUrl, Instant expiresAt) {
    }
}
