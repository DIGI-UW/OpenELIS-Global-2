package org.openelisglobal.analyzermigration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.Test;

public class AnalyzerMigrationRunTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    public void planReportsOneDeterministicOutcomeForEveryReleasedAnalyzer() throws Exception {
        ObjectNode fluoroProfile = profile("fluorocycler-xt");
        ObjectNode sourceSnapshot = JSON.createObjectNode();
        ArrayNode analyzers = sourceSnapshot.putArray("analyzers");
        analyzers.add(source("oe-analyzer-44", "c", "Excluded analyzer"));
        analyzers.add(source("oe-analyzer-42", "a", "FluoroCycler XT"));
        analyzers.add(source("oe-analyzer-43", "b", "Needs selection"));
        analyzers.get(1).withObject("configuration").put("importDirectory", "/srv/analyzers/fluoro")
                .put("filePattern", "*.{ods,ODS,xlsx,XLSX,xls,XLS}").put("fileFormat", "XLSX").put("hasHeader", true);

        ObjectNode selections = JSON.createObjectNode();
        selections.set("oe-analyzer-42", selection(fluoroProfile));
        selections.set("oe-analyzer-44", exclusion("DECOMMISSIONED_BEFORE_CUTOVER"));
        ArrayNode profiles = JSON.createArrayNode().add(fluoroProfile);

        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner());
        ObjectNode manifest = run.plan("migration-run-1", Instant.parse("2026-08-25T08:00:00Z"),
                Instant.parse("2026-08-25T08:00:02Z"), sourceSnapshot, selections, profiles);

        MigrationContractAssertions.assertManifestConforms(manifest);
        assertEquals("1.0", manifest.path("schemaVersion").asText());
        assertEquals("PLAN", manifest.path("mode").asText());
        assertTrue(manifest.path("sourceSnapshotFingerprint").asText().matches("sha256:[0-9a-f]{64}"));
        assertEquals(3, manifest.path("outcomes").size());

        JsonNode ready = manifest.path("outcomes").path(0);
        assertEquals("oe-analyzer-42:aaaaaaaa", ready.path("migrationKey").asText());
        assertEquals("READY", ready.path("outcome").asText());
        assertEquals("EXPLICIT", ready.path("profileSelection").path("method").asText());
        assertFalse(ready.has("connectionValues"));

        JsonNode correction = manifest.path("outcomes").path(1);
        assertEquals("NEEDS_CORRECTION", correction.path("outcome").asText());
        assertEquals("EXPLICIT_PROFILE_SELECTION_REQUIRED", correction.path("reasonCode").asText());

        JsonNode excluded = manifest.path("outcomes").path(2);
        assertEquals("INTENTIONALLY_EXCLUDED", excluded.path("outcome").asText());
        assertEquals("DECOMMISSIONED_BEFORE_CUTOVER", excluded.path("reasonCode").asText());
    }

    @Test
    public void planRejectsSelectionsWhoseProfileRevisionIsNotInTheSuppliedCatalog() throws Exception {
        ObjectNode sourceSnapshot = JSON.createObjectNode();
        sourceSnapshot.putArray("analyzers").add(source("oe-analyzer-42", "a", "FluoroCycler XT"));
        ObjectNode profile = profile("fluorocycler-xt");
        ObjectNode selections = JSON.createObjectNode();
        selections.set("oe-analyzer-42", selection(profile));

        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner());
        ObjectNode manifest = run.plan("migration-run-2", Instant.parse("2026-08-25T08:00:00Z"),
                Instant.parse("2026-08-25T08:00:01Z"), sourceSnapshot, selections, JSON.createArrayNode());

        MigrationContractAssertions.assertManifestConforms(manifest);
        assertEquals("NEEDS_CORRECTION", manifest.path("outcomes").path(0).path("outcome").asText());
        assertEquals("SELECTED_PROFILE_REVISION_NOT_FOUND",
                manifest.path("outcomes").path(0).path("reasonCode").asText());
    }

    @Test
    public void planRejectsDuplicateSourceAnalyzerIds() throws Exception {
        ObjectNode sourceSnapshot = JSON.createObjectNode();
        sourceSnapshot.putArray("analyzers").add(source("oe-analyzer-42", "a", "First"))
                .add(source("oe-analyzer-42", "b", "Duplicate"));
        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner());

        assertThrows(IllegalArgumentException.class,
                () -> run.plan("migration-run-3", Instant.parse("2026-08-25T08:00:00Z"),
                        Instant.parse("2026-08-25T08:00:01Z"), sourceSnapshot, JSON.createObjectNode(),
                        JSON.createArrayNode()));
    }

    @Test
    public void planTurnsAnEmptyExclusionReasonIntoCorrection() throws Exception {
        ObjectNode sourceSnapshot = JSON.createObjectNode();
        sourceSnapshot.putArray("analyzers").add(source("oe-analyzer-42", "a", "Excluded"));
        ObjectNode selections = JSON.createObjectNode();
        selections.putObject("oe-analyzer-42").put("method", "EXCLUDE");
        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner());

        ObjectNode manifest = run.plan("migration-run-4", Instant.parse("2026-08-25T08:00:00Z"),
                Instant.parse("2026-08-25T08:00:01Z"), sourceSnapshot, selections, JSON.createArrayNode());

        MigrationContractAssertions.assertManifestConforms(manifest);
        assertEquals("NEEDS_CORRECTION", manifest.path("outcomes").path(0).path("outcome").asText());
        assertEquals("EXCLUSION_REASON_REQUIRED", manifest.path("outcomes").path(0).path("reasonCode").asText());
    }

    private static ObjectNode source(String analyzerId, String fingerprintCharacter, String displayName) {
        ObjectNode source = JSON.createObjectNode();
        source.put("sourceAnalyzerId", analyzerId);
        source.put("sourceConfigFingerprint", "sha256:" + fingerprintCharacter.repeat(64));
        source.put("displayName", displayName);
        source.putObject("configuration");
        return source;
    }

    private static ObjectNode selection(ObjectNode profile) {
        ObjectNode selection = JSON.createObjectNode();
        selection.put("method", "EXPLICIT");
        ObjectNode profileRef = selection.putObject("profileRef");
        profileRef.put("profileId", profile.path("profileMeta").path("id").asText());
        profileRef.put("revision", profile.path("catalog").path("revision").asInt());
        profileRef.put("fingerprint", profile.path("catalog").path("revisionFingerprint").asText());
        selection.put("selectedBy", "migration-operator");
        selection.put("selectedAt", "2026-08-25T07:55:00Z");
        selection.putObject("connectionValues");
        return selection;
    }

    private static ObjectNode exclusion(String reasonCode) {
        ObjectNode exclusion = JSON.createObjectNode();
        exclusion.put("method", "EXCLUDE");
        exclusion.put("reasonCode", reasonCode);
        return exclusion;
    }

    private static ObjectNode profile(String profileId) throws Exception {
        Path path = Path.of("..", "openelis-analyzer-bridge", "src", "main", "resources", "analyzer-profiles",
                profileId + ".json");
        return (ObjectNode) JSON.readTree(Files.readString(path));
    }
}
