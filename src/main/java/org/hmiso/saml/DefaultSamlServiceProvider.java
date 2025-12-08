package org.hmiso.saml;

import org.hmiso.saml.api.SamlException;
import org.hmiso.saml.api.SamlPrincipal;
import org.hmiso.saml.api.SamlServiceProvider;
import org.hmiso.saml.binding.BindingMessage;
import org.hmiso.saml.binding.PostBindingEncoder;
import org.hmiso.saml.binding.RedirectBindingEncoder;
import org.hmiso.saml.binding.RelayStateManager;
import org.hmiso.saml.config.BindingType;
import org.hmiso.saml.config.SamlConfiguration;
import org.hmiso.saml.saml.AssertionExtractor;
import org.hmiso.saml.saml.AuthnRequestBuilder;
import org.hmiso.saml.saml.LogoutRequestBuilder;
import org.hmiso.saml.saml.LogoutResponseValidator;
import org.hmiso.saml.saml.SamlResponseValidator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Implémentation par défaut orchestrant les builders/validateurs internes.
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
        return assertionExtractor.toPrincipal(samlResponse, null, relayState, Map.of());
    }

    @Override
    public BindingMessage initiateLogout(String sessionIndex, String relayState) {
        // E10 : construire une LogoutRequest alignée sur le binding SLO configuré.
        String requestId = "LR-" + UUID.randomUUID();
        String logoutRequest = logoutRequestBuilder.build(requestId, "unknown", sessionIndex, Instant.now());
        if (configuration.getIdentityProvider().getSingleLogoutServiceUrl() == null) {
            throw new SamlException("Endpoint SLO non configuré");
        }
        if (configuration.getServiceProvider().getAuthnRequestBinding() == BindingType.HTTP_REDIRECT) {
            return redirectBindingEncoder.encode(logoutRequest, configuration.getIdentityProvider().getSingleLogoutServiceUrl(), relayState);
        }
        return postBindingEncoder.encode(logoutRequest, configuration.getIdentityProvider().getSingleLogoutServiceUrl(), relayState);
    }

    @Override
    public void processLogoutResponse(String logoutResponse, String inResponseTo) {
        // E10 : valider la réponse de logout avant d'invalider la session applicative.
        if (logoutResponse == null || logoutResponse.isBlank()) {
            throw new SamlException("LogoutResponse vide");
        }
        logoutResponseValidator.validate(inResponseTo, Instant.now(), Instant.now().plusSeconds(60));
    }
}
