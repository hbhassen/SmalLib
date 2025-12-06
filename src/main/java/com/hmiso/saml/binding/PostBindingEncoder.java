package com.hmiso.saml.binding;

import com.hmiso.saml.config.BindingType;

import java.net.URI;
import java.util.Base64;
import java.util.Objects;

/**
 * Encodeur pour le binding HTTP-POST (Base64 simple du message XML).
 */
public class PostBindingEncoder {
    public BindingMessage encode(String xmlMessage, URI destination, String relayState) {
        // E12 / TC-BIND-02 : envelopper le message SAML en Base64 pour un formulaire HTTP-POST auto-soumis.
        Objects.requireNonNull(xmlMessage, "xmlMessage");
        Objects.requireNonNull(destination, "destination");
        String encoded = Base64.getEncoder().encodeToString(xmlMessage.getBytes());
        return new BindingMessage(BindingType.HTTP_POST, destination, encoded, relayState);
    }
}
