package com.hmiso.saml.integration;

import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.IdentityProviderConfig;
import com.hmiso.saml.config.SamlConfiguration;
import com.hmiso.saml.config.SecurityConfig;
import com.hmiso.saml.config.ServiceProviderConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads application and SAML configuration from a YAML file.
 */
public final class SamlAppYamlConfigLoader {
    public static final String CONFIG_PROPERTY = "saml.config.path";
    public static final String CONFIG_ENV = "SAML_CONFIG_PATH";
    public static final String DEFAULT_RESOURCE = "/saml-config.yml";

    public SamlAppConfiguration load(String contextPath) {
        Map<String, Object> root = loadYaml();
        Map<String, Object> sp = requireMap(root, "service-provider");
        Map<String, Object> idp = requireMap(root, "identity-provider");
        Map<String, Object> security = requireMap(root, "security");
        Map<String, Object> app = requireMap(root, "app");

        String spEntityId = requireString(sp, "entity-id");
        String baseUrl = requireString(sp, "base-url");
        String acsPath = requireString(sp, "acs-path");
        String sloPath = requireString(sp, "slo-path");
        String nameIdFormat = requireString(sp, "name-id-format");
        BindingType authnRequestBinding = parseBinding(requireString(sp, "authn-request-binding"),
                "service-provider.authn-request-binding");
        boolean spWantAssertionsSigned = requireBoolean(sp, "want-assertions-signed");
        List<String> supportedNameIdFormats = stringList(sp, "supported-name-id-formats");
        if (supportedNameIdFormats.isEmpty()) {
            supportedNameIdFormats = List.of(nameIdFormat);
        }

        String normalizedContextPath = normalizeContextPath(contextPath);
        URI acsUrl = URI.create(joinUrl(baseUrl, normalizedContextPath, acsPath));
        URI sloUrl = URI.create(joinUrl(baseUrl, normalizedContextPath, sloPath));

        ServiceProviderConfig spConfig = new ServiceProviderConfig(
                spEntityId,
                acsUrl,
                sloUrl,
                nameIdFormat,
                authnRequestBinding,
                spWantAssertionsSigned,
                supportedNameIdFormats
        );

        String idpEntityId = requireString(idp, "entity-id");
        URI ssoUrl = URI.create(requireString(idp, "single-sign-on-service-url"));
        String idpSloRaw = optionalString(idp, "single-logout-service-url");
        URI idpSloUrl = idpSloRaw == null ? null : URI.create(idpSloRaw);
        String metadataRaw = optionalString(idp, "metadata-url");
        URI metadataUrl = metadataRaw == null ? null : URI.create(metadataRaw);
        boolean idpWantAssertionsSigned = requireBoolean(idp, "want-assertions-signed");
        boolean idpWantMessagesSigned = requireBoolean(idp, "want-messages-signed");
        List<BindingType> supportedBindings = bindingList(idp, "supported-bindings");

        IdentityProviderConfig idpConfig = new IdentityProviderConfig(
                idpEntityId,
                ssoUrl,
                idpSloUrl,
                null,
                null,
                metadataUrl,
                idpWantAssertionsSigned,
                idpWantMessagesSigned,
                supportedBindings
        );

        Duration clockSkew = parseDuration(requireString(security, "clock-skew"), "security.clock-skew");
        String signatureAlgorithm = requireString(security, "signature-algorithm");
        String digestAlgorithm = requireString(security, "digest-algorithm");
        String encryptionAlgorithm = optionalString(security, "encryption-algorithm");
        boolean forceHttpsRedirect = optionalBoolean(security, "force-https-redirect", false);
        boolean enableDetailedLogging = optionalBoolean(security, "enable-detailed-logging", false);

        SecurityConfig securityConfig = new SecurityConfig(
                clockSkew,
                signatureAlgorithm,
                digestAlgorithm,
                null,
                null,
                encryptionAlgorithm,
                forceHttpsRedirect,
                enableDetailedLogging
        );

        SamlConfiguration samlConfiguration = new SamlConfiguration(spConfig, idpConfig, securityConfig);

        String sessionAttributeKey = optionalString(app, "session-attribute-key");
        if (sessionAttributeKey == null) {
            sessionAttributeKey = SamlAppConfiguration.DEFAULT_SESSION_ATTRIBUTE_KEY;
        }
        String serverSessionAttributeKey = optionalString(app, "server-session-attribute-key");
        if (serverSessionAttributeKey == null) {
            serverSessionAttributeKey = SamlAppConfiguration.DEFAULT_SERVER_SESSION_ATTRIBUTE_KEY;
        }
        Duration sessionMaxTtl = parseSessionMaxTtl(app);
        String errorPath = optionalString(app, "error-path");
        if (errorPath == null) {
            errorPath = SamlAppConfiguration.DEFAULT_ERROR_PATH;
        }
        errorPath = prefixContextPath(normalizedContextPath, errorPath);
        List<String> protectedPaths = stringList(app, "protected-paths");
        if (protectedPaths.isEmpty()) {
            throw new IllegalStateException("Missing app.protected-paths");
        }
        List<String> normalizedProtectedPaths = new ArrayList<>(protectedPaths.size());
        for (String path : protectedPaths) {
            normalizedProtectedPaths.add(prefixContextPath(normalizedContextPath, path));
        }
        Duration relayStateTtl = parseRelayStateTtl(app);
        Duration jwtTtl = parseJwtTtl(security);
        String jwtSecret = optionalString(security, "jwt-secret");
        CorsConfiguration corsConfiguration = buildCorsConfiguration(app);
        boolean blockBrowserNavigation = optionalBoolean(app, "block-browser-navigation",
                SamlAppConfiguration.DEFAULT_BLOCK_BROWSER_NAVIGATION);
        boolean jaspicEnabled = optionalBoolean(app, "jaspic-enabled",
                SamlAppConfiguration.DEFAULT_JASPIC_ENABLED);

        return new SamlAppConfiguration(
                samlConfiguration,
                sessionAttributeKey,
                serverSessionAttributeKey,
                sessionMaxTtl,
                normalizedProtectedPaths,
                prefixContextPath(normalizedContextPath, acsPath),
                prefixContextPath(normalizedContextPath, sloPath),
                relayStateTtl,
                errorPath,
                jwtTtl,
                jwtSecret,
                corsConfiguration,
                blockBrowserNavigation,
                jaspicEnabled
        );
    }

    private CorsConfiguration buildCorsConfiguration(Map<String, Object> app) {
        List<String> allowedOrigins = stringList(app, "cors-allowed-origins");
        boolean enabled = optionalBoolean(app, "cors-enabled", !allowedOrigins.isEmpty());
        if (allowedOrigins.isEmpty()) {
            enabled = false;
        }

        List<String> allowedMethods = stringList(app, "cors-allowed-methods");
        if (allowedMethods.isEmpty()) {
            allowedMethods = CorsConfiguration.DEFAULT_ALLOWED_METHODS;
        }
        List<String> allowedHeaders = stringList(app, "cors-allowed-headers");
        if (allowedHeaders.isEmpty()) {
            allowedHeaders = CorsConfiguration.DEFAULT_ALLOWED_HEADERS;
        }
        List<String> exposeHeaders = stringList(app, "cors-expose-headers");
        if (exposeHeaders.isEmpty()) {
            exposeHeaders = CorsConfiguration.DEFAULT_EXPOSE_HEADERS;
        }
        boolean allowCredentials = optionalBoolean(app, "cors-allow-credentials", true);
        return new CorsConfiguration(enabled, allowCredentials, allowedOrigins, allowedMethods, allowedHeaders,
                exposeHeaders);
    }

    private Duration parseRelayStateTtl(Map<String, Object> app) {
        Object value = app.get("relay-state-ttl-minutes");
        if (value == null) {
            return SamlAppConfiguration.DEFAULT_RELAY_STATE_TTL;
        }
        if (value instanceof Number) {
            return Duration.ofMinutes(((Number) value).longValue());
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return SamlAppConfiguration.DEFAULT_RELAY_STATE_TTL;
        }
        try {
            return Duration.ofMinutes(Long.parseLong(text));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid relay-state-ttl-minutes", ex);
        }
    }

    private Duration parseSessionMaxTtl(Map<String, Object> app) {
        Object value = app.get("session-max-minutes");
        if (value == null) {
            return SamlAppConfiguration.DEFAULT_SESSION_MAX_TTL;
        }
        if (value instanceof Number) {
            return Duration.ofMinutes(((Number) value).longValue());
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return SamlAppConfiguration.DEFAULT_SESSION_MAX_TTL;
        }
        try {
            return Duration.ofMinutes(Long.parseLong(text));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid session-max-minutes", ex);
        }
    }

    private Duration parseJwtTtl(Map<String, Object> security) {
        Object value = security.get("jwt-ttl-seconds");
        if (value == null) {
            return SamlAppConfiguration.DEFAULT_JWT_TTL;
        }
        if (value instanceof Number) {
            return Duration.ofSeconds(((Number) value).longValue());
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return SamlAppConfiguration.DEFAULT_JWT_TTL;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(text));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid jwt-ttl-seconds", ex);
        }
    }

    private Map<String, Object> loadYaml() {
        try (InputStream input = openConfigStream()) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(input);
            if (!(data instanceof Map<?, ?>)) {
                throw new IllegalStateException("Invalid YAML: root must be a map");
            }
            return castMap(data);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read SAML config", e);
        }
    }

    private InputStream openConfigStream() throws IOException {
        String configuredPath = System.getProperty(CONFIG_PROPERTY);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv(CONFIG_ENV);
        }
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Files.newInputStream(Path.of(configuredPath));
        }
        InputStream input = openFromContextClassLoader();
        if (input != null) {
            return input;
        }
        input = SamlAppYamlConfigLoader.class.getResourceAsStream(DEFAULT_RESOURCE);
        if (input == null) {
            throw new IllegalStateException("Missing " + DEFAULT_RESOURCE + " on the classpath");
        }
        return input;
    }

    private InputStream openFromContextClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            return null;
        }
        String resourceName = DEFAULT_RESOURCE.startsWith("/") ? DEFAULT_RESOURCE.substring(1) : DEFAULT_RESOURCE;
        InputStream input = loader.getResourceAsStream(resourceName);
        if (input != null) {
            return input;
        }
        if (!resourceName.equals(DEFAULT_RESOURCE)) {
            return loader.getResourceAsStream(DEFAULT_RESOURCE);
        }
        return null;
    }

    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        if (!contextPath.startsWith("/")) {
            return "/" + contextPath;
        }
        return contextPath;
    }

    private static String joinUrl(String baseUrl, String contextPath, String path) {
        String normalizedBase = stripTrailingSlash(baseUrl);
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + contextPath + normalizedPath;
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String prefixContextPath(String contextPath, String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (contextPath == null || contextPath.isBlank()) {
            return normalized;
        }
        if (normalized.startsWith(contextPath + "/") || normalized.equals(contextPath)) {
            return normalized;
        }
        return contextPath + normalized;
    }

    private static Map<String, Object> requireMap(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Map<?, ?>) {
            return castMap(value);
        }
        throw new IllegalStateException("Missing map key: " + key);
    }

    private static String requireString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing key: " + key);
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                throw new IllegalStateException("Blank value for key: " + key);
            }
            return text;
        }
        throw new IllegalStateException("Expected string for key: " + key);
    }

    private static String optionalString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? null : text;
        }
        throw new IllegalStateException("Expected string for key: " + key);
    }

    private static boolean requireBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing key: " + key);
        }
        return parseBoolean(value, key);
    }

    private static boolean optionalBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return parseBoolean(value, key);
    }

    private static boolean parseBoolean(Object value, String key) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = value.toString().trim();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalStateException("Expected boolean for key: " + key);
    }

    private static List<String> stringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?>) {
            List<?> rawList = (List<?>) value;
            List<String> values = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (!(item instanceof String)) {
                    throw new IllegalStateException("Expected string items for key: " + key);
                }
                String text = ((String) item).trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            }
            return values;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? List.of() : List.of(text);
        }
        throw new IllegalStateException("Expected list for key: " + key);
    }

    private static List<BindingType> bindingList(Map<String, Object> map, String key) {
        List<String> rawValues = stringList(map, key);
        if (rawValues.isEmpty()) {
            throw new IllegalStateException("Missing bindings for key: " + key);
        }
        List<BindingType> bindings = new ArrayList<>(rawValues.size());
        for (String raw : rawValues) {
            bindings.add(parseBinding(raw, key));
        }
        return bindings;
    }

    private static BindingType parseBinding(String raw, String key) {
        try {
            return BindingType.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid binding for key: " + key, ex);
        }
    }

    private static Duration parseDuration(String raw, String key) {
        try {
            return Duration.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Invalid duration for key: " + key, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
