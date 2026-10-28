package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Optional;
import javax.net.ssl.SSLContext;

/**
 * OpenELIS's pairing with the Analyzer Bridge. Pairing exchanges certificates
 * once, authorized by a one-time code the Bridge shows or is configured with;
 * from then on each side accepts only the other's certificate.
 */
public interface AnalyzerBridgePairingService {

    /**
     * {@code pairedAt} is sent as ISO-8601 text, which the page reads as a date.
     * {@code pinCheck} is null while unpaired or when the Bridge's answer cannot be
     * read, and {@code UNREACHABLE} when it cannot be reached.
     */
    record Status(String bridgeUrl, boolean paired, String bridgeCertificateSha256, String clientCertificateSha256,
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant pairedAt, boolean pairsAutomatically,
            PinCheck pinCheck) {
    }

    /** What the Bridge at the configured address says about the stored pin. */
    enum PinCheck {
        MATCHES,
        /** The Bridge presents a certificate other than the pinned one. */
        OTHER_CERTIFICATE,
        /** The Bridge presents the pinned certificate but holds no pairing. */
        BRIDGE_UNPAIRED,
        /** Nothing answers at the configured address. */
        UNREACHABLE
    }

    /** The TLS context for calls to the paired Bridge, and whom it trusts. */
    record PairedBridge(String bridgeCertificateSha256, SSLContext sslContext) {
    }

    Status getStatus();

    /**
     * Pairs with the configured Bridge using its pairing code.
     *
     * @throws BridgePairingException with a message key when the Bridge refuses or
     *                                cannot be reached
     */
    Status pair(String code, String sysUserId);

    Optional<PairedBridge> getPairedBridge();

    /** Whether a TLS client certificate is the paired Bridge's. */
    boolean isPairedBridge(X509Certificate certificate);

    /**
     * Pairs with the configured code while unpaired, or when the Bridge no longer
     * matches the stored pin; does nothing otherwise.
     */
    void pairWithConfiguredCode();
}
