package com.hmiso.saml.saml;

import com.hmiso.saml.config.SamlConfiguration;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Construit un LogoutRequest basique pour initier le SLO.
 */
public class LogoutRequestBuilder {
    private final SamlConfiguration configuration;

    public LogoutRequestBuilder(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public String build(String requestId, String nameId, String sessionIndex, Instant issueInstant) {
        return build(requestId, nameId, sessionIndex, issueInstant, null);
    }

    public String build(String requestId,
                        String nameId,
                        String sessionIndex,
                        Instant issueInstant,
                        String nameIdFormatOverride) {
        Objects.requireNonNull(nameId, "nameId");
        Objects.requireNonNull(issueInstant, "issueInstant");
        String nameIdFormat = nameIdFormatOverride != null
                ? nameIdFormatOverride
                : configuration.getServiceProvider().getNameIdFormat();
        StringBuilder builder = new StringBuilder();
        builder.append("<samlp:LogoutRequest")
                .append(" xmlns:samlp='urn:oasis:names:tc:SAML:2.0:protocol'")
                .append(" xmlns:saml='urn:oasis:names:tc:SAML:2.0:assertion'")
                .append(" ID='").append(escapeXml(requestId)).append("'")
                .append(" Version='2.0'")
                .append(" IssueInstant='").append(escapeXml(formatInstant(issueInstant))).append("'")
                .append(" Destination='").append(escapeXml(configuration.getIdentityProvider().getSingleLogoutServiceUrl().toString()))
                .append("'>");
        builder.append("<saml:Issuer>")
                .append(escapeXml(configuration.getServiceProvider().getEntityId()))
                .append("</saml:Issuer>");
        builder.append("<saml:NameID");
        if (nameIdFormat != null && !nameIdFormat.isBlank()) {
            builder.append(" Format='").append(escapeXml(nameIdFormat)).append("'");
        }
        builder.append(">").append(escapeXml(nameId)).append("</saml:NameID>");
        if (sessionIndex != null && !sessionIndex.isBlank()) {
            builder.append("<samlp:SessionIndex>").append(escapeXml(sessionIndex)).append("</samlp:SessionIndex>");
        }
        builder.append("</samlp:LogoutRequest>");
        return builder.toString();
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value;
        escaped = escaped.replace("&", "&amp;");
        escaped = escaped.replace("<", "&lt;");
        escaped = escaped.replace(">", "&gt;");
        escaped = escaped.replace("\"", "&quot;");
        escaped = escaped.replace("'", "&apos;");
        return escaped;
    }

    private static String formatInstant(Instant instant) {
        Instant truncated = instant.truncatedTo(ChronoUnit.MILLIS);
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(truncated);
    }
}
