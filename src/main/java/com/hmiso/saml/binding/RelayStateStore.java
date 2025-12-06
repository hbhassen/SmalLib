package com.hmiso.saml.binding;

/**
 * Abstraction du stockage côté serveur pour les RelayState opaques.
 */
public interface RelayStateStore {
    void save(String relayState, String originalRequestUrl);

    String get(String relayState);

    void invalidate(String relayState);
}
