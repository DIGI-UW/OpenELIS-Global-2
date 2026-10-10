package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jasypt.util.text.AES256TextEncryptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.dao.AnalyzerBridgePairingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerBridgePairing;

public class BridgeHttpClientTest {

    private FakeBridge bridge;
    private AnalyzerBridgePairingService pairing;
    private AnalyzerBridgePairingDAO dao;
    private AES256TextEncryptor encryptor;

    @Before
    public void setUp() throws Exception {
        AtomicReference<AnalyzerBridgePairing> stored = new AtomicReference<>();
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
        bridge = new FakeBridge("CODE");
        pairing = new AnalyzerBridgePairingServiceImpl(dao, encryptor, bridge.url(), "", "", "");
    }

    @After
    public void tearDown() {
        bridge.close();
    }

    @Test
    public void anUnpairedBridgeIsNotCalled() {
        BridgeHttpClient client = new BridgeHttpClient(pairing);

        BridgeHttpClient.BridgeNotPairedException error = assertThrows(BridgeHttpClient.BridgeNotPairedException.class,
                () -> client.get(bridge.url() + "/api/ping", Duration.ofSeconds(5)));

        assertEquals("The Analyzer Bridge is not paired with OpenELIS", error.getMessage());
    }

    @Test
    public void aConfiguredCodePairsOnTheFirstCall() throws Exception {
        BridgeHttpClient client = new BridgeHttpClient(
                new AnalyzerBridgePairingServiceImpl(dao, encryptor, bridge.url(), "CODE", "", ""));

        BridgeHttpClient.BridgeResponse response = client.get(bridge.url() + "/api/ping", Duration.ofSeconds(5));

        assertEquals(200, response.status);
    }

    @Test
    public void thePairedBridgeIsCalledWithTheCertificateAndNoPassword() throws Exception {
        pairing.pair("CODE", "7");
        BridgeHttpClient client = new BridgeHttpClient(pairing);

        BridgeHttpClient.BridgeResponse response = client.get(bridge.url() + "/api/ping", Duration.ofSeconds(5));

        assertEquals(200, response.status);
        assertNull(bridge.lastAuthorization());
    }
}
