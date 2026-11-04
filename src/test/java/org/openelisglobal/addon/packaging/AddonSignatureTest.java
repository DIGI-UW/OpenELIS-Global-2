package org.openelisglobal.addon.packaging;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import org.openelisglobal.addon.AddonTestPackages;
import org.openelisglobal.addon.AddonTestPackages.Publisher;

public class AddonSignatureTest {

    private final Publisher example = new Publisher("example");

    private Map<String, byte[]> signed() {
        return example.sign(AddonTestPackages.files(AddonTestPackages.manifest("hello", "example", "[]", "[]")));
    }

    private static AddonSignature.Status status(Map<String, byte[]> files, Map<String, byte[]> trusted) {
        AddonSignature.Provenance p = AddonSignature.check(files, trusted);
        if (p.status() != AddonSignature.Status.VERIFIED) {
            assertNull("only a verified package names a verified publisher", p.verifiedPublisher());
        }
        return p.status();
    }

    @Test
    public void aPackageSignedByATrustedPublisherIsVerified() {
        AddonSignature.Provenance p = AddonSignature.check(signed(), example.trusted());
        assertEquals(AddonSignature.Status.VERIFIED, p.status());
        assertEquals("example", p.verifiedPublisher());
        assertEquals(AddonSignature.keyId(Base64.getDecoder().decode(example.publicKeyBase64())), p.keyId());
    }

    @Test
    public void anUnsignedPackageIsUnverified() {
        Map<String, byte[]> files = new TreeMap<>(signed());
        files.remove(AddonSignature.SIGNATURE_FILE);
        assertEquals(AddonSignature.Status.UNVERIFIED, status(files, example.trusted()));
    }

    @Test
    public void aPublisherThisServerDoesNotKnowIsUnverified() {
        assertEquals(AddonSignature.Status.UNVERIFIED, status(signed(), Map.of()));
        assertEquals(AddonSignature.Status.UNVERIFIED, status(signed(), new Publisher("other").trusted()));
    }

    @Test
    public void aTrustedNameWithAnotherKeyIsAMismatch() {
        Publisher impostor = new Publisher("example");
        assertEquals(AddonSignature.Status.MISMATCH, status(signed(), impostor.trusted()));
    }

    @Test
    public void changingOneByteOfAnyFileIsAMismatch() {
        Map<String, byte[]> files = new TreeMap<>(signed());
        byte[] js = files.get("ui/panel.js").clone();
        js[0] ^= 1;
        files.put("ui/panel.js", js);
        assertEquals(AddonSignature.Status.MISMATCH, status(files, example.trusted()));
    }

    @Test
    public void addingOrRemovingAFileIsAMismatch() {
        Map<String, byte[]> added = new TreeMap<>(signed());
        added.put("ui/extra.js", "x".getBytes(StandardCharsets.UTF_8));
        assertEquals(AddonSignature.Status.MISMATCH, status(added, example.trusted()));

        Map<String, byte[]> removed = new TreeMap<>(signed());
        removed.remove("ui/panel.js");
        assertEquals(AddonSignature.Status.MISMATCH, status(removed, example.trusted()));
    }

    @Test
    public void anUnreadableSignatureFileIsAMismatch() {
        Map<String, byte[]> files = new TreeMap<>(signed());
        files.put(AddonSignature.SIGNATURE_FILE, "not json".getBytes(StandardCharsets.UTF_8));
        assertEquals(AddonSignature.Status.MISMATCH, status(files, example.trusted()));
        files.put(AddonSignature.SIGNATURE_FILE, "{\"alg\":\"rsa\"}".getBytes(StandardCharsets.UTF_8));
        assertEquals(AddonSignature.Status.MISMATCH, status(files, example.trusted()));
    }

    @Test
    public void theSignedListingIsTheSha256sumFormatSortedByName() {
        Map<String, byte[]> files = new TreeMap<>();
        files.put("b.txt", "b".getBytes(StandardCharsets.UTF_8));
        files.put("a.txt", "a".getBytes(StandardCharsets.UTF_8));
        files.put(AddonSignature.SIGNATURE_FILE, "ignored".getBytes(StandardCharsets.UTF_8));
        // sha256("a"), sha256("b"): what `sha256sum a.txt b.txt` prints
        String expected = "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb  a.txt\n"
                + "3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d  b.txt\n";
        assertEquals(expected, new String(AddonSignature.signedListing(files), StandardCharsets.UTF_8));
    }

    @Test
    public void trustedPublishersAcceptRawAndDerKeys() {
        byte[] raw = Base64.getDecoder().decode(example.publicKeyBase64());
        String der = Base64.getEncoder().encodeToString(example.keys.getPublic().getEncoded());
        Map<String, byte[]> parsed = AddonSignature
                .parseTrustedPublishers("a=" + example.publicKeyBase64() + ", b=" + der);
        assertArrayEquals(raw, parsed.get("a"));
        assertArrayEquals(raw, parsed.get("b"));
        assertTrue(AddonSignature.parseTrustedPublishers("  ").isEmpty());
    }

    @Test
    public void aMalformedTrustedPublisherSettingIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AddonSignature.parseTrustedPublishers("example"));
        assertThrows(IllegalArgumentException.class, () -> AddonSignature.parseTrustedPublishers("example=AAAA"));
    }
}
