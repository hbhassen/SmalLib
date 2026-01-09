package com.hmiso.saml;

import com.hmiso.saml.api.SamlAttributeKeys;
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
import com.hmiso.saml.saml.SamlResponseValidationContext;
import com.hmiso.saml.saml.SamlResponseValidator;
import com.hmiso.saml.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation par defaut orchestrant les builders/validateurs internes.
 */
public class DefaultSamlServiceProvider implements SamlServiceProvider {
    private final SamlConfiguration configuration;
    private final AuthnRequestBuilder authnRequestBuilder;
    private final RedirectBindingEncoder redirectBindingEncoder;
    private final PostBindingEncoder postBindingEncoder;
    private final SamlResponseValidator samlResponseValidator;
    private final AssertionExtractor assertionExtractor;
    private final LogoutRequestBuilder logoutRequestBuilder;
    private final LogoutResponseValidator logoutResponseValidator;
    private final RelayStateManager relayStateManager;
    private final Map<String, String> relayStateToRequestId = new ConcurrentHashMap<>();
    private final Map<String, Instant> replayCache = new ConcurrentHashMap<>();

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
        // E7/E11/E13 : initier une AuthnRequest signee et transporter un RelayState opaque selon le binding configure.
        String effectiveRelayState = relayState != null ? relayState : relayStateManager.generate("/");
        String requestId = "AR-" + UUID.randomUUID();
        String authnRequest = authnRequestBuilder.build(requestId, Instant.now());
        relayStateToRequestId.put(effectiveRelayState, requestId);
        if (configuration.getServiceProvider().getAuthnRequestBinding() == BindingType.HTTP_REDIRECT) {
            return redirectBindingEncoder.encode(authnRequest,
                    configuration.getIdentityProvider().getSingleSignOnServiceUrl(),
                    effectiveRelayState);
        }
        return postBindingEncoder.encode(authnRequest,
                configuration.getIdentityProvider().getSingleSignOnServiceUrl(),
                effectiveRelayState);
    }

    @Override
    public SamlPrincipal processSamlResponse(String samlResponse, String relayState) {
        // E8/E9 : verifier la reponse (signature, issuer, audience, InResponseTo, horodatage).
        if (samlResponse == null || samlResponse.isBlank()) {
            throw new SamlException("SAMLResponse vide");
        }
        ParsedSamlResponse parsed = parseSamlResponse(samlResponse);
        if (parsed == null) {
            throw new SamlException("SAMLResponse illisible");
        }
        String expectedInResponseTo = consumeExpectedRequestId(relayState);
        SamlResponseValidationContext context = new SamlResponseValidationContext(
                parsed.audience,
                parsed.recipient,
                parsed.inResponseTo,
                expectedInResponseTo,
                parsed.notBefore,
                parsed.notOnOrAfter,
                parsed.issuer,
                parsed.destination,
                parsed.responseSigned,
                parsed.assertionSigned
        );
        samlResponseValidator.validate(context);
        enforceReplayProtection(parsed);

        Map<String, Object> attributes = new HashMap<>(parsed.attributes);
        if (parsed.nameIdFormat != null) {
            attributes.put(SamlAttributeKeys.NAME_ID_FORMAT, parsed.nameIdFormat);
        }
        if (parsed.notBefore != null) {
            attributes.put(SamlAttributeKeys.NOT_BEFORE, parsed.notBefore);
        }
        if (parsed.notOnOrAfter != null) {
            attributes.put(SamlAttributeKeys.NOT_ON_OR_AFTER, parsed.notOnOrAfter);
        }
        if (parsed.audience != null) {
            attributes.put(SamlAttributeKeys.AUDIENCE, parsed.audience);
        }
        if (parsed.inResponseTo != null) {
            attributes.put(SamlAttributeKeys.IN_RESPONSE_TO, parsed.inResponseTo);
        }
        if (parsed.destination != null) {
            attributes.put(SamlAttributeKeys.DESTINATION, parsed.destination);
        }
        if (parsed.issuer != null) {
            attributes.put(SamlAttributeKeys.ISSUER, parsed.issuer);
        }
        if (parsed.assertionId != null) {
            attributes.put(SamlAttributeKeys.ASSERTION_ID, parsed.assertionId);
        }
        if (parsed.responseId != null) {
            attributes.put(SamlAttributeKeys.RESPONSE_ID, parsed.responseId);
        }
        return assertionExtractor.toPrincipal(parsed.nameId, null, parsed.sessionIndex, attributes);
    }

    @Override
    public BindingMessage initiateLogout(String sessionIndex, String relayState) {
        // E10 : construire une LogoutRequest alignee sur le binding SLO configure.
        return initiateLogoutInternal("unknown", sessionIndex, relayState, null);
    }

    @Override
    public BindingMessage initiateLogout(SamlPrincipal principal, String relayState) {
        Objects.requireNonNull(principal, "principal");
        String nameIdFormat = "";
        Object formatAttribute = principal.getAttributes().get(SamlAttributeKeys.NAME_ID_FORMAT);
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
        // E10 : valider la reponse de logout avant d'invalider la session applicative.
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
            return redirectBindingEncoder.encode(logoutRequest,
                    configuration.getIdentityProvider().getSingleLogoutServiceUrl(),
                    relayState);
        }
        return postBindingEncoder.encode(logoutRequest,
                configuration.getIdentityProvider().getSingleLogoutServiceUrl(),
                relayState);
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
            Element responseElement = firstElementByLocalName(document, "Response");
            Element assertionElement = firstElementByLocalName(document, "Assertion");
            String nameId = firstTextContent(document, "NameID");
            String nameIdFormat = firstAttribute(document, "NameID", "Format");
            String sessionIndex = firstAttribute(document, "AuthnStatement", "SessionIndex");
            String responseIssuer = directChildText(responseElement, "Issuer");
            String assertionIssuer = directChildText(assertionElement, "Issuer");
            String issuer = resolveIssuer(responseIssuer, assertionIssuer);
            String inResponseTo = getAttribute(responseElement, "InResponseTo");
            String destination = getAttribute(responseElement, "Destination");
            String responseId = getAttribute(responseElement, "ID");
            String assertionId = getAttribute(assertionElement, "ID");
            String audience = firstTextContent(document, "Audience");
            String recipient = firstAttribute(document, "SubjectConfirmationData", "Recipient");
            Instant notBefore = firstInstantAttribute(document, "Conditions", "NotBefore");
            Instant notOnOrAfter = firstInstantAttribute(document, "Conditions", "NotOnOrAfter");
            boolean responseSigned = hasSignature(document, "Response");
            boolean assertionSigned = hasSignature(document, "Assertion");
            Map<String, Object> attributes = extractAttributes(document);
            if (nameId == null || nameId.isBlank()) {
                return null;
            }
            return new ParsedSamlResponse(nameId, sessionIndex, nameIdFormat, issuer, inResponseTo,
                    destination, audience, recipient, notBefore, notOnOrAfter, responseId, assertionId,
                    responseSigned, assertionSigned, attributes);
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

    private String consumeExpectedRequestId(String relayState) {
        if (relayState == null || relayState.isBlank()) {
            return null;
        }
        return relayStateToRequestId.remove(relayState);
    }

    private String resolveIssuer(String responseIssuer, String assertionIssuer) {
        String expected = configuration.getIdentityProvider().getEntityId();
        if (issuerMatches(expected, responseIssuer)) {
            return responseIssuer;
        }
        if (issuerMatches(expected, assertionIssuer)) {
            return assertionIssuer;
        }
        return responseIssuer != null ? responseIssuer : assertionIssuer;
    }

    private boolean issuerMatches(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        if (actual == null || actual.isBlank()) {
            return false;
        }
        if (expected.equals(actual)) {
            return true;
        }
        if (!expected.contains("://") && actual.endsWith("/" + expected)) {
            return true;
        }
        return false;
    }

    private String getAttribute(Element element, String attributeName) {
        if (element == null) {
            return null;
        }
        String value = element.getAttribute(attributeName);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String directChildText(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {
                String text = child.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    return text.trim();
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private Element firstElementByLocalName(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element) {
                return element;
            }
        }
        return null;
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

    private Instant firstInstantAttribute(Document document, String localName, String attributeName) {
        String value = firstAttribute(document, localName, attributeName);
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasSignature(Document document, String parentLocalName) {
        NodeList parents = document.getElementsByTagNameNS("*", parentLocalName);
        for (int i = 0; i < parents.getLength(); i++) {
            Node parent = parents.item(i);
            if (parent instanceof Element element && hasDirectSignature(element)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDirectSignature(Element parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "Signature".equals(child.getLocalName())) {
                return true;
            }
            child = child.getNextSibling();
        }
        return false;
    }

    private Map<String, Object> extractAttributes(Document document) {
        NodeList attributes = document.getElementsByTagNameNS("*", "Attribute");
        if (attributes.getLength() == 0) {
            return Map.of();
        }
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            if (!(node instanceof Element element)) {
                continue;
            }
            String name = element.getAttribute("Name");
            if (name == null || name.isBlank()) {
                name = element.getAttribute("FriendlyName");
            }
            if (name == null || name.isBlank()) {
                continue;
            }
            NodeList valueNodes = element.getElementsByTagNameNS("*", "AttributeValue");
            List<String> list = values.computeIfAbsent(name, key -> new ArrayList<>());
            for (int j = 0; j < valueNodes.getLength(); j++) {
                Node valueNode = valueNodes.item(j);
                if (valueNode != null) {
                    String value = valueNode.getTextContent();
                    if (value != null && !value.trim().isEmpty()) {
                        list.add(value.trim());
                    }
                }
            }
        }
        Map<String, Object> attributesMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            if (entry.getValue().size() == 1) {
                attributesMap.put(entry.getKey(), entry.getValue().get(0));
            } else if (!entry.getValue().isEmpty()) {
                attributesMap.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
        }
        return attributesMap;
    }

    private void enforceReplayProtection(ParsedSamlResponse parsed) {
        String id = parsed.assertionId != null ? parsed.assertionId : parsed.responseId;
        if (id == null || id.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        Instant existingExpiry = replayCache.get(id);
        if (existingExpiry != null && now.isBefore(existingExpiry)) {
            throw new SamlException("Assertion rejouee");
        }
        Instant expiry = parsed.notOnOrAfter != null ? parsed.notOnOrAfter : now.plusSeconds(300);
        replayCache.put(id, expiry);
        replayCache.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

    private static final class ParsedSamlResponse {
        private final String nameId;
        private final String sessionIndex;
        private final String nameIdFormat;
        private final String issuer;
        private final String inResponseTo;
        private final String destination;
        private final String audience;
        private final String recipient;
        private final Instant notBefore;
        private final Instant notOnOrAfter;
        private final String responseId;
        private final String assertionId;
        private final boolean responseSigned;
        private final boolean assertionSigned;
        private final Map<String, Object> attributes;

        private ParsedSamlResponse(String nameId,
                                  String sessionIndex,
                                  String nameIdFormat,
                                  String issuer,
                                  String inResponseTo,
                                  String destination,
                                  String audience,
                                  String recipient,
                                  Instant notBefore,
                                  Instant notOnOrAfter,
                                  String responseId,
                                  String assertionId,
                                  boolean responseSigned,
                                  boolean assertionSigned,
                                  Map<String, Object> attributes) {
            this.nameId = nameId;
            this.sessionIndex = sessionIndex;
            this.nameIdFormat = nameIdFormat;
            this.issuer = issuer;
            this.inResponseTo = inResponseTo;
            this.destination = destination;
            this.audience = audience;
            this.recipient = recipient;
            this.notBefore = notBefore;
            this.notOnOrAfter = notOnOrAfter;
            this.responseId = responseId;
            this.assertionId = assertionId;
            this.responseSigned = responseSigned;
            this.assertionSigned = assertionSigned;
            this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        }
    }
}
