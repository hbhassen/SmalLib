package org.hmiso.saml.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Utilitaires XML sécurisés (protection XXE, canonicalisation minimaliste).
 */
public final class XmlUtils {
    private XmlUtils() {
    }

    public static DocumentBuilderFactory newSecureDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de configurer le parser XML sécurisé", e);
        }
        return factory;
    }
}
