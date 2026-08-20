package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BridgeProfileCatalogServiceTest {

    @Mock
    private BridgeHttpClient bridgeHttpClient;

    private BridgeProfileCatalogService service;

    @Before
    public void setUp() {
        service = new BridgeProfileCatalogServiceImpl(bridgeHttpClient, "https://bridge.example/");
    }

    @Test
    public void getCatalogPreservesVersionedProfileAndPublicationData() throws Exception {
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles"), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, validCatalog()));

        BridgeProfileCatalog catalog = service.getCatalog();

        assertEquals("1.0", catalog.schemaVersion());
        assertEquals("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                catalog.catalogFingerprint());
        assertEquals(1, catalog.profiles().size());
        assertEquals("sysmex-xn", catalog.profiles().get(0).profile().path("profileMeta").path("id").asText());
        assertEquals(3, catalog.profiles().get(0).profile().path("catalog").path("revision").asInt());
        assertEquals("SHIPPED", catalog.profiles().get(0).publication().path("action").asText());
    }

    @Test
    public void getCatalogRejectsUnsupportedSchemaVersion() throws Exception {
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles"), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200,
                        validCatalog().replace("\"schemaVersion\":\"1.0\"", "\"schemaVersion\":\"2.0\"")));

        assertThrows(BridgeProfileCatalogException.class, () -> service.getCatalog());
    }

    @Test
    public void getCatalogFailsClosedWhenBridgeRejectsRequest() throws Exception {
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles"), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(401, "unauthorized"));

        BridgeProfileCatalogException exception = assertThrows(BridgeProfileCatalogException.class,
                () -> service.getCatalog());

        assertEquals("Bridge profile catalog request failed with HTTP 401", exception.getMessage());
    }

    private static String validCatalog() {
        return """
                {
                  "schemaVersion":"1.0",
                  "catalogFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "profiles":[{
                    "profile":{
                      "schemaVersion":"1.0",
                      "profileMeta":{"id":"sysmex-xn","version":"1.0.0","displayName":"Sysmex XN","confidence":"HIGH"},
                      "protocol":{"name":"ASTM","version":"LIS2-A2"},
                      "configDefaults":{"connectionRole":"SERVER","aggregationMode":"PER_MESSAGE"},
                      "catalog":{
                        "revision":3,
                        "revisionFingerprint":"sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        "source":"SHIPPED",
                        "status":"ACTIVE"
                      }
                    },
                    "publication":{
                      "action":"SHIPPED",
                      "actor":"release",
                      "markedAt":"2026-08-18T12:00:00Z"
                    }
                  }]
                }
                """;
    }
}
