package com.hmiso.saml.metadata;

import com.hmiso.saml.config.BindingType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MetadataServicesTest {

    @Test
    void parserShouldExtractEndpoints_TC_META_01() {
        MetadataParser parser = new MetadataParser();
        MetadataInfo info = parser.parse("idp", URI.create("https://idp/sso"), URI.create("https://idp/slo"),
                List.of(BindingType.HTTP_REDIRECT));

        assertEquals("idp", info.getEntityId());
        assertEquals(URI.create("https://idp/sso"), info.getSingleSignOnService());
        assertEquals(BindingType.HTTP_REDIRECT, info.getSupportedBindings().get(0));
    }

    @Test
    void cacheShouldExpireAfterTtl_TC_META_02() {
        Clock fixed = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        EntityDescriptorCache cache = new EntityDescriptorCache(Duration.ofSeconds(1), fixed);
        MetadataInfo info = new MetadataInfo("idp", URI.create("https://idp/sso"), null,
                List.of(BindingType.HTTP_POST));

        cache.store(info);
        Optional<MetadataInfo> cached = cache.get();
        assertTrue(cached.isPresent());

        EntityDescriptorCache expiredCache = new EntityDescriptorCache(Duration.ofSeconds(-1), fixed);
        expiredCache.store(info);
        assertTrue(expiredCache.get().isEmpty());
    }
}
