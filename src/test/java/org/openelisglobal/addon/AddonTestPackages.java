package org.openelisglobal.addon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.openelisglobal.addon.packaging.AddonSignature;

/**
 * Builds add-on packages for tests: a publisher with a fresh Ed25519 key, a
 * manifest, UI files, and a signed (or unsigned, or tampered) zip.
 */
public final class AddonTestPackages {

    public static final String IMAGE = "registry.example.org/hello@sha256:" + "a".repeat(64);

    private AddonTestPackages() {
    }

    /** A publisher and its key pair. */
    public static final class Publisher {
        public final String name;
        public final KeyPair keys;

        public Publisher(String name) {
            this.name = name;
            try {
                this.keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * The raw 32-byte public key, base64, as {@code addons.trustedPublishers} takes
         * it.
         */
        public String publicKeyBase64() {
            byte[] spki = keys.getPublic().getEncoded();
            return Base64.getEncoder().encodeToString(Arrays.copyOfRange(spki, spki.length - 32, spki.length));
        }

        public String trustSetting() {
            return name + "=" + publicKeyBase64();
        }

        public Map<String, byte[]> trusted() {
            return AddonSignature.parseTrustedPublishers(trustSetting());
        }

        /** Adds SIGNATURE.json for the given files. */
        public Map<String, byte[]> sign(Map<String, byte[]> files) {
            try {
                Signature signer = Signature.getInstance("Ed25519");
                signer.initSign(keys.getPrivate());
                signer.update(AddonSignature.signedListing(files));
                byte[] raw = Base64.getDecoder().decode(publicKeyBase64());
                String sig = "{\"alg\":\"ed25519\",\"publisher\":\"" + name + "\",\"keyId\":\""
                        + AddonSignature.keyId(raw) + "\",\"signature\":\""
                        + Base64.getEncoder().encodeToString(signer.sign()) + "\"}";
                Map<String, byte[]> out = new TreeMap<>(files);
                out.put(AddonSignature.SIGNATURE_FILE, sig.getBytes(StandardCharsets.UTF_8));
                return out;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static String manifest(String id, String publisher, String scopesJson, String eventsJson) {
        return "{\"schemaVersion\":1,\"id\":\"" + id + "\",\"name\":\"Hello " + id
                + "\",\"version\":\"1.0.0\",\"publisher\":\"" + publisher
                + "\",\"description\":\"Test add-on\",\"image\":{\"ref\":\"" + IMAGE + "\"},"
                + "\"runtime\":{\"port\":8080,\"eventsPath\":\"/events\"},\"scopes\":" + scopesJson + ",\"events\":"
                + eventsJson + ",\"extensions\":[{\"name\":\"" + id
                + "-panel\",\"slot\":\"result-lookup-side-panel-slot\",\"page\":\"panel.html\",\"order\":10,"
                + "\"meta\":{\"title\":\"Hello\",\"height\":200}}],"
                + "\"config\":[{\"key\":\"greeting\",\"type\":\"string\",\"label\":\"Greeting\",\"default\":\"Hi\"},"
                + "{\"key\":\"limit\",\"type\":\"number\",\"label\":\"Limit\",\"min\":1,\"max\":50,\"default\":5}]}";
    }

    /**
     * The manifest with a {@code network.egress} list and a secret config field
     * {@code apiKey}.
     */
    public static String withEgressAndSecret(String manifest, String egressJson) {
        return manifest.replace(",\"extensions\":", ",\"network\":{\"egress\":" + egressJson + "},\"extensions\":")
                .replace("\"config\":[", "\"config\":[{\"key\":\"apiKey\",\"type\":\"secret\",\"label\":\"API key\"},");
    }

    /** addon.json + ui/panel.html + ui/panel.js. */
    public static Map<String, byte[]> files(String manifest) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("addon.json", manifest.getBytes(StandardCharsets.UTF_8));
        files.put("ui/panel.html",
                "<!doctype html><script src=\"panel.js\"></script>".getBytes(StandardCharsets.UTF_8));
        files.put("ui/panel.js", "parent.postMessage({type:'ready'},'*');".getBytes(StandardCharsets.UTF_8));
        return files;
    }

    public static byte[] zip(Map<String, byte[]> files) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A signed package for an add-on reading orders and results and receiving
     * result.validated.
     */
    public static byte[] signedPackage(Publisher publisher, String id) {
        return zip(publisher.sign(files(manifest(id, publisher.name, "[\"orders.read\",\"results.read\"]",
                "[\"result.validated\",\"order.created\"]"))));
    }
}
