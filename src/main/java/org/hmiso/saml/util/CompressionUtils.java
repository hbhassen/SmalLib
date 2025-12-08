package org.hmiso.saml.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Utilitaires de compression compatibles avec le binding HTTP-Redirect.
 */
public final class CompressionUtils {
    private CompressionUtils() {
    }

    public static String deflateToBase64(String xml) {
        try {
            byte[] input = xml.getBytes(StandardCharsets.UTF_8);
            Deflater deflater = new Deflater(Deflater.DEFLATED, true);
            deflater.setInput(input);
            deflater.finish();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
            byte[] buffer = new byte[256];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Échec deflate", e);
        }
    }

    public static String inflateFromBase64(String base64) {
        byte[] decoded = Base64.getDecoder().decode(base64);
        Inflater inflater = new Inflater(true);
        inflater.setInput(decoded);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(decoded.length)) {
            byte[] buffer = new byte[256];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Échec inflate", e);
        }
    }
}
