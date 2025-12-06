package com.hmiso.saml;

import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.SamlConfiguration;
import com.hmiso.saml.util.CompressionUtils;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultSamlServiceProviderIntegrationTest {

    @Test
    void shouldInitiateRedirectFlowAndValidateResponse_TC_E2E_AUTHN_01() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        DefaultSamlServiceProvider provider = new DefaultSamlServiceProvider(configuration);

        BindingMessage message = provider.initiateAuthentication(null);
        String xml = CompressionUtils.inflateFromBase64(message.getPayload());
        assertEquals(BindingType.HTTP_REDIRECT, message.getBindingType());
        assertNotNull(xml);

        SamlPrincipal principal = provider.processSamlResponse("user@example.com", message.getRelayState());
        assertEquals("user@example.com", principal.getNameId());
    }

    @Test
    void shouldInitiatePostLogout_TC_E2E_LOGOUT_01() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_POST);
        DefaultSamlServiceProvider provider = new DefaultSamlServiceProvider(configuration);

        BindingMessage message = provider.initiateLogout("session-1", "relay");
        String xml = new String(Base64.getDecoder().decode(message.getPayload()));
        assertEquals(BindingType.HTTP_POST, message.getBindingType());
        assertNotNull(xml);

        provider.processLogoutResponse("<LogoutResponse/>", "LR-123");
    }
}
