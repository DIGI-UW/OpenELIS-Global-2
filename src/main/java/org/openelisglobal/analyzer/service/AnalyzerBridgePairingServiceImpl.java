package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.SSLContext;
import org.jasypt.util.text.TextEncryptor;
import org.openelisglobal.analyzer.dao.AnalyzerBridgePairingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerBridgePairing;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.security.KeystoreUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AnalyzerBridgePairingServiceImpl implements AnalyzerBridgePairingService {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final Set<String> REFUSALS = Set.of("analyzer.bridgePairing.error.wrongCode",
            "analyzer.bridgePairing.error.closed");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AnalyzerBridgePairingDAO dao;
    private final TextEncryptor encryptor;
    private final String bridgeUrl;
    private final String configuredCode;
    private final String serverKeyStore;
    private final String serverKeyStoreCredential;

    // The WAR runs in two web contexts, each with its own copy of this service, and
    // either may pair. The stored pairing is the only shared state, so the pin is
    // read from it; only the TLS context built for a pin is kept.
    private volatile PairedBridge pairedBridge;
    // A code the Bridge refused will be refused again; retrying it would use up the
    // Bridge's attempts before the right code is entered on the Analyzers page.
    private volatile boolean configuredCodeRefused;

    public AnalyzerBridgePairingServiceImpl(AnalyzerBridgePairingDAO dao, TextEncryptor encryptor,
            @Value("${analyzer.bridge.url:}") String bridgeUrl,
            @Value("${analyzer.bridge.pairing-code:}") String configuredCode,
            @Value("${server.ssl.key-store:}") String serverKeyStore,
            @Value("${server.ssl.key-store-password:}") String serverKeyStoreCredential) {
        this.dao = dao;
        this.encryptor = encryptor;
        this.bridgeUrl = bridgeUrl == null ? "" : bridgeUrl.trim().replaceAll("/+$", "");
        this.configuredCode = configuredCode == null ? "" : configuredCode.trim();
        this.serverKeyStore = serverKeyStore;
        this.serverKeyStoreCredential = serverKeyStoreCredential;
    }

    @Override
    public Status getStatus() {
        Optional<AnalyzerBridgePairing> record = dao.findCurrent();
        String clientFingerprint = record.map(this::clientFingerprint).orElse(null);
        boolean paired = record.map(r -> r.getBridgeCertificateSha256() != null).orElse(false);
        return new Status(bridgeUrl, paired, paired ? record.get().getBridgeCertificateSha256() : null,
                clientFingerprint, paired ? record.get().getPairedAt().toInstant() : null, !configuredCode.isEmpty());
    }

    @Override
    public synchronized Status pair(String code, String sysUserId) {
        if (bridgeUrl.isEmpty()) {
            throw new BridgePairingException("analyzer.bridgePairing.error.notConfigured");
        }
        if (code == null || code.isBlank()) {
            throw new BridgePairingException("analyzer.bridgePairing.error.codeRequired");
        }
        AnalyzerBridgePairing record = identity();
        BridgeTls.ObservingServerTrustManager observed = new BridgeTls.ObservingServerTrustManager();
        JsonNode answer = requestPairing(record, code.trim(), observed);
        String bridgeFingerprint = answer.path("bridgeCertificateSha256").asText("").toLowerCase();
        try {
            X509Certificate served = observed.observed();
            if (served == null || !BridgeTls.sha256(served).equals(bridgeFingerprint)) {
                throw new BridgePairingException("analyzer.bridgePairing.error.certificateMismatch");
            }
            record.setBridgeCertificate(BridgeTls.pem(served));
        } catch (CertificateException e) {
            throw new BridgePairingException("analyzer.bridgePairing.error.certificateMismatch", e);
        }
        record.setBridgeUrl(bridgeUrl);
        record.setBridgeCertificateSha256(bridgeFingerprint);
        record.setPairedAt(Timestamp.from(Instant.now()));
        record.setPairedBy(sysUserId);
        dao.update(record);
        pairedBridge = null;
        LogEvent.logInfo(getClass().getSimpleName(), "pair",
                "Paired with the Analyzer Bridge at " + bridgeUrl + ", certificate " + bridgeFingerprint);
        return getStatus();
    }

    @Override
    public Optional<PairedBridge> getPairedBridge() {
        Optional<AnalyzerBridgePairing> record = dao.findCurrent().filter(r -> r.getBridgeCertificateSha256() != null);
        if (record.isEmpty()) {
            pairedBridge = null;
            return Optional.empty();
        }
        String fingerprint = record.get().getBridgeCertificateSha256();
        PairedBridge cached = pairedBridge;
        if (cached != null && cached.bridgeCertificateSha256().equals(fingerprint)) {
            return Optional.of(cached);
        }
        try {
            SSLContext context = BridgeTls.context(clientIdentity(record.get()),
                    new BridgeTls.PinnedServerTrustManager(fingerprint));
            cached = new PairedBridge(fingerprint, context);
            pairedBridge = cached;
            return Optional.of(cached);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("The stored Analyzer Bridge pairing cannot be read", e);
        }
    }

    @Override
    public boolean isPairedBridge(X509Certificate certificate) {
        if (certificate == null) {
            return false;
        }
        String pinned = dao.findCurrent().map(AnalyzerBridgePairing::getBridgeCertificateSha256).orElse(null);
        if (pinned == null) {
            return false;
        }
        try {
            return pinned.equalsIgnoreCase(BridgeTls.sha256(certificate));
        } catch (CertificateException e) {
            return false;
        }
    }

    @Override
    @Scheduled(initialDelay = 5_000, fixedDelay = 30_000)
    public void pairWithConfiguredCode() {
        if (configuredCode.isEmpty() || bridgeUrl.isEmpty() || configuredCodeRefused || getStatus().paired()) {
            return;
        }
        try {
            pair(configuredCode, null);
        } catch (BridgePairingException e) {
            if (REFUSALS.contains(e.getMessage())) {
                configuredCodeRefused = true;
                LogEvent.logError(getClass().getSimpleName(), "pairWithConfiguredCode",
                        "The Analyzer Bridge at " + bridgeUrl + " refused the configured pairing code ("
                                + e.getMessage()
                                + "); it is not tried again. Enter the Bridge's code on the Analyzers page.");
                return;
            }
            LogEvent.logWarn(getClass().getSimpleName(), "pairWithConfiguredCode",
                    "Could not pair with the Analyzer Bridge at " + bridgeUrl + " yet: " + e.getMessage());
        }
    }

    private JsonNode requestPairing(AnalyzerBridgePairing record, String code,
            BridgeTls.ObservingServerTrustManager observed) {
        HttpResponse<String> response;
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT)
                    .sslContext(BridgeTls.context(clientIdentity(record), observed)).build();
            Map<String, String> body = new LinkedHashMap<>();
            body.put("code", code);
            servedCertificateFingerprint().ifPresent(fingerprint -> body.put("serverCertificateSha256", fingerprint));
            response = client.send(
                    HttpRequest.newBuilder(URI.create(bridgeUrl + "/pairing")).timeout(TIMEOUT)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (GeneralSecurityException e) {
            throw new BridgePairingException("analyzer.bridgePairing.error.identity", e);
        } catch (IOException e) {
            throw new BridgePairingException("analyzer.bridgePairing.error.unreachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BridgePairingException("analyzer.bridgePairing.error.unreachable", e);
        }
        switch (response.statusCode()) {
        case 200:
            try {
                return JSON.readTree(response.body());
            } catch (IOException e) {
                throw new BridgePairingException("analyzer.bridgePairing.error.invalidAnswer", e);
            }
        case 403:
            throw new BridgePairingException("analyzer.bridgePairing.error.wrongCode");
        case 409:
            throw new BridgePairingException("analyzer.bridgePairing.error.closed");
        default:
            throw new BridgePairingException("analyzer.bridgePairing.error.refused");
        }
    }

    /** The pairing record with OpenELIS's client identity, created on first use. */
    private AnalyzerBridgePairing identity() {
        Optional<AnalyzerBridgePairing> existing = dao.findCurrent();
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            BridgeTls.Identity identity = BridgeTls.generateIdentity();
            AnalyzerBridgePairing record = new AnalyzerBridgePairing();
            record.setClientPrivateKey(
                    encryptor.encrypt(Base64.getEncoder().encodeToString(identity.privateKey().getEncoded())));
            record.setClientCertificate(BridgeTls.pem(identity.certificate()));
            dao.insert(record);
            return record;
        } catch (GeneralSecurityException e) {
            throw new BridgePairingException("analyzer.bridgePairing.error.identity", e);
        }
    }

    private BridgeTls.Identity clientIdentity(AnalyzerBridgePairing record) throws GeneralSecurityException {
        return new BridgeTls.Identity(
                BridgeTls.privateKey(Base64.getDecoder().decode(encryptor.decrypt(record.getClientPrivateKey()))),
                BridgeTls.certificate(record.getClientCertificate()));
    }

    private String clientFingerprint(AnalyzerBridgePairing record) {
        try {
            return BridgeTls.sha256(BridgeTls.certificate(record.getClientCertificate()));
        } catch (CertificateException e) {
            return null;
        }
    }

    /**
     * The certificate OpenELIS serves HTTPS with, which the Bridge pins for its
     * calls to OpenELIS. Without it the Bridge accepts only what its own trust
     * store validates.
     */
    private Optional<String> servedCertificateFingerprint() {
        if (serverKeyStore == null || serverKeyStore.isBlank()) {
            return Optional.empty();
        }
        try {
            char[] credential = serverKeyStoreCredential == null ? new char[0] : serverKeyStoreCredential.toCharArray();
            KeyStore keyStore = KeystoreUtil.readKeyStoreFile(new DefaultResourceLoader().getResource(serverKeyStore),
                    credential);
            return Optional
                    .of(BridgeTls.sha256((X509Certificate) KeystoreUtil.getCertFromKeyStore(keyStore, credential)));
        } catch (Exception e) {
            LogEvent.logWarn(getClass().getSimpleName(), "servedCertificateFingerprint",
                    "Cannot read the HTTPS certificate from " + serverKeyStore + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
