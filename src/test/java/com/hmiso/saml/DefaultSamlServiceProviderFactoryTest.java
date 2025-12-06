package com.hmiso.saml;

import com.hmiso.saml.api.SamlServiceProvider;
import com.hmiso.saml.config.BindingType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultSamlServiceProviderFactoryTest {

    @Test
    void shouldCreateProviderFromConfig_TC_API_01() {
        DefaultSamlServiceProviderFactory factory = new DefaultSamlServiceProviderFactory();
        SamlServiceProvider provider = factory.create(TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT));
        assertNotNull(provider);
    }

    @Test
    void shouldRejectNullLoader_TC_API_02() {
        DefaultSamlServiceProviderFactory factory = new DefaultSamlServiceProviderFactory();
        assertThrows(NullPointerException.class, () -> factory.create((com.hmiso.saml.config.ConfigLoader) null));
    }
}
