package org.hmiso.saml.util;

import java.time.Duration;
import java.time.Instant;

/**
 * Aides de comparaison d'horodatage avec dérive configurable.
 */
public final class TimeUtils {
    private TimeUtils() {
    }

    public static boolean isWithinClockSkew(Instant now, Instant notBefore, Instant notOnOrAfter, Duration skew) {
        // E21 / TC-UTIL-02 : appliquer la tolérance clockSkew sur les conditions temporelles SAML.
        Instant lowerBound = notBefore.minus(skew);
        Instant upperBound = notOnOrAfter.plus(skew);
        return !now.isBefore(lowerBound) && !now.isAfter(upperBound);
    }
}
