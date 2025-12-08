package org.hmiso.saml.binding;

import org.hmiso.saml.config.BindingType;

import java.net.URI;
import java.util.Objects;

/**
 * Message encodé selon un binding HTTP (Redirect ou POST).
 */
public final class BindingMessage {
    private final BindingType bindingType;
    private final URI destination;
    private final String payload;
    private final String relayState;

    public BindingMessage(BindingType bindingType, URI destination, String payload, String relayState) {
        this.bindingType = Objects.requireNonNull(bindingType, "bindingType");
        this.destination = Objects.requireNonNull(destination, "destination");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.relayState = relayState;
    }

    public BindingType getBindingType() {
        return bindingType;
    }

    public URI getDestination() {
        return destination;
    }

    public String getPayload() {
        return payload;
    }

    public String getRelayState() {
        return relayState;
    }
}
