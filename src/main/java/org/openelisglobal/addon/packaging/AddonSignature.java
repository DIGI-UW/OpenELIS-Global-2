package org.openelisglobal.addon.packaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ed25519 package signatures.
 *
 * <p>
 * The signed bytes are a listing of every file in the package except
 * {@code SIGNATURE.json}, one line per file sorted by name:
 * {@code <sha256 hex>  <name>\n} (the format of {@code sha256sum}). Adding,
 * removing or changing any file therefore breaks the signature.
 * {@code SIGNATURE.json} is
 * {@code {"alg":"ed25519","publisher":…,"keyId":…,"signature":<base64>}};
 * {@code keyId} is the first 16 hex digits of the SHA-256 of the raw public
 * key.
 */
public final class AddonSignature {

    public static final String SIGNATURE_FILE = "SIGNATURE.json";

    /** DER prefix of an X.509 SubjectPublicKeyInfo holding a raw Ed25519 key. */
    private static final byte[] SPKI_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");
    private static final ObjectMapper JSON = new ObjectMapper();

    private AddonSignature() {
    }

    /** What the signature says about who published a package. */
    public enum Status {
        /** Signed with the registered key of a trusted publisher. */
        VERIFIED,
        /** Not signed, or signed by a publisher this server does not know. */
        UNVERIFIED,
        /**
         * A signature is present but does not match the contents (files changed after
         * signing, or a trusted publisher's name with another key).
         */
        MISMATCH
    }

    /**
     * Provenance of a package. {@code verifiedPublisher} is set only for
     * {@link Status#VERIFIED}; a claimed name is never reported as verified.
     */
    public record Provenance(Status status, String verifiedPublisher, String keyId) {
        public static Provenance unverified() {
            return new Provenance(Status.UNVERIFIED, null, null);
        }

        public static Provenance mismatch() {
            return new Provenance(Status.MISMATCH, null, null);
        }
    }

    public static byte[] signedListing(Map<String, byte[]> files) {
        StringBuilder sb = new StringBuilder();
        files.keySet().stream().filter(n -> !SIGNATURE_FILE.equals(n)).sorted()
                .forEach(n -> sb.append(sha256Hex(files.get(n))).append("  ").append(n).append('\n'));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Says who published the package. The signature only establishes provenance: it
     * never decides whether a package may be installed (the administrator's consent
     * and the sandbox do). Never throws.
     */
    public static Provenance check(Map<String, byte[]> files, Map<String, byte[]> trustedPublishers) {
        byte[] raw = files.get(SIGNATURE_FILE);
        if (raw == null) {
            return Provenance.unverified();
        }
        JsonNode sig;
        try {
            sig = JSON.readTree(raw);
        } catch (Exception e) {
            return Provenance.mismatch();
        }
        if (sig == null || !"ed25519".equals(sig.path("alg").asText())) {
            return Provenance.mismatch();
        }
        String publisher = sig.path("publisher").asText("");
        byte[] key = trustedPublishers.get(publisher);
        if (key == null) {
            // a publisher we have no key for: nothing to check the signature against
            return Provenance.unverified();
        }
        if (!keyId(key).equals(sig.path("keyId").asText())) {
            return Provenance.mismatch();
        }
        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey(key));
            verifier.update(signedListing(files));
            if (verifier.verify(Base64.getDecoder().decode(sig.path("signature").asText("")))) {
                return new Provenance(Status.VERIFIED, publisher, keyId(key));
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // falls through: an unreadable signature is a mismatch
        }
        return Provenance.mismatch();
    }

    /**
     * Parses the {@code addons.trustedPublishers} setting:
     * {@code name=key[,name=key…]}, each key the base64 of a raw 32-byte Ed25519
     * public key or of its DER SubjectPublicKeyInfo (what
     * {@code openssl pkey -pubout -outform DER} prints). Blank = no trusted
     * publisher, so every package is refused.
     */
    public static Map<String, byte[]> parseTrustedPublishers(String setting) {
        Map<String, byte[]> out = new LinkedHashMap<>();
        if (setting == null || setting.isBlank()) {
            return out;
        }
        for (String entry : setting.split(",")) {
            if (entry.isBlank()) {
                continue;
            }
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                throw new IllegalArgumentException("addons.trustedPublishers: expected name=key, got '" + entry + "'");
            }
            String name = entry.substring(0, eq).trim();
            out.put(name, rawKey(entry.substring(eq + 1).trim()));
        }
        return out;
    }

    static byte[] rawKey(String base64) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("addons.trustedPublishers: key is not base64");
        }
        if (key.length == 44 && Arrays.equals(Arrays.copyOf(key, 12), SPKI_PREFIX)) {
            key = Arrays.copyOfRange(key, 12, 44);
        }
        if (key.length != 32) {
            throw new IllegalArgumentException("addons.trustedPublishers: not an Ed25519 public key");
        }
        return key;
    }

    public static String keyId(byte[] rawKey) {
        return sha256Hex(rawKey).substring(0, 16);
    }

    static PublicKey publicKey(byte[] rawKey) throws GeneralSecurityException {
        byte[] spki = new byte[44];
        System.arraycopy(SPKI_PREFIX, 0, spki, 0, 12);
        System.arraycopy(rawKey, 0, spki, 12, 32);
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(spki));
    }

    public static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
