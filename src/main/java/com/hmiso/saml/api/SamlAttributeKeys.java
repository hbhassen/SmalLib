package com.hmiso.saml.api;

/**
 * Keys utilises pour enrichir les attributs du {@link com.hmiso.saml.api.SamlPrincipal}.
 */
public final class SamlAttributeKeys {
    public static final String NAME_ID_FORMAT = "saml.nameIdFormat";
    public static final String NOT_BEFORE = "saml.notBefore";
    public static final String NOT_ON_OR_AFTER = "saml.notOnOrAfter";
    public static final String AUDIENCE = "saml.audience";
    public static final String IN_RESPONSE_TO = "saml.inResponseTo";
    public static final String DESTINATION = "saml.destination";
    public static final String ISSUER = "saml.issuer";
    public static final String ASSERTION_ID = "saml.assertionId";
    public static final String RESPONSE_ID = "saml.responseId";

    private SamlAttributeKeys() {
    }
}
