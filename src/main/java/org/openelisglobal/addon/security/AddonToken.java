package org.openelisglobal.addon.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Short-lived token given to one add-on's UI frame instead of the user's
 * session: {@code oeat.<payload>.<mac>}. The payload names the add-on, the
 * user, the scopes and the lab numbers on the user's screen, and the expiry;
 * the MAC is HMAC-SHA256 with a key that never leaves the server. The token is
 * only accepted by the add-on API ({@code /rest/addon-api/v1}).
 */
public final class AddonToken {

    public static final String PREFIX = "oeat.";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder D64 = Base64.getUrlDecoder();

    /**
     * {@code accessions} null means a service credential (no screen context);
     * {@code userId} null for the same.
     */
    public record Claims(String addonId, String userId, List<String> scopes, List<String> accessions, long expiresAt) {
        public Claims {
            scopes = scopes == null ? List.of() : List.copyOf(scopes);
            accessions = accessions == null ? null : List.copyOf(accessions);
        }

        public boolean isService() {
            return accessions == null;
        }
    }

    private AddonToken() {
    }

    public static String sign(byte[] key, Claims claims) {
        ObjectNode o = JSON.createObjectNode();
        o.put("a", claims.addonId());
        o.put("u", claims.userId());
        o.set("s", JSON.valueToTree(claims.scopes()));
        o.set("c", JSON.valueToTree(claims.accessions() == null ? List.of() : claims.accessions()));
        o.put("e", claims.expiresAt());
        String payload = B64.encodeToString(o.toString().getBytes(StandardCharsets.UTF_8));
        return PREFIX + payload + "." + B64.encodeToString(hmac(key, payload));
    }

    /**
     * Checks the MAC and the expiry.
     *
     * @throws IllegalArgumentException for any problem, without saying which
     */
    public static Claims verify(byte[] key, String token, long nowEpochSeconds) {
        if (token == null || !token.startsWith(PREFIX)) {
            throw new IllegalArgumentException("token");
        }
        String body = token.substring(PREFIX.length());
        int dot = body.indexOf('.');
        if (dot <= 0 || dot != body.lastIndexOf('.')) {
            throw new IllegalArgumentException("token");
        }
        String payload = body.substring(0, dot);
        byte[] mac;
        try {
            mac = D64.decode(body.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("token");
        }
        if (!MessageDigest.isEqual(mac, hmac(key, payload))) {
            throw new IllegalArgumentException("token");
        }
        JsonNode o;
        try {
            o = JSON.readTree(D64.decode(payload));
        } catch (Exception e) {
            throw new IllegalArgumentException("token");
        }
        if (o.path("e").asLong(0) <= nowEpochSeconds) {
            throw new IllegalArgumentException("token");
        }
        List<String> scopes = new ArrayList<>();
        o.path("s").forEach(n -> scopes.add(n.asText()));
        List<String> accessions = new ArrayList<>();
        o.path("c").forEach(n -> accessions.add(n.asText()));
        return new Claims(o.path("a").asText(), o.path("u").asText(null), scopes, accessions, o.path("e").asLong());
    }

    static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
