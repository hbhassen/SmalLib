package org.hmiso.saml.security;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Initialisation idempotente d'OpenSAML.
 */
public final class OpenSamlInitializer {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private OpenSamlInitializer() {
    }

    public static void initialize() {
        if (INITIALIZED.compareAndSet(false, true)) {
            // point d'extension : InitializationService.initialize();
        }
    }

    public static boolean isInitialized() {
        return INITIALIZED.get();
    }
}
