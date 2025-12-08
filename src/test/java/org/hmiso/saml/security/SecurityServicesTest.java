package org.hmiso.saml.security;

import org.hmiso.saml.config.KeystoreConfig;
import org.hmiso.saml.config.SecurityConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SecurityServicesTest {

    @Test
    void initializerShouldBeIdempotent_TC_SEC_01() {
        OpenSamlInitializer.initialize();
        OpenSamlInitializer.initialize();

        assertTrue(OpenSamlInitializer.isInitialized());
    }

    @Test
    void signatureServiceShouldSignAndValidate_TC_SEC_02() {
        SecurityConfig security = new SecurityConfig(Duration.ofMinutes(1), "RSA-SHA256", "SHA-256",
                null, null, null, true, true);
        SignatureService signatureService = new SignatureService(security);

        String signature = signatureService.sign("payload");

        assertTrue(signatureService.validate("payload", signature));
        assertFalse(signatureService.validate("other", signature));
    }

    @Test
    void credentialResolverValidatesPaths_TC_SEC_04() throws Exception {
        SecurityConfig security = new SecurityConfig(Duration.ofMinutes(1), "RSA-SHA256", "SHA-256",
                null, null, null, true, true);
        CredentialResolver resolver = new CredentialResolver(security);

        assertDoesNotThrow(resolver::validateKeystores);

        Path temp = Files.createTempFile("keystore", ".jks");
        KeystoreConfig keystore = new KeystoreConfig(temp, "secret", "alias", "secret", "PKCS12");
        SecurityConfig withStore = new SecurityConfig(Duration.ofMinutes(1), "RSA-SHA256", "SHA-256",
                keystore, null, null, true, true);
        CredentialResolver withResolver = new CredentialResolver(withStore);

        withResolver.validateKeystores();
    }

    @Test
    void encryptionServiceEchoesAssertions_TC_SEC_03() {
        SecurityConfig security = new SecurityConfig(Duration.ofMinutes(1), "RSA-SHA256", "SHA-256",
                null, null, null, true, true);
        EncryptionService encryptionService = new EncryptionService(security);

        assertEquals("assertion", encryptionService.decrypt(encryptionService.encrypt("assertion")));
    }
}
