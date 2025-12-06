package com.hmiso.saml.config;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

/**
 * Configuration de l'IdP SAML.
 */
public final class IdentityProviderConfig {
    private final String entityId;
    private final URI singleSignOnServiceUrl;
    private final URI singleLogoutServiceUrl;
    private final X509Certificate signingCertificate;
    private final X509Certificate encryptionCertificate;
    private final URI metadataUrl;
    private final boolean wantAssertionsSigned;
    private final boolean wantMessagesSigned;
    private final List<BindingType> supportedBindings;

    public IdentityProviderConfig(String entityId,
                                  URI singleSignOnServiceUrl,
                                  URI singleLogoutServiceUrl,
                                  X509Certificate signingCertificate,
                                  X509Certificate encryptionCertificate,
                                  URI metadataUrl,
                                  boolean wantAssertionsSigned,
                                  boolean wantMessagesSigned,
                                  List<BindingType> supportedBindings) {
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.singleSignOnServiceUrl = Objects.requireNonNull(singleSignOnServiceUrl, "singleSignOnServiceUrl");
        this.singleLogoutServiceUrl = singleLogoutServiceUrl;
        this.signingCertificate = signingCertificate;
        this.encryptionCertificate = encryptionCertificate;
        this.metadataUrl = metadataUrl;
        this.wantAssertionsSigned = wantAssertionsSigned;
        this.wantMessagesSigned = wantMessagesSigned;
        this.supportedBindings = List.copyOf(Objects.requireNonNull(supportedBindings, "supportedBindings"));
    }

    public String getEntityId() {
        return entityId;
    }

    public URI getSingleSignOnServiceUrl() {
        return singleSignOnServiceUrl;
    }

    public URI getSingleLogoutServiceUrl() {
        return singleLogoutServiceUrl;
    }

    public X509Certificate getSigningCertificate() {
        return signingCertificate;
    }

    public X509Certificate getEncryptionCertificate() {
        return encryptionCertificate;
    }

    public URI getMetadataUrl() {
        return metadataUrl;
    }

    public boolean isWantAssertionsSigned() {
        return wantAssertionsSigned;
    }

    public boolean isWantMessagesSigned() {
        return wantMessagesSigned;
    }

    public List<BindingType> getSupportedBindings() {
        return supportedBindings;
    }
}
