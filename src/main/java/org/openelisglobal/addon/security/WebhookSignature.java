package org.openelisglobal.addon.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Event signatures in the Standard Webhooks format
 * (https://www.standardwebhooks.com/): headers {@code webhook-id},
 * {@code webhook-timestamp} and
 * {@code webhook-signature: v1,<base64 HMAC-SHA256(key, id + "." + timestamp + "." + body)>},
 * where the secret is {@code whsec_<base64 key>}. Existing Standard Webhooks
 * libraries can verify these deliveries.
 */
public final class WebhookSignature {

    public static final String SECRET_PREFIX = "whsec_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebhookSignature() {
    }

    public static String newSecret() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        return SECRET_PREFIX + Base64.getEncoder().encodeToString(key);
    }

    public static String sign(String secret, String id, long timestamp, String body) {
        if (secret == null || !secret.startsWith(SECRET_PREFIX)) {
            throw new IllegalArgumentException("secret");
        }
        byte[] key = Base64.getDecoder().decode(secret.substring(SECRET_PREFIX.length()));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] sig = mac.doFinal((id + "." + timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return "v1," + Base64.getEncoder().encodeToString(sig);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
