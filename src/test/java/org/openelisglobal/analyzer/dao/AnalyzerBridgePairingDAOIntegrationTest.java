package org.openelisglobal.analyzer.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerBridgePairing;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyzerBridgePairingDAOIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String[] TABLES = { "analyzer_bridge_pairing" };

    @Autowired
    private AnalyzerBridgePairingDAO dao;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        cleanRowsInCurrentConnection(TABLES);
    }

    @After
    public void tearDown() throws Exception {
        cleanRowsInCurrentConnection(TABLES);
    }

    @Test
    public void anIdentityIsStoredBeforeAnyPairingAndThePairingIsAddedToIt() {
        AnalyzerBridgePairing identity = new AnalyzerBridgePairing();
        identity.setClientPrivateKey("encrypted-key");
        identity.setClientCertificate("-----BEGIN CERTIFICATE-----");
        dao.insert(identity);

        AnalyzerBridgePairing unpaired = dao.findCurrent().orElseThrow();
        assertNull(unpaired.getBridgeCertificateSha256());

        unpaired.setBridgeUrl("https://bridge.openelis.org:8443");
        unpaired.setBridgeCertificate("-----BEGIN CERTIFICATE-----");
        unpaired.setBridgeCertificateSha256("ab".repeat(32));
        unpaired.setPairedAt(Timestamp.from(Instant.parse("2026-10-08T12:00:00Z")));
        unpaired.setPairedBy("1");
        dao.update(unpaired);

        AnalyzerBridgePairing paired = dao.findCurrent().orElseThrow();
        assertEquals("encrypted-key", paired.getClientPrivateKey());
        assertEquals("ab".repeat(32), paired.getBridgeCertificateSha256());
        assertEquals("https://bridge.openelis.org:8443", paired.getBridgeUrl());
        assertEquals("1", paired.getPairedBy());
        assertTrue(dao.findCurrent().isPresent());
    }
}
