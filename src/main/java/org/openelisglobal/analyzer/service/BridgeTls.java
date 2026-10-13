package org.openelisglobal.analyzer.service;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * The TLS pieces of OpenELIS's link with the Analyzer Bridge: OpenELIS's own
 * key pair, certificate fingerprints, and trust by fingerprint.
 *
 * <p>
 * Both sides use self-signed certificates and trust each other by the SHA-256
 * fingerprint each recorded when they paired, so neither needs a certificate
 * authority, and host names do not enter into it.
 */
public final class BridgeTls {

    private static final String KEY_ALIAS = "openelis";

    public record Identity(PrivateKey privateKey, X509Certificate certificate) {
    }

    private BridgeTls() {
    }

    public static Identity generateIdentity() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        X500Name name = new X500Name("CN=OpenELIS Global analyzer client");
        Instant now = Instant.now();
        try {
            X509Certificate certificate = new JcaX509CertificateConverter()
                    .getCertificate(new JcaX509v3CertificateBuilder(name, new BigInteger(64, new SecureRandom()),
                            Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(36500, ChronoUnit.DAYS)), name,
                            keyPair.getPublic())
                            .build(new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate())));
            return new Identity(keyPair.getPrivate(), certificate);
        } catch (org.bouncycastle.operator.OperatorCreationException e) {
            throw new GeneralSecurityException("Cannot sign the OpenELIS analyzer client certificate", e);
        }
    }

    public static String sha256(X509Certificate certificate) throws CertificateException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String pem(X509Certificate certificate) throws CertificateException {
        return "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(certificate.getEncoded()) + "\n-----END CERTIFICATE-----\n";
    }

    public static X509Certificate certificate(String pem) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    public static PrivateKey privateKey(byte[] pkcs8) throws GeneralSecurityException {
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }

    /** A TLS context that presents this identity and trusts servers as given. */
    public static SSLContext context(Identity identity, X509ExtendedTrustManager trust)
            throws GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try {
            keyStore.load(null, null);
        } catch (java.io.IOException e) {
            throw new GeneralSecurityException(e);
        }
        // The key store exists only to hand the key to the key manager.
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        char[] transientPassword = HexFormat.of().formatHex(random).toCharArray();
        keyStore.setKeyEntry(KEY_ALIAS, identity.privateKey(), transientPassword,
                new X509Certificate[] { identity.certificate() });
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(keyStore, transientPassword);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), new TrustManager[] { trust }, new SecureRandom());
        return context;
    }

    /** Trusts exactly one server certificate, the one paired. */
    public static final class PinnedServerTrustManager extends ServerOnlyTrustManager {

        private final String fingerprint;

        public PinnedServerTrustManager(String fingerprint) {
            this.fingerprint = fingerprint;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (chain == null || chain.length == 0 || !sha256(chain[0]).equalsIgnoreCase(fingerprint)) {
                throw new CertificateException("The Analyzer Bridge is not the one OpenELIS paired with");
            }
        }
    }

    /**
     * Accepts whichever server certificate is presented and remembers it. Used only
     * to pair, where the Bridge proves itself by accepting the pairing code and
     * OpenELIS then pins the certificate it saw.
     */
    public static final class ObservingServerTrustManager extends ServerOnlyTrustManager {

        private final AtomicReference<X509Certificate> observed = new AtomicReference<>();

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (chain == null || chain.length == 0) {
                throw new CertificateException("The Analyzer Bridge presented no certificate");
            }
            observed.set(chain[0]);
        }

        public X509Certificate observed() {
            return observed.get();
        }
    }

    /**
     * An extended trust manager, so the JDK leaves host name checks to it: the
     * Bridge's certificate names only localhost, and its fingerprint is what is
     * checked.
     */
    private abstract static class ServerOnlyTrustManager extends X509ExtendedTrustManager {

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            checkServerTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            checkServerTrusted(chain, authType);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            throw new CertificateException("Only server certificates are checked here");
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            checkClientTrusted(chain, authType);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            checkClientTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
