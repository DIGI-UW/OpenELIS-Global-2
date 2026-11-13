package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.junit.Test;

public class BridgeTlsTest {

    @Test
    public void aGeneratedIdentityIsAKeyPairWithItsOwnCertificate() throws Exception {
        BridgeTls.Identity identity = BridgeTls.generateIdentity();

        byte[] data = "pair".getBytes();
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(identity.privateKey());
        signer.update(data);
        byte[] signature = signer.sign();
        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(identity.certificate());
        verifier.update(data);

        assertTrue(verifier.verify(signature));
        identity.certificate().checkValidity();
        identity.certificate().verify(identity.certificate().getPublicKey());
    }

    @Test
    public void certificatesAndKeysSurviveTheirStoredForms() throws Exception {
        BridgeTls.Identity identity = BridgeTls.generateIdentity();

        X509Certificate restored = BridgeTls.certificate(BridgeTls.pem(identity.certificate()));

        assertEquals(identity.certificate(), restored);
        assertArrayEquals(identity.privateKey().getEncoded(),
                BridgeTls.privateKey(identity.privateKey().getEncoded()).getEncoded());
    }

    @Test
    public void aFingerprintIsTheLowercaseHexSha256OfTheCertificate() throws Exception {
        BridgeTls.Identity identity = BridgeTls.generateIdentity();

        String fingerprint = BridgeTls.sha256(identity.certificate());

        assertTrue(fingerprint.matches("[0-9a-f]{64}"));
        assertNotEquals(fingerprint, BridgeTls.sha256(BridgeTls.generateIdentity().certificate()));
    }

    @Test
    public void thePinnedTrustManagerTrustsOnlyThePinnedServer() throws Exception {
        X509Certificate pinned = BridgeTls.generateIdentity().certificate();
        X509Certificate other = BridgeTls.generateIdentity().certificate();
        BridgeTls.PinnedServerTrustManager trust = new BridgeTls.PinnedServerTrustManager(BridgeTls.sha256(pinned));

        trust.checkServerTrusted(new X509Certificate[] { pinned }, "ECDHE_ECDSA");

        assertThrows(CertificateException.class,
                () -> trust.checkServerTrusted(new X509Certificate[] { other }, "ECDHE_ECDSA"));
        assertThrows(CertificateException.class, () -> trust.checkServerTrusted(new X509Certificate[0], "ECDHE_ECDSA"));
        assertThrows(CertificateException.class,
                () -> trust.checkClientTrusted(new X509Certificate[] { pinned }, "ECDHE_ECDSA"));
    }
}
