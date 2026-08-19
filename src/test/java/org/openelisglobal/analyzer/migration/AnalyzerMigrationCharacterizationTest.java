package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.form.AnalyzerForm;
import org.openelisglobal.analyzer.service.AnalyzerQcRuleService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.QualitativeResultMappingService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
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
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Executable inventory of representative persisted OpenELIS state that E0 must
 * migrate without loss.
 *
 * <p>
 * This proves the migration dispositions against a real PostgreSQL schema and
 * deterministic fixture. It does not perform the later M1-M4 production
 * migration.
 */
public class AnalyzerMigrationCharacterizationTest extends BaseWebContextSensitiveTest {

    private static final String EXACT_ANALYZER_ID = "9701";
    private static final String DIVERGENT_ANALYZER_ID = "9702";
    private static final String INVALID_ANALYZER_ID = "9703";
    private static final String TEST_ID = "9701";
    private static final Path ARTIFACT_ROOT = Path.of("specs", "015-ogc-1054-analyzer-contract-migration");
    private static final Path CHARACTERIZATION = ARTIFACT_ROOT.resolve("fixtures/current-state-characterization.json");
    private static final Path ADR = ARTIFACT_ROOT.resolve("adr-001-analyzer-profile-site-binding-boundary.md");
    private static final Path MIGRATION_CONTRACT = ARTIFACT_ROOT.resolve("migration-contract.md");
    private static final Path BRIDGE_CONTRACT_ROOT = Path.of("tools", "openelis-analyzer-bridge", "contracts",
            "analyzer", "v1");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMAS = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired
    private QualitativeResultMappingService qualitativeResultMappingService;

    @Autowired
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private DataSource dataSource;

    @Before
    public void loadCurrentState() throws Exception {
        executeDataSetWithStateManagement("testdata/ogc-1054-e0-migration.xml");
    }

    @Test
    public void legacyStoresRemainReadableWithoutPretendingTheyAreTargetAuthority() throws Exception {
        assertNotNull(AnalyzerForm.class.getDeclaredField("defaultConfigId"));
        assertFalse(Arrays.stream(Analyzer.class.getDeclaredFields()).map(Field::getName)
                .anyMatch("defaultConfigId"::equals));

        Analyzer analyzer = analyzerService.get(EXACT_ANALYZER_ID);
        assertNotNull(analyzer);
        assertEquals("E0 Exact Rules Analyzer", analyzer.getName());
        assertEquals(List.of("9701", "9702"), analyzer.getTestUnitIds());

        String storedConfig = new JdbcTemplate(dataSource).queryForObject(
                "SELECT config::text FROM clinlims.analyzer_plugin_config WHERE analyzer_id = ?", String.class,
                Integer.valueOf(EXACT_ANALYZER_ID));
        JsonNode copiedConfig = JSON.readTree(storedConfig);
        assertEquals("legacy-copy", copiedConfig.path("bootstrapMarker").asText());
        assertEquals(List.of("WBC#", "WHITE_COUNT"), StreamSupport
                .stream(copiedConfig.path("sourceAliases").spliterator(), false).map(JsonNode::asText).toList());
        assertFalse(copiedConfig.has("profileRef"));

        List<AnalyzerTestMapping> mappings = analyzerTestMappingService.getAllForAnalyzer(EXACT_ANALYZER_ID);
        assertEquals(2, mappings.size());
        assertEquals(List.of("WBC", "WBC_ALIAS"),
                mappings.stream().map(AnalyzerTestMapping::getAnalyzerTestName).sorted().toList());
        assertTrue(mappings.stream().allMatch(mapping -> TEST_ID.equals(mapping.getTestId())));

        List<QualitativeResultMapping> values = qualitativeResultMappingService
                .getMappingsByAnalyzerFieldId("field-e0");
        assertEquals(1, values.size());
        assertEquals("POSITIVE", values.get(0).getOpenelisCode());
        assertFalse("legacy display/code text is not a Result Option identity",
                values.get(0).getOpenelisCode().equals("9701"));
    }

    @Test
    public void catalogCoverageDistinguishesZeroOneManyInactiveAndMissingLoinc() {
        assertEquals(0, testService.getActiveTestsByLoinc("0000-0").size());
        assertEquals(1, testService.getActiveTestsByLoinc("1111-1").size());
        assertEquals(2, testService.getActiveTestsByLoinc("2222-2").size());
        assertEquals(0, testService.getActiveTestsByLoinc("3333-3").size());
        assertNull(testService.get("9705").getLoinc());

        List<TestResult> activeOptions = testResultService.getActiveTestResultsByTest(TEST_ID);
        assertEquals(1, activeOptions.size());
        assertEquals("9701", activeOptions.get(0).getId());
        assertTrue(activeOptions.get(0).getIsActive());
    }

    @Test
    public void ingressInventorySeparatesActiveLegacyReadersFromTheNormalizedTarget() throws Exception {
        assertPostRoute(AnalyzerImportController.class, "/importAnalyzer", false);
        assertPostRoute(AnalyzerImportController.class, "/analyzer/astm", true);
        assertPostRoute(AnalyzerImportController.class, "/analyzer/hl7", true);
        assertPostRoute(AnalyzerImportController.class, "/analyzer/runAction", false);
        assertPostRoute(AnalyzerFhirImportController.class, "/analyzer/fhir", false);
    }

    @Test
    public void everyPersistedQcRuleSetHasOneExplicitMigrationDisposition() throws Exception {
        JsonNode document = JSON.readTree(CHARACTERIZATION.toFile());
        JsonNode inventory = document.path("inputs").path("analyzerQcRule").path("inventory");
        assertTrue(inventory.isArray());

        Map<String, JsonNode> byAnalyzer = StreamSupport.stream(inventory.spliterator(), false)
                .collect(Collectors.toMap(node -> node.path("analyzerId").asText(), Function.identity()));
        assertEquals(Set.of(EXACT_ANALYZER_ID, DIVERGENT_ANALYZER_ID, INVALID_ANALYZER_ID), byAnalyzer.keySet());
        assertEquals("DISCARD_AFTER_EXACT_PROFILE_PIN", byAnalyzer.get(EXACT_ANALYZER_ID).path("disposition").asText());
        assertEquals("CREATE_SITE_PROFILE_BEFORE_PIN",
                byAnalyzer.get(DIVERGENT_ANALYZER_ID).path("disposition").asText());
        assertEquals("BLOCK_MIGRATION", byAnalyzer.get(INVALID_ANALYZER_ID).path("disposition").asText());

        Set<String> storedRuleIds = new LinkedHashSet<>(new JdbcTemplate(dataSource)
                .queryForList("SELECT id FROM clinlims.analyzer_qc_rule ORDER BY id", String.class));
        Set<String> inventoriedRuleIds = StreamSupport.stream(inventory.spliterator(), false)
                .flatMap(node -> StreamSupport.stream(node.path("persistedRuleIds").spliterator(), false))
                .map(JsonNode::asText).collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals("every persisted AnalyzerQcRule row is accounted for", storedRuleIds, inventoriedRuleIds);

        JsonNode selectedProfile = JSON
                .readTree(BRIDGE_CONTRACT_ROOT.resolve("fixtures/portable-profile.json").toFile());
        assertEquals(canonicalProfileRules(selectedProfile),
                canonicalLegacyRules(analyzerQcRuleService.getActiveRulesForAnalyzer(EXACT_ANALYZER_ID)));
        List<AnalyzerQcRule> divergentRules = analyzerQcRuleService.getActiveRulesForAnalyzer(DIVERGENT_ANALYZER_ID);
        assertFalse("the site-profile disposition requires actual semantic divergence",
                canonicalProfileRules(selectedProfile).equals(canonicalLegacyRules(divergentRules)));
        assertTrue(profileValidationMessages(profileWithLegacyRules(selectedProfile, divergentRules)).isEmpty());
        assertFalse(profileValidationMessages(profileWithLegacyRules(selectedProfile,
                analyzerQcRuleService.getActiveRulesForAnalyzer(INVALID_ANALYZER_ID))).isEmpty());
    }

    @Test
    public void migrationContractDefinesNoLossOneWriterCutoverAndRollback() throws Exception {
        JsonNode document = JSON.readTree(CHARACTERIZATION.toFile());
        assertEquals("1.0", document.path("schemaVersion").asText());
        for (String key : List.of("defaultConfigId", "copiedPluginJson", "analyzerTestMap", "qualitativeValues",
                "existingAnalyzers", "analyzerQcRule", "rawIngress", "catalogCoverage")) {
            assertTrue("missing characterization for " + key, document.path("inputs").has(key));
        }
        assertEquals("BR-M1", document.path("cutovers").path("portableProfileWriter").asText());
        assertEquals("OE-M1", document.path("cutovers").path("siteBindingWriter").asText());
        assertEquals("BR-M2", document.path("cutovers").path("controlRecognitionRuntime").asText());
        assertEquals("OE-M2", document.path("cutovers").path("legacyClassifierWriterDisabled").asText());
        assertEquals("OE-M4", document.path("cutovers").path("legacyClassifierSchemaRemoved").asText());
        assertEquals("BR-M4", document.path("cutovers").path("normalizedTrafficWriter").asText());
        assertEquals("READ_LEGACY_WRITE_LEGACY", document.path("phases").path("beforeCutover").asText());
        assertEquals("READ_TARGET_WRITE_TARGET", document.path("phases").path("afterCutover").asText());
        assertEquals("RESTORE_PRE_CUTOVER_BACKUP", document.path("rollback").path("databaseAction").asText());
        assertEquals("RESTORE_PRE_CUTOVER_PROFILE_CATALOG",
                document.path("rollback").path("profileCatalogAction").asText());
        assertTrue(document.path("anomalies").isArray());
        assertTrue(document.path("anomalies").size() >= 8);
    }

    @Test
    public void adrAndMigrationContractCloseTheBoundaryWithoutASecondLedger() throws Exception {
        assertTrue(Files.isRegularFile(ADR));
        assertTrue(Files.isRegularFile(MIGRATION_CONTRACT));

        String adr = Files.readString(ADR);
        assertTrue(adr.contains("**Status:** Accepted"));
        assertTrue(adr.contains("Bridge-owned portable profile"));
        assertTrue(adr.contains("OpenELIS-owned site binding"));
        assertTrue(adr.contains("immutable"));
        assertTrue(adr.contains("revision-scoped"));
        assertTrue(adr.contains("desired-state fingerprint"));
        assertTrue(adr.contains("No dual write"));
        assertTrue(adr.contains("Operational QC"));
        assertTrue(adr.contains("AnalyzerQcRule"));

        String migration = Files.readString(MIGRATION_CONTRACT);
        assertTrue(migration.contains("## No-loss invariants"));
        assertTrue(migration.contains("## Preflight outcomes"));
        assertTrue(migration.contains("## One-writer cutovers"));
        assertTrue(migration.contains("## Rollback"));
        assertTrue(migration.contains("## Runtime removal"));
        assertFalse(migration.contains("checkpoint-evidence"));
        assertFalse(migration.contains("branch head"));
    }

    private void assertPostRoute(Class<?> controller, String route, boolean deprecated) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.isAnnotationPresent(PostMapping.class))
                .filter(candidate -> Arrays.asList(candidate.getAnnotation(PostMapping.class).value()).contains(route))
                .findFirst().orElseThrow();
        assertEquals(route + " deprecation state", deprecated, method.isAnnotationPresent(Deprecated.class));
    }

    private Set<String> profileValidationMessages(JsonNode profile) throws Exception {
        JsonSchema schema = SCHEMAS
                .getSchema(JSON.readTree(BRIDGE_CONTRACT_ROOT.resolve("portable-profile.schema.json").toFile()));
        return schema.validate(profile).stream().map(Object::toString).collect(Collectors.toSet());
    }

    private JsonNode profileWithLegacyRules(JsonNode selectedProfile, List<AnalyzerQcRule> legacyRules) {
        ObjectNode candidate = selectedProfile.deepCopy();
        ObjectNode rules = JSON.createObjectNode();
        for (AnalyzerQcRule rule : legacyRules) {
            ObjectNode target = JSON.createObjectNode();
            target.put("ruleType", rule.getRuleType().name());
            if (rule.getTargetField() != null && !rule.getTargetField().isBlank()) {
                target.put("targetField", rule.getTargetField());
            }
            target.put("operand", rule.getOperand());
            rules.set("legacy-" + rule.getId(), target);
        }
        ((ObjectNode) candidate.path("controlResultRecognition")).set("rules", rules);
        return candidate;
    }

    private List<String> canonicalLegacyRules(List<AnalyzerQcRule> rules) {
        return rules.stream()
                .map(rule -> canonicalRule(rule.getRuleType().name(), rule.getTargetField(), rule.getOperand()))
                .sorted().toList();
    }

    private List<String> canonicalProfileRules(JsonNode profile) {
        return StreamSupport.stream(profile.path("controlResultRecognition").path("rules").spliterator(), false)
                .map(rule -> canonicalRule(rule.path("ruleType").asText(),
                        rule.has("targetField") ? rule.path("targetField").asText() : null,
                        rule.path("operand").asText()))
                .sorted().toList();
    }

    private String canonicalRule(String type, String field, String operand) {
        return type + "|" + (field == null ? "" : field) + "|" + operand;
    }
}
