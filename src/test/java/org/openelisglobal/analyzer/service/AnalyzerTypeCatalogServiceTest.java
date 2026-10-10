package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import org.openelisglobal.analyzer.dao.AnalyzerMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerTypeCatalogServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BridgeProfileCatalogService bridgeCatalogService;

    @Mock
    private AnalyzerMappingDAO mappingDAO;

    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;

    @Mock
    private TestResultService testResultService;

    private AnalyzerTypeCatalogService service;

    @Before
    public void setUp() {
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of(activeTest("100")));
        when(mappingCatalogService.getActiveResultOptions("100")).thenReturn(answers());
        when(testResultService.getActiveTestResultsByTest("100")).thenReturn(List.of(numericResult()));
        service = new AnalyzerTypeCatalogServiceImpl(bridgeCatalogService, mappingDAO,
                new AnalyzerMappingDefaults(mappingCatalogService, testResultService), mappingCatalogService);
    }

    @Test
    public void getCatalogComposesPortableMetadataWithLocalUsageAndHonestCompleteness() throws Exception {
        when(mappingDAO.findAnalyzersInForceOnProfile("site.mock-hematology")).thenReturn(
                List.of(analyzer("501", "Hematology - Main Lab", 3, 2), analyzer("502", "Hematology - Night Bench", 2, 1),
                        analyzer("503", "Hematology - Reference Lab", 1, 1)));
        when(mappingDAO.findAnalyzersInForceOnProfile("site.retired-file")).thenReturn(List.of());
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
        assertEquals("LIS2-A2", serializedActive.path("protocolVersion").asText());
        assertEquals("BOTH", serializedActive.path("communicationMode").asText());
        assertFalse(serializedActive.has("instanceDefaults"));
        assertFalse("A type no longer owns a mapping", serializedActive.has("mappingId"));
        assertEquals(2, active.testMappings().total());
        assertEquals(2, active.testMappings().mapped());
        assertEquals("COMPLETE", active.testMappings().state());
        assertEquals(2, active.resultMappings().total());
        assertEquals(2, active.resultMappings().mapped());
        assertEquals("COMPLETE", active.resultMappings().state());
        assertEquals(3L, active.usedBy());
        assertEquals(List.of("Hematology - Main Lab", "Hematology - Night Bench", "Hematology - Reference Lab"),
                active.affectedAnalyzers().stream().map(AnalyzerTypeCatalogView.AffectedAnalyzer::name).toList());
        assertEquals(List.of(false, true, true), active.affectedAnalyzers().stream()
                .map(AnalyzerTypeCatalogView.AffectedAnalyzer::newerProfileRevision).toList());
        assertEquals(List.of(false, false, false), active.affectedAnalyzers().stream()
                .map(AnalyzerTypeCatalogView.AffectedAnalyzer::newerMappingRevision).toList());
        assertEquals(List.of(3, 2, 1), active.affectedAnalyzers().stream()
                .map(AnalyzerTypeCatalogView.AffectedAnalyzer::pinnedProfileRevision).toList());
        assertEquals(List.of(2, 1, 1), active.affectedAnalyzers().stream()
                .map(AnalyzerTypeCatalogView.AffectedAnalyzer::pinnedMappingRevision).toList());
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
    public void getCatalogKeepsRowsTheCatalogCannotResolveVisibleAsIncompleteAttention() throws Exception {
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of(activeTest("100", "6690-2")));
        when(mappingDAO.findAnalyzersInForceOnProfile("site.mock-hematology"))
                .thenReturn(List.of(analyzer("501", "Hematology - Main Lab", 3, 1)));
        when(mappingDAO.findAnalyzersInForceOnProfile("site.retired-file")).thenReturn(List.of());
        when(bridgeCatalogService.getCatalog()).thenReturn(catalog());

        AnalyzerTypeCatalogView result = service.getCatalog();

        AnalyzerTypeCatalogView.TypeSummary active = result.types().get(0);
        assertEquals(1, result.summary().needsAttention());
        assertEquals(1, active.testMappings().mapped());
        assertEquals(2, active.testMappings().total());
        assertEquals("INCOMPLETE", active.testMappings().state());
        assertEquals(0, active.resultMappings().mapped());
        assertEquals(2, active.resultMappings().total());
        assertEquals("NOT_STARTED", active.resultMappings().state());
        assertEquals("NEEDS_LOCAL_MAPPING", active.readiness());
    }

    @Test
    public void getCatalogShowsTheProfilesTheBridgeSetAside() throws Exception {
        when(mappingDAO.findAnalyzersInForceOnProfile("site.mock-hematology")).thenReturn(List.of());
        when(mappingDAO.findAnalyzersInForceOnProfile("site.retired-file")).thenReturn(List.of());
        BridgeProfileCatalog loaded = catalog();
        List<BridgeProfileCatalog.CatalogIssue> issues = List
                .of(new BridgeProfileCatalog.CatalogIssue("file [/app/analyzer-profiles/broken.json]", "Unexpected character"));
        when(bridgeCatalogService.getCatalog()).thenReturn(new BridgeProfileCatalog(loaded.schemaVersion(),
                loaded.catalogFingerprint(), loaded.profiles(), issues));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of());

        AnalyzerTypeCatalogView view = service.getCatalog();

        assertEquals(2, view.types().size());
        assertEquals(issues, view.issues());
    }

    @Test
    public void getCatalogTreatsAProfileWithNoActiveTestsAsNotStarted() throws Exception {
        when(mappingDAO.findAnalyzersInForceOnProfile("site.mock-hematology"))
                .thenReturn(List.of(analyzer("501", "Hematology - Main Lab", 3, 1)));
        when(mappingDAO.findAnalyzersInForceOnProfile("site.retired-file")).thenReturn(List.of());
        when(bridgeCatalogService.getCatalog()).thenReturn(catalog());
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of());

        AnalyzerTypeCatalogView.TypeSummary active = service.getCatalog().types().get(0);

        assertEquals("NOT_STARTED", active.testMappings().state());
        assertEquals("NOT_STARTED", active.resultMappings().state());
        assertEquals("NEEDS_LOCAL_MAPPING", active.readiness());
    }

    @Test
    public void getCatalogTreatsATestThatCannotHoldTheProfilesAnswersAsIncomplete() throws Exception {
        when(mappingDAO.findAnalyzersInForceOnProfile("site.mock-hematology"))
                .thenReturn(List.of(analyzer("501", "Hematology - Main Lab", 3, 1)));
        when(mappingDAO.findAnalyzersInForceOnProfile("site.retired-file")).thenReturn(List.of());
        when(bridgeCatalogService.getCatalog()).thenReturn(catalog());
        when(mappingCatalogService.getActiveResultOptions("100")).thenReturn(List.of());

        AnalyzerTypeCatalogView.TypeSummary active = service.getCatalog().types().get(0);

        assertEquals("INCOMPLETE", active.testMappings().state());
        assertEquals("NOT_STARTED", active.resultMappings().state());
        assertEquals("NEEDS_LOCAL_MAPPING", active.readiness());
    }

    @Test
    public void getCatalogDoesNotChooseBetweenTwoActiveTestsWithTheSameLoinc() throws Exception {
        when(mappingCatalogService.searchActiveTests(null))
                .thenReturn(List.of(activeTest("100"), activeTest("101")));
        when(mappingCatalogService.getActiveResultOptions("101")).thenReturn(answers());
        when(testResultService.getActiveTestResultsByTest("101")).thenReturn(List.of(numericResult()));
        when(mappingDAO.findAnalyzersInForceOnProfile("site.mock-hematology")).thenReturn(List.of());
        when(mappingDAO.findAnalyzersInForceOnProfile("site.retired-file")).thenReturn(List.of());
        when(bridgeCatalogService.getCatalog()).thenReturn(catalog());

        AnalyzerTypeCatalogView.TypeSummary active = service.getCatalog().types().get(0);

        assertEquals("NOT_STARTED", active.testMappings().state());
        assertEquals("NEEDS_LOCAL_MAPPING", active.readiness());
    }

    @Test
    public void getTypeComposesTheExactRequestedRevisionInsteadOfTheLatestRevision() throws Exception {
        when(mappingDAO.findAnalyzersInForceOnProfile("site.mock-hematology"))
                .thenReturn(List.of(analyzer("501", "Hematology - Main Lab", 2, 1)));
        BridgeProfileCatalog.ProfileRevision revision = profileRevision(2, "Mock Hematology revision 2");
        when(bridgeCatalogService.getProfile("site.mock-hematology", 2)).thenReturn(revision);

        AnalyzerTypeCatalogView.TypeSummary result = service.getType("site.mock-hematology", 2);

        assertEquals("site.mock-hematology", result.profileId());
        assertEquals(2, result.revision());
        assertEquals("Mock Hematology revision 2", result.displayName());
        assertEquals("LIS2-A2", result.protocolVersion());
        assertEquals(1L, result.usedBy());
        assertEquals("Hematology - Main Lab", result.affectedAnalyzers().get(0).name());
        assertFalse(result.affectedAnalyzers().get(0).newerProfileRevision());
    }

    @Test
    public void aSavedRevisionNewerThanTheOneInForceIsReportedForVerification() throws Exception {
        when(mappingDAO.findAnalyzersInForceOnProfile("site.mock-hematology"))
                .thenReturn(List.of(analyzer("501", "Hematology - Main Lab", 2, 1)));
        when(mappingDAO.findLatestByAnalyzerId("501")).thenReturn(Optional.of(mapping(2)));
        when(bridgeCatalogService.getProfile("site.mock-hematology", 2))
                .thenReturn(profileRevision(2, "Mock Hematology revision 2"));

        AnalyzerTypeCatalogView.AffectedAnalyzer result = service.getType("site.mock-hematology", 2)
                .affectedAnalyzers().get(0);

        assertTrue(result.newerMappingRevision());
        assertFalse(result.newerProfileRevision());
    }

    private static AnalyzerMapping mapping(int revisionNumber) {
        AnalyzerMapping mapping = new AnalyzerMapping();
        mapping.setRevisionNumber(revisionNumber);
        return mapping;
    }

    private static TestResult numericResult() {
        TestResult numeric = new TestResult();
        numeric.setTestResultType("N");
        return numeric;
    }

    private static AnalyzerMappingCatalogService.TestOption activeTest(String id) {
        return activeTest(id, "6690-2", "58410-2");
    }

    private static AnalyzerMappingCatalogService.TestOption activeTest(String id, String... loincCodes) {
        return new AnalyzerMappingCatalogService.TestOption(id, "Mock active Test " + id, "MOCK", List.of(loincCodes));
    }

    private static List<AnalyzerMappingCatalogService.ResultOption> answers() {
        return List.of(new AnalyzerMappingCatalogService.ResultOption("200", "POS", "Positive", loinc("LA6576-8")),
                new AnalyzerMappingCatalogService.ResultOption("201", "NEG", "Negative", loinc("LA6577-6")));
    }

    private static Analyzer analyzer(String id, String name, int profileRevision, int mappingRevision) {
        AnalyzerMapping inForce = new AnalyzerMapping();
        inForce.setProfileId("site.mock-hematology");
        inForce.setProfileRevision(profileRevision);
        inForce.setRevisionNumber(mappingRevision);
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName(name);
        analyzer.setActive(true);
        analyzer.setMapping(inForce);
        return analyzer;
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
                            {"test_code":"FLAG","loinc":"58410-2","result_type":"qualitative","values":["POS","NEG"],"value_codes":{"POS":[{"system":"http://loinc.org","code":"LA6576-8"}],"NEG":[{"system":"http://loinc.org","code":"LA6577-6"}]}}
                          ],
                          "configDefaults":{"connectionRole":"SERVER","transport":"TCP/IP","port":9100,"aggregationMode":"PER_MESSAGE"},
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

    private BridgeProfileCatalog.ProfileRevision profileRevision(int revision, String displayName) throws Exception {
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
                          "configDefaults":{"connectionRole":"SERVER","transport":"TCP/IP","port":9200,"aggregationMode":"PER_MESSAGE"},
                          "catalog":{
                            "revision":%d,
                            "revisionFingerprint":"sha256:3333333333333333333333333333333333333333333333333333333333333333",
                            "source":"SITE",
                            "status":"ACTIVE"
                          }
                        }
                        """
                        .formatted(displayName, revision));
        JsonNode publication = objectMapper
                .readTree("{\"action\":\"PUBLISHED\",\"actor\":\"17\",\"markedAt\":\"2026-08-18T12:00:00Z\"}");
        return new BridgeProfileCatalog.ProfileRevision(profile, publication);
    }

    private static List<AnalyzerMappingCatalogService.AnswerCoding> loinc(String code) {
        return List.of(new AnalyzerMappingCatalogService.AnswerCoding("http://loinc.org", code));
    }
}
