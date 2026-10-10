package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jasypt.util.text.AES256TextEncryptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.dao.AnalyzerBridgePairingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerBridgePairing;

public class AnalyzerBridgePairingServiceTest {

    private static final String CODE = "ABCD-EFGH-JKLM";

    private final AtomicReference<AnalyzerBridgePairing> stored = new AtomicReference<>();
    private AnalyzerBridgePairingDAO dao;
    private AES256TextEncryptor encryptor;
    private FakeBridge bridge;

    @Before
    public void setUp() throws Exception {
        dao = mock(AnalyzerBridgePairingDAO.class);
        when(dao.findCurrent()).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(dao.insert(any())).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return "1";
        });
        when(dao.update(any())).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        encryptor = new AES256TextEncryptor();
        encryptor.setPassword(java.util.UUID.randomUUID().toString());
        bridge = new FakeBridge(CODE);
    }

    @After
    public void tearDown() {
        bridge.close();
    }

    private AnalyzerBridgePairingServiceImpl service(String configuredCode) {
        return new AnalyzerBridgePairingServiceImpl(dao, encryptor, bridge.url() + "/", configuredCode, "", "");
    }

    @Test
    public void pairingPinsTheBridgeAndTheBridgePinsOpenElis() {
        AnalyzerBridgePairingService pairing = service("");

        AnalyzerBridgePairingService.Status status = pairing.pair(CODE, "7");

        assertTrue(status.paired());
        assertEquals(sha256(bridge.identity.certificate()), status.bridgeCertificateSha256());
        assertEquals(status.clientCertificateSha256(), bridge.pairedClient());
        assertEquals("7", stored.get().getPairedBy());
        assertTrue(pairing.isPairedBridge(bridge.identity.certificate()));
    }

    @Test
    public void thePrivateKeyIsStoredEncrypted() throws Exception {
        service("").pair(CODE, "7");

        String storedKey = stored.get().getClientPrivateKey();

        assertFalse(storedKey.startsWith("MI"));
        BridgeTls.privateKey(java.util.Base64.getDecoder().decode(encryptor.decrypt(storedKey)));
    }

    @Test
    public void callsAfterPairingTrustOnlyThePairedBridge() throws Exception {
        AnalyzerBridgePairingService pairing = service("");
        pairing.pair(CODE, "7");

        HttpClient client = HttpClient.newBuilder().sslContext(pairing.getPairedBridge().orElseThrow().sslContext())
                .build();
        HttpResponse<String> paired = client.send(
                HttpRequest.newBuilder(URI.create(bridge.url() + "/api/ping")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, paired.statusCode());

        try (FakeBridge impostor = new FakeBridge(CODE)) {
            assertThrows(javax.net.ssl.SSLHandshakeException.class,
                    () -> client.send(HttpRequest.newBuilder(URI.create(impostor.url() + "/api/ping")).build(),
                            HttpResponse.BodyHandlers.ofString()));
        }
    }

    @Test
    public void aWrongCodeLeavesOpenElisUnpaired() {
        AnalyzerBridgePairingService pairing = service("");

        BridgePairingException error = assertThrows(BridgePairingException.class, () -> pairing.pair("WRONG", "7"));

        assertEquals("analyzer.bridgePairing.error.wrongCode", error.getMessage());
        assertFalse(pairing.getStatus().paired());
        assertTrue(pairing.getPairedBridge().isEmpty());
    }

    @Test
    public void aBridgeThatAnswersForAnotherCertificateIsNotPinned() throws Exception {
        bridge.announce(sha256(BridgeTls.generateIdentity().certificate()));
        AnalyzerBridgePairingService pairing = service("");

        BridgePairingException error = assertThrows(BridgePairingException.class, () -> pairing.pair(CODE, "7"));

        assertEquals("analyzer.bridgePairing.error.certificateMismatch", error.getMessage());
        assertFalse(pairing.getStatus().paired());
    }

    @Test
    public void aClosedBridgeSaysSo() {
        service("").pair(CODE, "7");
        stored.set(null);

        BridgePairingException error = assertThrows(BridgePairingException.class, () -> service("").pair(CODE, "7"));

        assertEquals("analyzer.bridgePairing.error.closed", error.getMessage());
    }

    @Test
    public void theConfiguredCodePairsOnceAndOnlyWhileUnpaired() {
        AnalyzerBridgePairingService pairing = service(CODE);
        assertTrue(pairing.getStatus().pairsAutomatically());

        pairing.pairWithConfiguredCode();
        String pairedClient = bridge.pairedClient();
        pairing.pairWithConfiguredCode();

        assertTrue(pairing.getStatus().paired());
        assertNull(stored.get().getPairedBy());
        assertEquals(pairedClient, bridge.pairedClient());
    }

    @Test
    public void aPairingMadeInTheOtherWebContextIsTrustedWithoutARestart() throws Exception {
        AnalyzerBridgePairingService delivery = service("");
        service("").pair(CODE, "7");
        assertTrue(delivery.isPairedBridge(bridge.identity.certificate()));
        delivery.getPairedBridge().orElseThrow();

        try (FakeBridge reinstalled = new FakeBridge(CODE)) {
            new AnalyzerBridgePairingServiceImpl(dao, encryptor, reinstalled.url() + "/", "", "", "").pair(CODE, "7");

            assertTrue(delivery.isPairedBridge(reinstalled.identity.certificate()));
            assertFalse("the replaced Bridge is no longer trusted",
                    delivery.isPairedBridge(bridge.identity.certificate()));
            assertEquals(sha256(reinstalled.identity.certificate()),
                    delivery.getPairedBridge().orElseThrow().bridgeCertificateSha256());
        }
    }

    @Test
    public void aConfiguredCodeTheBridgeRefusesIsNotTriedAgain() {
        AnalyzerBridgePairingService pairing = service("WRONG-CODE");

        pairing.pairWithConfiguredCode();
        pairing.pairWithConfiguredCode();

        assertEquals("the Bridge keeps its attempts for the code entered on the page", 1, bridge.pairingAttempts.get());
        assertFalse(pairing.getStatus().paired());
        assertTrue(pairing.pair(CODE, "7").paired());
    }

    @Test
    public void openElisSendsTheCertificateItServesSoTheBridgeCanPinIt() throws Exception {
        BridgeTls.Identity served = BridgeTls.generateIdentity();
        Path keyStore = Files.createTempFile("served", ".p12");
        String storeCredential = java.util.UUID.randomUUID().toString();
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry("served", served.privateKey(), storeCredential.toCharArray(),
                new X509Certificate[] { served.certificate() });
        try (OutputStream out = Files.newOutputStream(keyStore)) {
            store.store(out, storeCredential.toCharArray());
        }

        new AnalyzerBridgePairingServiceImpl(dao, encryptor, bridge.url(), "", "file:" + keyStore, storeCredential)
                .pair(CODE, "7");

        assertEquals(List.of(sha256(served.certificate())), bridge.receivedServerFingerprints);
    }

    @Test
    public void withoutABridgeUrlThereIsNothingToPairWith() {
        AnalyzerBridgePairingServiceImpl pairing = new AnalyzerBridgePairingServiceImpl(dao, encryptor, "", CODE, "",
                "");

        BridgePairingException error = assertThrows(BridgePairingException.class, () -> pairing.pair(CODE, "7"));

        assertEquals("analyzer.bridgePairing.error.notConfigured", error.getMessage());
        pairing.pairWithConfiguredCode();
        assertNull(stored.get());
    }

    private static String sha256(X509Certificate certificate) {
        try {
            return BridgeTls.sha256(certificate);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
