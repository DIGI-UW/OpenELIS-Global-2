package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerTypeCatalogServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BridgeProfileCatalogService bridgeCatalogService;

    private AnalyzerTypeCatalogService service;

    @Before
    public void setUp() {
        service = new AnalyzerTypeCatalogServiceImpl(bridgeCatalogService);
    }

    @Test
    public void getCatalogComposesAnalyzerTypesFromBridgeProfilesWithoutLocalBindingState() throws Exception {
        when(bridgeCatalogService.getCatalog()).thenReturn(catalog());

        AnalyzerTypeCatalogView result = service.getCatalog();

        assertEquals("1.0", result.schemaVersion());
        assertEquals(2, result.summary().total());
        assertEquals(0, result.summary().inUse());
        assertEquals(1, result.summary().needsAttention());
        assertEquals(1, result.summary().deactivated());

        AnalyzerTypeCatalogView.TypeSummary active = result.types().get(0);
        assertEquals("site.mock-hematology", active.profileId());
        assertEquals(3, active.revision());
        assertEquals("Mock Hematology", active.displayName());
        assertEquals("OpenELIS", active.manufacturer());
        assertEquals("Mock H", active.model());
        assertEquals("SHIPPED", active.source());
        assertEquals("ACTIVE", active.status());
        assertEquals("ASTM", active.protocol());
        assertEquals("LIS2-A2", active.instanceDefaults().protocolVersion());
        assertEquals("BOTH", active.instanceDefaults().communicationMode());
        assertEquals(Integer.valueOf(9100), active.instanceDefaults().port());
        assertEquals(2, active.testMappings().total());
        assertEquals(0, active.testMappings().mapped());
        assertEquals("NOT_STARTED", active.testMappings().state());
        assertEquals(2, active.resultMappings().total());
        assertEquals(0, active.resultMappings().mapped());
        assertEquals("NOT_STARTED", active.resultMappings().state());
        assertNull(active.siteBindingId());
        assertEquals(0L, active.usedBy());
        assertEquals("NEEDS_LOCAL_MAPPING", active.readiness());

        AnalyzerTypeCatalogView.TypeSummary inactive = result.types().get(1);
        assertEquals("site.retired-file", inactive.profileId());
        assertEquals("SITE", inactive.source());
        assertEquals("INACTIVE", inactive.status());
        assertEquals("DEACTIVATED", inactive.readiness());
        assertEquals("site.file-base", inactive.parentProfileId());
        assertEquals(Integer.valueOf(1), inactive.parentRevision());
        assertEquals("NOT_APPLICABLE", inactive.resultMappings().state());
    }

    @Test
    public void getTypeReturnsTheExactRequestedBridgeRevision() throws Exception {
        BridgeProfileCatalog.ProfileRevision revision = profileRevision(2, "Mock Hematology revision 2", 9200);
        when(bridgeCatalogService.getProfile("site.mock-hematology", 2)).thenReturn(revision);

        AnalyzerTypeCatalogView.TypeSummary result = service.getType("site.mock-hematology", 2);

        assertEquals("site.mock-hematology", result.profileId());
        assertEquals(2, result.revision());
        assertEquals("Mock Hematology revision 2", result.displayName());
        assertEquals(Integer.valueOf(9200), result.instanceDefaults().port());
        assertEquals(0L, result.usedBy());
        assertNull(result.siteBindingId());
    }

    private BridgeProfileCatalog catalog() throws Exception {
        JsonNode active = objectMapper.readTree(
                """
                        {
                          "schemaVersion":"1.0",
                          "profileMeta":{"id":"site.mock-hematology","version":"1.0.0","displayName":"Mock Hematology","confidence":"VALIDATED"},
                          "manufacturer":"OpenELIS",
                          "model":"Mock H",
                          "protocol":{"name":"ASTM","version":"LIS2-A2"},
                          "communication":{"mode":"BOTH","supports_lis_initiated":true},
                          "default_test_mappings":[
                            {"test_code":"WBC","loinc":"6690-2","result_type":"quantitative"},
                            {"test_code":"FLAG","loinc":"58410-2","result_type":"qualitative","values":["POS","NEG"]}
                          ],
                          "configDefaults":{"connectionRole":"SERVER","defaultTransport":"TCP/IP","defaultPort":9100,"aggregationMode":"PER_MESSAGE"},
                          "catalog":{
                            "revision":3,
                            "revisionFingerprint":"sha256:1111111111111111111111111111111111111111111111111111111111111111",
                            "source":"SHIPPED",
                            "status":"ACTIVE"
                          }
                        }
                        """);
        JsonNode inactive = objectMapper.readTree(
                """
                        {
                          "schemaVersion":"1.0",
                          "profileMeta":{"id":"site.retired-file","version":"2.0.0","displayName":"Retired File Analyzer","confidence":"HIGH"},
                          "protocol":{"name":"FILE","format":"XLSX"},
                          "default_test_mappings":[{"test_code":"RESULT","loinc":"94500-6","result_type":"quantitative"}],
                          "catalog":{
                            "revision":2,
                            "revisionFingerprint":"sha256:2222222222222222222222222222222222222222222222222222222222222222",
                            "source":"SITE",
                            "status":"INACTIVE",
                            "lineage":{"parentProfileId":"site.file-base","parentRevision":1}
                          }
                        }
                        """);
        JsonNode publication = objectMapper.createObjectNode().put("action", "CREATED");
        return new BridgeProfileCatalog("1.0",
                "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                List.of(new BridgeProfileCatalog.ProfileRevision(active, publication),
                        new BridgeProfileCatalog.ProfileRevision(inactive, publication)));
    }

    private BridgeProfileCatalog.ProfileRevision profileRevision(int revision, String displayName, int port)
            throws Exception {
        JsonNode profile = objectMapper.readTree(
                """
                        {
                          "schemaVersion":"1.0",
                          "profileMeta":{"id":"site.mock-hematology","version":"1.0.0","displayName":"%s","confidence":"VALIDATED"},
                          "manufacturer":"OpenELIS",
                          "model":"Mock H",
                          "protocol":{"name":"ASTM","version":"LIS2-A2"},
                          "communication":{"mode":"BOTH","supports_lis_initiated":true},
                          "default_test_mappings":[{"test_code":"WBC","loinc":"6690-2","result_type":"quantitative"}],
                          "configDefaults":{"connectionRole":"SERVER","defaultTransport":"TCP/IP","defaultPort":%d,"aggregationMode":"PER_MESSAGE"},
                          "catalog":{
                            "revision":%d,
                            "revisionFingerprint":"sha256:3333333333333333333333333333333333333333333333333333333333333333",
                            "source":"SITE",
                            "status":"ACTIVE"
                          }
                        }
                        """
                        .formatted(displayName, port, revision));
        JsonNode publication = objectMapper
                .readTree("{\"action\":\"PUBLISHED\",\"actor\":\"17\",\"markedAt\":\"2026-08-18T12:00:00Z\"}");
        return new BridgeProfileCatalog.ProfileRevision(profile, publication);
    }
}
