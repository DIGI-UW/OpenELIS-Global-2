package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.service.AnalyzerPluginConfigService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.QualitativeResultMappingService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.analyzer.valueholder.QualitativeResultMapping;
import org.openelisglobal.analyzerimport.action.AnalyzerFhirImportController;
import org.openelisglobal.analyzerimport.action.AnalyzerImportController;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Executable inventory of the OpenELIS state that E0 must migrate without loss.
 */
public class AnalyzerMigrationCharacterizationTest extends BaseWebContextSensitiveTest {

    private static final String ANALYZER_ID = "9701";
    private static final String TEST_ID = "9701";
    private static final Path CHARACTERIZATION = Path.of("specs", "015-ogc-1054-analyzer-contract-migration",
            "fixtures", "current-state-characterization.json");
    private static final Path ADR = Path.of("specs", "015-ogc-1054-analyzer-contract-migration",
            "adr-001-analyzer-profile-site-binding-boundary.md");
    private static final Path MIGRATION_REPORT = Path.of("specs", "015-ogc-1054-analyzer-contract-migration",
            "migration-plan-and-anomaly-report.md");

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerPluginConfigService analyzerPluginConfigService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired
    private QualitativeResultMappingService qualitativeResultMappingService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultService testResultService;

    @Before
    public void loadCurrentState() throws Exception {
        executeDataSetWithStateManagement("testdata/ogc-1054-e0-migration.xml");
    }

    @Test
    public void defaultConfigIdIsOnlyATransientCreateHint() throws Exception {
        assertNotNull(AnalyzerForm.class.getDeclaredField("defaultConfigId"));
        assertFalse("analyzer instances do not persist the bootstrap profile file id",
                Arrays.stream(Analyzer.class.getDeclaredFields())
                        .anyMatch(field -> field.getName().equals("defaultConfigId")));
    }

    @Test
    public void existingAnalyzerRetainsInstanceStateAndLegacyInputsSeparately() throws Exception {
        Analyzer analyzer = analyzerService.get(ANALYZER_ID);
        assertNotNull(analyzer);
        assertEquals("E0 Existing Analyzer", analyzer.getName());
        assertEquals(List.of("9701", "9702"), analyzer.getTestUnitIds());

        JsonNode expectedConfig = new ObjectMapper()
                .readTree("{\"bootstrapMarker\":\"legacy-copy\",\"sourceAliases\":[\"WBC#\",\"WHITE_COUNT\"]}");
        String storedConfig = new JdbcTemplate(dataSource).queryForObject(
                "SELECT config::text FROM clinlims.analyzer_plugin_config WHERE analyzer_id = ?", String.class,
                Integer.valueOf(ANALYZER_ID));
        assertEquals("fixture preserves copied JSONB", expectedConfig, new ObjectMapper().readTree(storedConfig));

        Object serviceTarget = AopTestUtils.getTargetObject(analyzerPluginConfigService);
        assertEquals("the injected service is the production implementation", "AnalyzerPluginConfigServiceImpl",
                serviceTarget.getClass().getSimpleName());

        List<AnalyzerPluginConfig> explicitlyOrderedRows = analyzerPluginConfigService.getAllOrdered("analyzerId",
                false);
        assertEquals("criteria query resolves the copied config row by its mapped identifier", 1,
                explicitlyOrderedRows.size());

        AnalyzerPluginConfig configEntity = analyzerPluginConfigService.get(ANALYZER_ID);
        assertNotNull("Hibernate resolves copied config by analyzer id", configEntity);
        assertEquals("Hibernate JSONB type preserves copied JSON", expectedConfig,
                new ObjectMapper().readTree(configEntity.getConfig()));

        Map<String, Object> copiedConfig = analyzerPluginConfigService.getConfigAsMap(ANALYZER_ID);
        assertEquals("legacy-copy", copiedConfig.get("bootstrapMarker"));
        assertEquals(List.of("WBC#", "WHITE_COUNT"), copiedConfig.get("sourceAliases"));
        assertFalse("copied JSON has no durable versioned profile association", copiedConfig.containsKey("profileRef"));

        List<AnalyzerTestMapping> mappings = analyzerTestMappingService.getAllForAnalyzer(ANALYZER_ID);
        assertEquals("distinct source rows must not collapse when they bind to the same local Test", 2,
                mappings.size());
        assertEquals(List.of("WBC", "WBC_ALIAS"),
                mappings.stream().map(AnalyzerTestMapping::getAnalyzerTestName).sorted().toList());
        assertTrue(mappings.stream().allMatch(mapping -> TEST_ID.equals(mapping.getTestId())));

        List<QualitativeResultMapping> values = qualitativeResultMappingService
                .getMappingsByAnalyzerFieldId("field-e0");
        assertEquals(1, values.size());
        assertEquals("POS", values.get(0).getAnalyzerValue());
        assertEquals("POSITIVE", values.get(0).getOpenelisCode());
    }

    @Test
    public void activeCatalogResolutionDistinguishesZeroOneAndManyCandidates() {
        assertEquals(0, testService.getActiveTestsByLoinc("0000-0").size());
        assertEquals(1, testService.getActiveTestsByLoinc("1111-1").size());
        assertEquals(2, testService.getActiveTestsByLoinc("2222-2").size());
        assertEquals("inactive candidates do not count", 0, testService.getActiveTestsByLoinc("3333-3").size());

        List<TestResult> activeOptions = testResultService.getActiveTestResultsByTest(TEST_ID);
        assertEquals(1, activeOptions.size());
        assertEquals("9701", activeOptions.get(0).getId());
        assertTrue(activeOptions.get(0).getIsActive());
        assertFalse("legacy free text is not a catalog option identity",
                "POSITIVE".equals(activeOptions.get(0).getId()));
    }

    @Test
    public void ingressInventoryMarksRawRoutesDeprecatedAndFhirAsTarget() throws Exception {
        assertDeprecatedPostRoute(AnalyzerImportController.class, "doPost", "/analyzer/astm");
        assertDeprecatedPostRoute(AnalyzerImportController.class, "doPostHl7", "/analyzer/hl7");

        Method fhir = AnalyzerFhirImportController.class.getMethod("importFhirBundle",
                jakarta.servlet.http.HttpServletRequest.class, String.class);
        assertFalse(fhir.isAnnotationPresent(Deprecated.class));
        assertTrue(Arrays.asList(fhir.getAnnotation(PostMapping.class).value()).contains("/analyzer/fhir"));
    }

    @Test
    public void migrationCharacterizationNamesEveryRequiredInputAndCutover() throws Exception {
        JsonNode document = new ObjectMapper().readTree(Files.readString(CHARACTERIZATION));

        assertEquals("1.0", document.path("schemaVersion").asText());
        for (String key : List.of("defaultConfigId", "copiedPluginJson", "analyzerTestMap", "qualitativeValues",
                "existingAnalyzers", "rawIngress", "catalogCoverage")) {
            assertTrue("missing characterization for " + key, document.path("inputs").has(key));
        }
        assertEquals("BR-M1", document.path("cutovers").path("portableProfileWriter").asText());
        assertEquals("OE-M1", document.path("cutovers").path("siteBindingWriter").asText());
        assertEquals("BR-M4", document.path("cutovers").path("normalizedTrafficWriter").asText());
        assertEquals("GROUP_IDENTICAL_BY_FINGERPRINT_PRESERVE_DIVERGENCE_AS_FORKS",
                document.path("inputs").path("analyzerTestMap").path("migrationStrategy").asText());
        assertEquals("RESTORE_PRE_CUTOVER_BACKUP", document.path("rollback").path("databaseAction").asText());
        assertEquals("READ_LEGACY_WRITE_LEGACY", document.path("phases").path("beforeCutover").asText());
        assertEquals("READ_TARGET_WRITE_TARGET", document.path("phases").path("afterCutover").asText());
        assertTrue("migration anomalies must be explicit", document.path("anomalies").isArray());
        assertTrue("migration anomalies must cover every unsafe current state", document.path("anomalies").size() >= 6);
    }

    @Test
    public void acceptedAdrAndMigrationReportCloseTheEngineeringBoundary() throws Exception {
        assertTrue("E0 requires an accepted engineering ADR", Files.isRegularFile(ADR));
        assertTrue("E0 requires a migration and anomaly report", Files.isRegularFile(MIGRATION_REPORT));

        String adr = Files.readString(ADR);
        assertTrue(adr.contains("**Status:** Accepted"));
        assertTrue(adr.contains("Bridge-owned portable profile"));
        assertTrue(adr.contains("OpenELIS-owned site binding"));
        assertTrue(adr.contains("shared, revisioned site-binding aggregate"));
        assertTrue(adr.contains("Lab-facing Analyzer Type identity"));
        assertTrue(adr.contains("Bridge `profileId`"));
        assertTrue(adr.contains("administrator must select the Bridge profile and"));
        assertTrue(adr.contains("write endpoints reject all writes after the OE-M1 writer cutover"));
        assertFalse("per-analyzer legacy mappings cannot remain the reusable binding authority",
                adr.contains("existing `analyzer_test_map` is evolved in place"));
        assertTrue(adr.contains("No dual write"));

        String report = Files.readString(MIGRATION_REPORT);
        assertTrue(report.contains("## No-loss invariants"));
        assertTrue(report.contains("## One-writer cutovers"));
        assertTrue(report.contains("## Rollback"));
        assertTrue(report.contains("## Runtime characterization"));
    }

    private void assertDeprecatedPostRoute(Class<?> controller, String methodName, String route) {
        Method method = Arrays.stream(controller.getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .filter(candidate -> candidate.isAnnotationPresent(PostMapping.class))
                .filter(candidate -> Arrays.asList(candidate.getAnnotation(PostMapping.class).value()).contains(route))
                .findFirst().orElseThrow();
        assertTrue(route + " must remain deprecated during migration", method.isAnnotationPresent(Deprecated.class));
    }
}
