package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Emission et validation d'un JWT court lie a la session serveur.
 */
public class SamlJwtService {
    public static final String DEFAULT_HEADER_NAME = "X-Auth-Token";

    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public SamlJwtService(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new SamlException("JWT secret manquant");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issueToken(SamlServerSession session, Duration ttl, List<String> roles) {
        if (session == null) {
            throw new SamlException("Session serveur manquante");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new SamlException("TTL JWT invalide");
        }
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String header = BASE64_URL.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

        String payloadJson = buildPayload(session, now.getEpochSecond(), exp.getEpochSecond(), roles);
        String payload = BASE64_URL.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public JwtClaims validate(String token) {
        if (token == null || token.isBlank()) {
            throw new SamlException("JWT vide");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new SamlException("JWT invalide");
        }
        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];
        String expected = sign(header + "." + payload);
        if (!constantTimeEquals(expected, signature)) {
            throw new SamlException("Signature JWT invalide");
        }
        String payloadJson = new String(BASE64_URL_DECODER.decode(payload), StandardCharsets.UTF_8);
        String subject = extractString(payloadJson, "sub");
        String sessionId = extractString(payloadJson, "sid");
        long iat = extractLong(payloadJson, "iat");
        long exp = extractLong(payloadJson, "exp");
        if (subject == null || subject.isBlank()) {
            throw new SamlException("JWT subject manquant");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new SamlException("JWT session manquante");
        }
        Instant now = Instant.now();
        if (exp <= now.getEpochSecond()) {
            throw new SamlException("JWT expire");
        }
        return new JwtClaims(subject, sessionId, iat, exp, extractStringList(payloadJson, "roles"));
    }

    private String buildPayload(SamlServerSession session, long iat, long exp, List<String> roles) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"sub\":\"").append(escapeJson(session.getPrincipal().getNameId())).append("\",");
        json.append("\"sid\":\"").append(escapeJson(session.getSessionId())).append("\",");
        json.append("\"iat\":").append(iat).append(",");
        json.append("\"exp\":").append(exp).append(",");
        json.append("\"jti\":\"").append(UUID.randomUUID()).append("\"");
        if (roles != null && !roles.isEmpty()) {
            json.append(",\"roles\":[");
            for (int i = 0; i < roles.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(escapeJson(roles.get(i))).append("\"");
            }
            json.append("]");
        }
        json.append("}");
        return json.toString();
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL.encodeToString(signature);
        } catch (Exception ex) {
            throw new SamlException("Impossible de signer le JWT", ex);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length; i++) {
            result |= left[i] ^ right[i];
        }
        return result == 0;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private long extractLong(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new SamlException("Champ JWT manquant: " + key);
        }
        return Long.parseLong(matcher.group(1));
    }

    private List<String> extractStringList(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[(.*?)\\]").matcher(json);
        if (!matcher.find()) {
            return List.of();
        }
        String body = matcher.group(1).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        String[] parts = body.split(",");
        List<String> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
                values.add(trimmed.substring(1, trimmed.length() - 1));
            } else if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    public static final class JwtClaims {
        private final String subject;
        private final String sessionId;
        private final long issuedAt;
        private final long expiresAt;
        private final List<String> roles;

        public JwtClaims(String subject, String sessionId, long issuedAt, long expiresAt, List<String> roles) {
            this.subject = subject;
            this.sessionId = sessionId;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.roles = roles == null ? List.of() : List.copyOf(roles);
        }

        public String getSubject() {
            return subject;
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
