package org.hmiso.saml.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityCoverageTest {

    @Test
    void compressionRoundTrip_TC_UTIL_03() {
        String xml = "<Request>demo</Request>";

        String compressed = CompressionUtils.deflateToBase64(xml);
        assertEquals(xml, CompressionUtils.inflateFromBase64(compressed));
    }

    @Test
    void loggingMasksSecrets_TC_UTIL_04() {
        assertEquals("pa****rd", LoggingUtils.maskSecret("password"));
        assertEquals("****", LoggingUtils.maskSecret(null));
        assertTrue(LoggingUtils.getLogger(UtilityCoverageTest.class) != null);
    }
}
