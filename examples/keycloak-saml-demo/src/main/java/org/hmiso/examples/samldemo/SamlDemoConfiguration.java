package org.hmiso.examples.samldemo;

import org.hmiso.saml.DefaultSamlServiceProviderFactory;
import org.hmiso.saml.api.SamlServiceProvider;
import org.hmiso.saml.api.SamlServiceProviderFactory;
import org.hmiso.saml.binding.RelayStateManager;
import org.hmiso.saml.binding.RelayStateStore;
import org.hmiso.saml.config.BindingType;
import org.hmiso.saml.config.IdentityProviderConfig;
import org.hmiso.saml.config.SamlConfiguration;
import org.hmiso.saml.config.SecurityConfig;
import org.hmiso.saml.config.ServiceProviderConfig;
import org.hmiso.saml.integration.DefaultSamlAuditLogger;
import org.hmiso.saml.integration.DefaultSamlErrorHandler;
import org.hmiso.saml.integration.SamlAuthenticationFilterConfig;
import org.hmiso.saml.integration.SamlAuthenticationFilterHelper;
import org.hmiso.saml.integration.SamlSessionHelper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
public class SamlDemoConfiguration {

    @Bean
    public SamlConfiguration samlConfiguration() {
        ServiceProviderConfig spConfig = new ServiceProviderConfig(
                "saml-sp",
                URI.create("http://localhost:8080/login/saml2/sso/acs"),
                URI.create("http://localhost:8080/logout/saml"),
                "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                BindingType.HTTP_POST,
                true,
                List.of("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress")
        );

        IdentityProviderConfig idpConfig = new IdentityProviderConfig(
                "saml-realm",
                URI.create("https://localhost:8443/realms/saml-realm/protocol/saml"),
                URI.create("https://localhost:8443/realms/saml-realm/protocol/saml"),
                null,
                null,
                URI.create("https://localhost:8443/realms/saml-realm/protocol/saml/descriptor"),
                true,
                true,
                List.of(BindingType.HTTP_POST, BindingType.HTTP_REDIRECT)
        );

        SecurityConfig securityConfig = new SecurityConfig(
                Duration.ofMinutes(2),
                "rsa-sha256",
                "sha256",
                null,
                null,
                "aes256",
                false,
                true
        );

        return new SamlConfiguration(spConfig, idpConfig, securityConfig);
    }

    @Bean
    public RelayStateStore relayStateStore() {
        return new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC());
    }

    @Bean
    public SamlServiceProvider samlServiceProvider(SamlConfiguration configuration) {
        SamlServiceProviderFactory factory = new DefaultSamlServiceProviderFactory();
        return factory.create(configuration);
    }

    @Bean
    public SamlAuthenticationFilterConfig samlAuthenticationFilterConfig(SamlServiceProvider serviceProvider,
                                                                         RelayStateStore relayStateStore,
                                                                         SamlDemoProperties properties) {
        return SamlAuthenticationFilterConfig.builder()
                .protectedPaths(List.of("/api/*"))
                .acsPath("/login/saml2/sso/acs")
                .sloPath("/logout/saml")
                .sessionAttributeKey(properties.sessionAttributeKey())
                .samlServiceProvider(serviceProvider)
                .relayStateStore(relayStateStore)
                .build();
    }

    @Bean
    public SamlAuthenticationFilterHelper samlAuthenticationFilterHelper(SamlAuthenticationFilterConfig config) {
        return new SamlAuthenticationFilterHelper(
                config,
                samlSessionHelper(),
                new DefaultSamlAuditLogger(),
                new DefaultSamlErrorHandler("/saml/error"));
    }

    @Bean
    public SamlSessionHelper samlSessionHelper() {
        return new SamlSessionHelper();
    }

    @Bean
    public SamlDemoProperties samlDemoProperties() {
        return new SamlDemoProperties("saml.principal");
    }

    @Bean
    public FilterRegistrationBean<SamlFilter> samlFilter(SamlAuthenticationFilterConfig config,
                                                         SamlAuthenticationFilterHelper helper) {
        FilterRegistrationBean<SamlFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SamlFilter(config, helper));
        registrationBean.setOrder(1);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
