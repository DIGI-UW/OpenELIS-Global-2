package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * Executable OE-E0 contract for reference-only persistence and one-time
 * migration.
 */
public class AnalyzerOwnershipContractTest {

    private static final Path ARTIFACT_ROOT = Path.of("specs", "015-ogc-1054-analyzer-contract-migration");
    private static final Path CONTRACT_ROOT = ARTIFACT_ROOT.resolve("contracts");
    private static final Path FIXTURE_ROOT = ARTIFACT_ROOT.resolve("fixtures");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMAS = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";

    @Test
    public void openElisAnalyzerStoresOnlyBridgeReferenceAndLimsOwnedState() throws Exception {
        JsonNode analyzer = localFixture("openelis-analyzer-reference.json");
        assertLocalConforms("openelis-analyzer-reference.schema.json", analyzer);

        assertFalse(analyzer.path("analyzerId").asText().isBlank());
        assertFalse(analyzer.path("bridgeConnectionId").asText().isBlank());
        assertTrue(analyzer.path("labUnitIds").size() > 0);
        assertTrue(analyzer.path("bindingRef").path("revision").asInt() > 0);
        assertTrue(analyzer.path("bindingRef").path("fingerprint").asText().matches(FINGERPRINT_PATTERN));
        assertFalse(analyzer.path("verification").path("actor").path("id").asText().isBlank());
        assertFalse(analyzer.path("verification").path("verifiedAt").asText().isBlank());
        assertEquals(analyzer.path("bridgeConnectionId"),
                analyzer.path("activation").path("acknowledgement").path("connectionId"));

    }

    @Test
    public void openElisAnalyzerContractRejectsUndeclaredState() throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode analyzer = (com.fasterxml.jackson.databind.node.ObjectNode) localFixture(
                "openelis-analyzer-reference.json").deepCopy();
        analyzer.put("undeclaredState", true);

        assertFalse(localValidationMessages("openelis-analyzer-reference.schema.json", analyzer).isEmpty());
    }

    @Test
    public void migrationPlanApplyAndVerifyCoverTheSameReleasedAnalyzers() throws Exception {
        JsonNode plan = migrationFixture("analyzer-migration-plan.json");
        JsonNode apply = migrationFixture("analyzer-migration-apply.json");
        JsonNode verify = migrationFixture("analyzer-migration-verify.json");

        assertEquals("PLAN", plan.path("mode").asText());
        assertEquals("APPLY", apply.path("mode").asText());
        assertEquals("VERIFY", verify.path("mode").asText());
        assertEquals(outcomeKeys(plan), outcomeKeys(apply));
        assertEquals(outcomeKeys(plan), outcomeKeys(verify));

        Map<String, JsonNode> planByKey = outcomesByKey(plan);
        Map<String, JsonNode> applyByKey = outcomesByKey(apply);
        Map<String, JsonNode> verifyByKey = outcomesByKey(verify);
        for (String migrationKey : planByKey.keySet()) {
            assertEquals(planByKey.get(migrationKey).path("sourceAnalyzerId"),
                    applyByKey.get(migrationKey).path("sourceAnalyzerId"));
            assertEquals(planByKey.get(migrationKey).path("sourceConfigFingerprint"),
                    applyByKey.get(migrationKey).path("sourceConfigFingerprint"));
            assertEquals(applyByKey.get(migrationKey).path("sourceConfigFingerprint"),
                    verifyByKey.get(migrationKey).path("sourceConfigFingerprint"));
        }
    }

    @Test
    public void finalMigrationHasOneExplicitOutcomeForEverySourceAnalyzer() throws Exception {
        JsonNode verify = migrationFixture("analyzer-migration-verify.json");
        Set<String> allowed = Set.of("MIGRATED", "NEEDS_CORRECTION", "INTENTIONALLY_EXCLUDED");
        Set<String> sourceIds = new LinkedHashSet<>();

        for (JsonNode outcome : verify.path("outcomes")) {
            assertTrue(allowed.contains(outcome.path("outcome").asText()));
            assertTrue("duplicate source analyzer", sourceIds.add(outcome.path("sourceAnalyzerId").asText()));
            if ("MIGRATED".equals(outcome.path("outcome").asText())) {
                assertEquals("EXPLICIT", outcome.path("profileSelection").path("method").asText());
                assertTrue(outcome.path("profileSelection").path("profileRef").path("revision").asInt() > 0);
                assertFalse(outcome.path("bridgeConnectionId").asText().isBlank());
            } else {
                assertFalse(outcome.path("reasonCode").asText().isBlank());
            }
        }
    }

    @Test
    public void migrationContractRejectsInferredProfileSelection() throws Exception {
        JsonNode plan = migrationFixture("analyzer-migration-plan.json").deepCopy();
        JsonNode selected = outcomesByKey(plan).values().stream().filter(outcome -> outcome.has("profileSelection"))
                .findFirst().orElseThrow();
        ((com.fasterxml.jackson.databind.node.ObjectNode) selected.path("profileSelection")).put("method",
                "INFERRED_BY_NAME");

        assertFalse(localValidationMessages("analyzer-migration-manifest.schema.json", plan).isEmpty());
    }

    @Test
    public void applyAndVerifyRejectPlanOnlyReadyOutcomes() throws Exception {
        for (String fixtureName : new String[] { "analyzer-migration-apply.json", "analyzer-migration-verify.json" }) {
            JsonNode manifest = migrationFixture(fixtureName).deepCopy();
            ((com.fasterxml.jackson.databind.node.ObjectNode) manifest.path("outcomes").path(0)).put("outcome",
                    "READY");

            assertFalse(fixtureName,
                    localValidationMessages("analyzer-migration-manifest.schema.json", manifest).isEmpty());
        }
    }

    private static JsonNode migrationFixture(String name) throws IOException {
        JsonNode fixture = localFixture(name);
        assertLocalConforms("analyzer-migration-manifest.schema.json", fixture);
        return fixture;
    }

    private static JsonNode localFixture(String name) throws IOException {
        return JSON.readTree(FIXTURE_ROOT.resolve(name).toFile());
    }

    private static void assertLocalConforms(String schemaName, JsonNode fixture) throws IOException {
        Set<ValidationMessage> messages = localValidationMessages(schemaName, fixture);
        assertTrue(schemaName + " violations: " + messages, messages.isEmpty());
    }

    private static Set<ValidationMessage> localValidationMessages(String schemaName, JsonNode fixture)
            throws IOException {
        JsonSchema schema = SCHEMAS.getSchema(JSON.readTree(CONTRACT_ROOT.resolve(schemaName).toFile()));
        return schema.validate(fixture);
    }

    private static Set<String> outcomeKeys(JsonNode manifest) {
        return outcomesByKey(manifest).keySet();
    }

    private static Map<String, JsonNode> outcomesByKey(JsonNode manifest) {
        Map<String, JsonNode> outcomes = new LinkedHashMap<>();
        for (JsonNode outcome : manifest.path("outcomes")) {
            JsonNode previous = outcomes.put(outcome.path("migrationKey").asText(), outcome);
            assertNotNull("migration key is required", outcome.path("migrationKey").textValue());
            assertTrue("duplicate migration key", previous == null);
        }
        return outcomes;
    }
}
