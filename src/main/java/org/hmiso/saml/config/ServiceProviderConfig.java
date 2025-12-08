package org.hmiso.saml.config;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Configuration du fournisseur de service (SP).
 */
public final class ServiceProviderConfig {
    private final String entityId;
    private final URI assertionConsumerServiceUrl;
    private final URI singleLogoutServiceUrl;
    private final String nameIdFormat;
    private final BindingType authnRequestBinding;
    private final boolean wantAssertionsSigned;
    private final List<String> supportedNameIdFormats;

    public ServiceProviderConfig(String entityId,
                                 URI assertionConsumerServiceUrl,
                                 URI singleLogoutServiceUrl,
                                 String nameIdFormat,
                                 BindingType authnRequestBinding,
                                 boolean wantAssertionsSigned,
                                 List<String> supportedNameIdFormats) {
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.assertionConsumerServiceUrl = Objects.requireNonNull(assertionConsumerServiceUrl, "assertionConsumerServiceUrl");
        this.singleLogoutServiceUrl = singleLogoutServiceUrl;
        this.nameIdFormat = Objects.requireNonNull(nameIdFormat, "nameIdFormat");
        this.authnRequestBinding = Objects.requireNonNull(authnRequestBinding, "authnRequestBinding");
        this.wantAssertionsSigned = wantAssertionsSigned;
        this.supportedNameIdFormats = List.copyOf(Objects.requireNonNull(supportedNameIdFormats, "supportedNameIdFormats"));
    }

    public String getEntityId() {
        return entityId;
    }

    public URI getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }

    public URI getSingleLogoutServiceUrl() {
        return singleLogoutServiceUrl;
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public BindingType getAuthnRequestBinding() {
        return authnRequestBinding;
    }

    public boolean isWantAssertionsSigned() {
        return wantAssertionsSigned;
    }

    public List<String> getSupportedNameIdFormats() {
        return supportedNameIdFormats;
    }
}
