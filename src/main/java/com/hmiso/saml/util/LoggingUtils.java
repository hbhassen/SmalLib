package com.hmiso.saml.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpers de logging centralisant le masquage des secrets.
 */
public final class LoggingUtils {
    private LoggingUtils() {
    }

    public static Logger getLogger(Class<?> type) {
        return LoggerFactory.getLogger(type);
    }

    public static String maskSecret(String value) {
        if (value == null || value.length() < 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
