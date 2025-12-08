package org.hmiso.saml.binding;

import org.hmiso.saml.config.BindingType;
import org.hmiso.saml.util.CompressionUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BindingEncodersTest {

    @Test
    void redirectEncoderShouldCompressAndBase64_TC_BIND_01() {
        RedirectBindingEncoder encoder = new RedirectBindingEncoder();
        String message = "<AuthnRequest>payload</AuthnRequest>";

        BindingMessage result = encoder.encode(message, URI.create("https://idp.example.com/sso"), "relay");

        assertEquals(BindingType.HTTP_REDIRECT, result.getBindingType());
        assertEquals(message, CompressionUtils.inflateFromBase64(result.getPayload()));
        assertEquals("relay", result.getRelayState());
    }

    @Test
    void postEncoderShouldBase64_TC_BIND_02() {
        PostBindingEncoder encoder = new PostBindingEncoder();
        String message = "<LogoutRequest>payload</LogoutRequest>";

        BindingMessage result = encoder.encode(message, URI.create("https://idp.example.com/slo"), "relay2");

        assertEquals(BindingType.HTTP_POST, result.getBindingType());
        assertEquals(message, new String(Base64.getDecoder().decode(result.getPayload())));
        assertNotNull(result.getDestination());
    }
}
