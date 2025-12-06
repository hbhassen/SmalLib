package com.hmiso.saml.binding;

import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.util.CompressionUtils;

import java.net.URI;
import java.util.Objects;

/**
 * Encodeur pour le binding HTTP-Redirect (compression + Base64).
 */
public class RedirectBindingEncoder {
    public BindingMessage encode(String xmlMessage, URI destination, String relayState) {
        // E11 / TC-BIND-01 : encoder HTTP-Redirect via deflate + Base64 et conserver le RelayState.
        Objects.requireNonNull(xmlMessage, "xmlMessage");
        Objects.requireNonNull(destination, "destination");
        String compressed = CompressionUtils.deflateToBase64(xmlMessage);
        return new BindingMessage(BindingType.HTTP_REDIRECT, destination, compressed, relayState);
    }
}
