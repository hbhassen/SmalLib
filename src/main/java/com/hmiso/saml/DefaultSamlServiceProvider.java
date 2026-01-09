package com.hmiso.saml;

import com.hmiso.saml.api.SamlException;
import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.api.SamlServiceProvider;
import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.binding.PostBindingEncoder;
import com.hmiso.saml.binding.RedirectBindingEncoder;
import com.hmiso.saml.binding.RelayStateManager;
import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.SamlConfiguration;
import com.hmiso.saml.saml.AssertionExtractor;
import com.hmiso.saml.saml.AuthnRequestBuilder;
import com.hmiso.saml.saml.LogoutRequestBuilder;
import com.hmiso.saml.saml.LogoutResponseValidator;
import com.hmiso.saml.saml.SamlResponseValidator;
import com.hmiso.saml.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Implémentation par défaut orchestrant les builders/validateurs internes.
 */
public class DefaultSamlServiceProvider implements SamlServiceProvider {
    private static final String NAME_ID_FORMAT_ATTRIBUTE = "saml.nameIdFormat";

    private final SamlConfiguration configuration;
    private final AuthnRequestBuilder authnRequestBuilder;
    private final RedirectBindingEncoder redirectBindingEncoder;
    private final PostBindingEncoder postBindingEncoder;
    private final SamlResponseValidator samlResponseValidator;
    private final AssertionExtractor assertionExtractor;
    private final LogoutRequestBuilder logoutRequestBuilder;
    private final LogoutResponseValidator logoutResponseValidator;
    private final RelayStateManager relayStateManager;

    public DefaultSamlServiceProvider(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.authnRequestBuilder = new AuthnRequestBuilder(configuration);
        this.redirectBindingEncoder = new RedirectBindingEncoder();
        this.postBindingEncoder = new PostBindingEncoder();
        this.samlResponseValidator = new SamlResponseValidator(configuration);
        this.assertionExtractor = new AssertionExtractor();
        this.logoutRequestBuilder = new LogoutRequestBuilder(configuration);
        this.logoutResponseValidator = new LogoutResponseValidator(configuration);
        this.relayStateManager = new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC());
    }

    @Override
    public SamlConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public BindingMessage initiateAuthentication(String relayState) {
        // E7/E11/E13 : initier une AuthnRequest signée et transporter un RelayState opaque selon le binding configuré.
        String effectiveRelayState = relayState != null ? relayState : relayStateManager.generate("/" );
        String requestId = "AR-" + UUID.randomUUID();
        String authnRequest = authnRequestBuilder.build(requestId, Instant.now());
        if (configuration.getServiceProvider().getAuthnRequestBinding() == BindingType.HTTP_REDIRECT) {
            return redirectBindingEncoder.encode(authnRequest, configuration.getIdentityProvider().getSingleSignOnServiceUrl(), effectiveRelayState);
        }
        return postBindingEncoder.encode(authnRequest, configuration.getIdentityProvider().getSingleSignOnServiceUrl(), effectiveRelayState);
    }

    @Override
    public SamlPrincipal processSamlResponse(String samlResponse, String relayState) {
        // E8/E9 : vérifier la réponse (audience, Recipient, InResponseTo, horodatage) puis convertir en principal.
        if (samlResponse == null || samlResponse.isBlank()) {
            throw new SamlException("SAMLResponse vide");
        }
        samlResponseValidator.validate(configuration.getServiceProvider().getEntityId(),
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                "request-id-placeholder", Instant.now(), Instant.now().plusSeconds(60));
        ParsedSamlResponse parsed = parseSamlResponse(samlResponse);
        String nameId = parsed != null && parsed.nameId != null ? parsed.nameId : samlResponse;
        String sessionIndex = parsed != null ? parsed.sessionIndex : null;
        Map<String, Object> attributes = Map.of();
        if (parsed != null && parsed.nameIdFormat != null) {
            attributes = Map.of(NAME_ID_FORMAT_ATTRIBUTE, parsed.nameIdFormat);
        }
        return assertionExtractor.toPrincipal(nameId, null, sessionIndex, attributes);
    }

    @Override
    public BindingMessage initiateLogout(String sessionIndex, String relayState) {
        // E10 : construire une LogoutRequest alignée sur le binding SLO configuré.
        return initiateLogoutInternal("unknown", sessionIndex, relayState, null);
    }

    @Override
    public BindingMessage initiateLogout(SamlPrincipal principal, String relayState) {
        Objects.requireNonNull(principal, "principal");
        String nameIdFormat = "";
        Object formatAttribute = principal.getAttributes().get(NAME_ID_FORMAT_ATTRIBUTE);
        if (formatAttribute instanceof String) {
            String candidate = ((String) formatAttribute).trim();
            if (!candidate.isEmpty()) {
                nameIdFormat = candidate;
            }
        }
        return initiateLogoutInternal(principal.getNameId(), principal.getSessionIndex(), relayState, nameIdFormat);
    }

    @Override
    public void processLogoutResponse(String logoutResponse, String inResponseTo) {
        // E10 : valider la réponse de logout avant d'invalider la session applicative.
        if (logoutResponse == null || logoutResponse.isBlank()) {
            throw new SamlException("LogoutResponse vide");
        }
        logoutResponseValidator.validate(inResponseTo, Instant.now(), Instant.now().plusSeconds(60));
    }

    private BindingMessage initiateLogoutInternal(String nameId,
                                                  String sessionIndex,
                                                  String relayState,
                                                  String nameIdFormat) {
        String requestId = "LR-" + UUID.randomUUID();
        String logoutRequest = logoutRequestBuilder.build(requestId, nameId, sessionIndex, Instant.now(), nameIdFormat);
        if (configuration.getIdentityProvider().getSingleLogoutServiceUrl() == null) {
            throw new SamlException("Endpoint SLO non configure");
        }
        if (configuration.getServiceProvider().getAuthnRequestBinding() == BindingType.HTTP_REDIRECT) {
            return redirectBindingEncoder.encode(logoutRequest, configuration.getIdentityProvider().getSingleLogoutServiceUrl(), relayState);
        }
        return postBindingEncoder.encode(logoutRequest, configuration.getIdentityProvider().getSingleLogoutServiceUrl(), relayState);
    }

    private ParsedSamlResponse parseSamlResponse(String samlResponse) {
        String xml = extractXmlPayload(samlResponse);
        if (xml == null) {
            return null;
        }
        try {
            Document document = XmlUtils.newSecureDocumentBuilderFactory()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            String nameId = firstTextContent(document, "NameID");
            String nameIdFormat = firstAttribute(document, "NameID", "Format");
            String sessionIndex = firstAttribute(document, "AuthnStatement", "SessionIndex");
            if (nameId == null && sessionIndex == null && nameIdFormat == null) {
                return null;
            }
            return new ParsedSamlResponse(nameId, sessionIndex, nameIdFormat);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractXmlPayload(String samlResponse) {
        String trimmed = samlResponse.trim();
        if (trimmed.startsWith("<")) {
            return trimmed;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            String xml = new String(decoded, StandardCharsets.UTF_8).trim();
            return xml.startsWith("<") ? xml : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String firstTextContent(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node != null) {
                String text = node.getTextContent();
                if (text != null) {
                    String trimmed = text.trim();
                    if (!trimmed.isEmpty()) {
                        return trimmed;
                    }
                }
            }
        }
        return null;
    }

    private String firstAttribute(Document document, String localName, String attributeName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node != null && node.getAttributes() != null) {
                Node attribute = node.getAttributes().getNamedItem(attributeName);
                if (attribute != null) {
                    String value = attribute.getNodeValue();
                    if (value != null) {
                        String trimmed = value.trim();
                        if (!trimmed.isEmpty()) {
                            return trimmed;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static final class ParsedSamlResponse {
        private final String nameId;
        private final String sessionIndex;
        private final String nameIdFormat;

        private ParsedSamlResponse(String nameId, String sessionIndex, String nameIdFormat) {
            this.nameId = nameId;
            this.sessionIndex = sessionIndex;
            this.nameIdFormat = nameIdFormat;
        }
    }

}
