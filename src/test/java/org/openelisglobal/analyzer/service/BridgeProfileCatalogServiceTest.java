package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        assertEquals("sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                catalog.profiles().get(0).controlRecognitionSummary().recognitionFingerprint());
        assertEquals("RULES", catalog.profiles().get(0).controlRecognitionSummary().mode());
        assertEquals("Specimen ID starts with QC-",
                catalog.profiles().get(0).controlRecognitionSummary().conditions().get(0).description());
        assertEquals("SPECIMEN_ID_STARTS_WITH",
                catalog.profiles().get(0).controlRecognitionSummary().conditions().get(0).kind());
        assertEquals("Specimen ID",
                catalog.profiles().get(0).controlRecognitionSummary().conditions().get(0).sourceLabel());
        assertEquals("QC-", catalog.profiles().get(0).controlRecognitionSummary().conditions().get(0).value());
    }

    @Test
    public void getProfileFetchesAndValidatesTheExactRequestedRevision() throws Exception {
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles/site.mock%20hematology?revision=2"),
                any(Duration.class))).thenReturn(new BridgeHttpClient.BridgeResponse(200, validProfileRevision()));

        BridgeProfileCatalog.ProfileRevision revision = service.getProfile("site.mock hematology", 2);

        assertEquals("site.mock hematology", revision.profile().path("profileMeta").path("id").asText());
        assertEquals(2, revision.profile().path("catalog").path("revision").asInt());
        assertEquals("PUBLISHED", revision.publication().path("action").asText());
        assertEquals("NONE", revision.controlRecognitionSummary().mode());
        assertEquals(true, revision.controlRecognitionSummary().affirmedNoControlResults());
    }

    @Test
    public void getProfileRejectsAResponseForADifferentRevision() throws Exception {
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles/site.mock%20hematology?revision=1"),
                any(Duration.class))).thenReturn(new BridgeHttpClient.BridgeResponse(200, validProfileRevision()));

        BridgeProfileCatalogException exception = assertThrows(BridgeProfileCatalogException.class,
                () -> service.getProfile("site.mock hematology", 1));

        assertEquals("Bridge returned a different profile revision than requested", exception.getMessage());
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

    @Test
    public void getCatalogRejectsRevisionWithoutControlRecognitionSummary() throws Exception {
        JsonNode catalog = new ObjectMapper().readTree(validCatalog());
        ((ObjectNode) catalog.path("profiles").get(0)).remove("controlRecognitionSummary");
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles"), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, catalog.toString()));

        BridgeProfileCatalogException exception = assertThrows(BridgeProfileCatalogException.class,
                () -> service.getCatalog());

        assertEquals("Bridge profile catalog contains an invalid control recognition summary", exception.getMessage());
    }

    @Test
    public void getCatalogRejectsRecognitionSummaryWithoutFingerprint() throws Exception {
        JsonNode catalog = new ObjectMapper().readTree(validCatalog());
        ((ObjectNode) catalog.path("profiles").get(0).path("controlRecognitionSummary"))
                .remove("recognitionFingerprint");
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles"), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, catalog.toString()));

        BridgeProfileCatalogException exception = assertThrows(BridgeProfileCatalogException.class,
                () -> service.getCatalog());

        assertEquals("Bridge profile catalog contains an invalid control recognition summary", exception.getMessage());
    }

    @Test
    public void getCatalogRejectsRuleSummaryWithoutSafeSemanticFields() throws Exception {
        JsonNode catalog = new ObjectMapper().readTree(validCatalog());
        ((ObjectNode) catalog.path("profiles").get(0).path("controlRecognitionSummary").path("conditions").get(0))
                .remove("kind");
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles"), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, catalog.toString()));

        BridgeProfileCatalogException exception = assertThrows(BridgeProfileCatalogException.class,
                () -> service.getCatalog());

        assertEquals("Bridge profile catalog contains an invalid control recognition summary", exception.getMessage());
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
                      "communication":{"mode":"ANALYZER_INITIATED","supports_lis_initiated":false},
                      "configDefaults":{"connectionRole":"SERVER","aggregationMode":"PER_MESSAGE"},
                      "catalog":{
                        "revision":3,
                        "revisionFingerprint":"sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        "source":"SHIPPED",
                        "status":"ACTIVE"
                      }
                    },
                    "controlRecognitionSummary":{
                      "recognitionFingerprint":"sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                      "mode":"RULES",
                      "description":"Control results match any configured condition.",
                      "affirmedNoControlResults":false,
                      "conditions":[{
                        "key":"qc-prefix",
                        "kind":"SPECIMEN_ID_STARTS_WITH",
                        "sourceLabel":"Specimen ID",
                        "value":"QC-",
                        "description":"Specimen ID starts with QC-",
                        "controlLevel":"QC"
                      }]
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

    private static String validProfileRevision() {
        return """
                {
                  "profile":{
                    "schemaVersion":"1.0",
                    "profileMeta":{"id":"site.mock hematology","version":"1.0.0","displayName":"Mock Hematology revision 2","confidence":"HIGH"},
                    "protocol":{"name":"ASTM","version":"LIS2-A2"},
                    "communication":{"mode":"ANALYZER_INITIATED","supports_lis_initiated":false},
                    "configDefaults":{"connectionRole":"SERVER","aggregationMode":"PER_MESSAGE","port":9200},
                    "catalog":{
                      "revision":2,
                      "revisionFingerprint":"sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                      "source":"SITE",
                      "status":"ACTIVE"
                    }
                  },
                  "controlRecognitionSummary":{
                    "recognitionFingerprint":"sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
                    "mode":"NONE",
                    "description":"This analyzer interface transports no control results.",
                    "affirmedNoControlResults":true,
                    "conditions":[]
                  },
                  "publication":{"action":"PUBLISHED","actor":"17","markedAt":"2026-08-18T12:00:00Z"}
                }
                """;
    }
}
