package com.hmiso.saml.metadata;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Cache TTL pour EntityDescriptor.
 */
public class EntityDescriptorCache {
    private final Duration ttl;
    private final Clock clock;
    private CachedEntity cachedEntity;

    public EntityDescriptorCache(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    public void store(MetadataInfo info) {
        cachedEntity = new CachedEntity(info, clock.instant().plus(ttl));
    }

    public Optional<MetadataInfo> get() {
        if (cachedEntity == null || cachedEntity.expiresAt.isBefore(clock.instant())) {
            cachedEntity = null;
            return Optional.empty();
        }
        return Optional.of(cachedEntity.info);
    }

    public void invalidate() {
        cachedEntity = null;
    }

    private record CachedEntity(MetadataInfo info, Instant expiresAt) {
    }
}
