package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerTypeCatalogServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BridgeProfileCatalogService bridgeCatalogService;

    @Mock
    private AnalyzerProfileBindingDAO bindingDAO;

    @Mock
    private AnalyzerSiteBindingService siteBindingService;

    private AnalyzerTypeCatalogService service;

    @Before
    public void setUp() {
        service = new AnalyzerTypeCatalogServiceImpl(bridgeCatalogService, bindingDAO, siteBindingService);
    }

    @Test
    public void getCatalogComposesPortableMetadataWithLocalUsageAndHonestCompleteness() throws Exception {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("41");
        binding.setProfileId("site.mock-hematology");
        binding.setProfileRevision(3);
        AnalyzerProfileBinding previousBinding = new AnalyzerProfileBinding();
        previousBinding.setId("40");
        previousBinding.setProfileId("site.mock-hematology");
        previousBinding.setProfileRevision(2);
        when(bindingDAO.getAll()).thenReturn(List.of(previousBinding, binding));
        when(bindingDAO.countAnalyzersByBindingId("40")).thenReturn(4L);
        when(bindingDAO.countAnalyzersByBindingId("41")).thenReturn(3L);
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(siteBindingSnapshot(binding,
                List.of(AnalyzerSiteBindingMappingState.BOUND, AnalyzerSiteBindingMappingState.EXCLUDED),
                List.of(AnalyzerSiteBindingMappingState.BOUND, AnalyzerSiteBindingMappingState.EXCLUDED))));
        when(bridgeCatalogService.getCatalog()).thenReturn(catalog());

        AnalyzerTypeCatalogView result = service.getCatalog();

        assertEquals("1.0", result.schemaVersion());
        assertEquals(2, result.summary().total());
        assertEquals(1, result.summary().inUse());
        assertEquals(0, result.summary().needsAttention());
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
        JsonNode serializedActive = objectMapper.valueToTree(active);
        assertEquals("ASTM_LIS2_A2", serializedActive.at("/instanceDefaults/protocolVersion").asText());
        assertEquals("BOTH", serializedActive.at("/instanceDefaults/communicationMode").asText());
        assertEquals(9100, serializedActive.at("/instanceDefaults/port").asInt());
        assertEquals(2, active.testMappings().total());
        assertEquals(2, active.testMappings().mapped());
        assertEquals("COMPLETE", active.testMappings().state());
        assertEquals(2, active.resultMappings().total());
        assertEquals(2, active.resultMappings().mapped());
        assertEquals("COMPLETE", active.resultMappings().state());
        assertEquals("51", active.siteBindingId());
        assertEquals(7L, active.usedBy());
        assertEquals("READY", active.readiness());

        AnalyzerTypeCatalogView.TypeSummary inactive = result.types().get(1);
        assertEquals("site.retired-file", inactive.profileId());
        assertEquals("SITE", inactive.source());
        assertEquals("INACTIVE", inactive.status());
        assertEquals("DEACTIVATED", inactive.readiness());
        assertEquals("site.file-base", inactive.parentProfileId());
        assertEquals(Integer.valueOf(1), inactive.parentRevision());
        assertEquals("NOT_APPLICABLE", inactive.resultMappings().state());
        assertNull(active.parentProfileId());
    }

    @Test
    public void getCatalogKeepsUnresolvedLatestRowsVisibleAsIncompleteAttention() throws Exception {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("41");
        binding.setProfileId("site.mock-hematology");
        binding.setProfileRevision(3);
        when(bindingDAO.getAll()).thenReturn(List.of(binding));
        when(bindingDAO.countAnalyzersByBindingId("41")).thenReturn(1L);
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(siteBindingSnapshot(binding,
                List.of(AnalyzerSiteBindingMappingState.BOUND, AnalyzerSiteBindingMappingState.UNRESOLVED),
                List.of(AnalyzerSiteBindingMappingState.BOUND, AnalyzerSiteBindingMappingState.UNRESOLVED))));
        when(bridgeCatalogService.getCatalog()).thenReturn(catalog());

        AnalyzerTypeCatalogView result = service.getCatalog();

        AnalyzerTypeCatalogView.TypeSummary active = result.types().get(0);
        assertEquals(1, result.summary().needsAttention());
        assertEquals(1, active.testMappings().mapped());
        assertEquals(2, active.testMappings().total());
        assertEquals("INCOMPLETE", active.testMappings().state());
        assertEquals(1, active.resultMappings().mapped());
        assertEquals(2, active.resultMappings().total());
        assertEquals("INCOMPLETE", active.resultMappings().state());
        assertEquals("NEEDS_LOCAL_MAPPING", active.readiness());
    }

    @Test
    public void getTypeComposesTheExactRequestedRevisionInsteadOfTheLatestRevision() throws Exception {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("40");
        binding.setProfileId("site.mock-hematology");
        binding.setProfileRevision(2);
        when(bindingDAO.findByProfileIdAndRevision("site.mock-hematology", 2)).thenReturn(Optional.of(binding));
        when(bindingDAO.countAnalyzersByBindingId("40")).thenReturn(1L);
        when(siteBindingService.findCurrentByProfileBindingId("40")).thenReturn(Optional.empty());
        BridgeProfileCatalog.ProfileRevision revision = profileRevision(2, "Mock Hematology revision 2", 9200);
        when(bridgeCatalogService.getProfile("site.mock-hematology", 2)).thenReturn(revision);

        AnalyzerTypeCatalogView.TypeSummary result = service.getType("site.mock-hematology", 2);

        assertEquals("site.mock-hematology", result.profileId());
        assertEquals(2, result.revision());
        assertEquals("Mock Hematology revision 2", result.displayName());
        assertEquals(Integer.valueOf(9200), result.instanceDefaults().port());
        assertEquals(1L, result.usedBy());
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

    private static AnalyzerSiteBindingSnapshot siteBindingSnapshot(AnalyzerProfileBinding profileBinding,
            List<AnalyzerSiteBindingMappingState> testStates, List<AnalyzerSiteBindingMappingState> resultStates) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("51");
        binding.setProfileBinding(profileBinding);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("61");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(2);

        AnalyzerSiteBindingTest wbc = test(revision, "WBC", testStates.get(0));
        AnalyzerSiteBindingTest flag = test(revision, "FLAG", testStates.get(1));
        AnalyzerSiteBindingResult positive = result(revision, "FLAG", "POS", resultStates.get(0));
        AnalyzerSiteBindingResult negative = result(revision, "FLAG", "NEG", resultStates.get(1));
        return new AnalyzerSiteBindingSnapshot(binding, revision, List.of(wbc, flag), List.of(positive, negative));
    }

    private static AnalyzerSiteBindingTest test(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            AnalyzerSiteBindingMappingState state) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRowKey));
        row.setSiteBindingRevision(revision);
        row.setMappingState(state);
        if (state == AnalyzerSiteBindingMappingState.BOUND) {
            row.setTestId("100");
        }
        return row;
    }

    private static AnalyzerSiteBindingResult result(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            String rawValue, AnalyzerSiteBindingMappingState state) {
        AnalyzerSiteBindingResult row = new AnalyzerSiteBindingResult();
        row.setId(new AnalyzerSiteBindingResultPK(revision.getId(), sourceRowKey, rawValue));
        row.setSiteBindingRevision(revision);
        row.setMappingState(state);
        if (state == AnalyzerSiteBindingMappingState.BOUND) {
            row.setTestResultId("200");
        }
        return row;
    }
}
