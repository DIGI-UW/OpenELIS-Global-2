package org.openelisglobal.analyzermigration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class ReleasedPriorityAnalyzerMigrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    public void planApplyAndVerifyCoverAllThreePriorityReleasedShapes() throws Exception {
        ObjectNode source = (ObjectNode) JSON
                .readTree(Path.of("src", "test", "resources", "released-priority-analyzers.json").toFile());
        ArrayNode profiles = JSON.createArrayNode().add(profile("genexpert-astm")).add(profile("fluorocycler-xt"))
                .add(profile("quantstudio"));
        ObjectNode selections = selections(profiles);
        Map<String, ObjectNode> connections = new LinkedHashMap<>();
        Map<String, ObjectNode> connectionValues = new LinkedHashMap<>();
        Map<String, ObjectNode> references = new LinkedHashMap<>();
        AnalyzerMigrationRun.BridgeGateway bridge = new AnalyzerMigrationRun.BridgeGateway() {
            @Override
            public ObjectNode createConnection(ObjectNode request) {
                ObjectNode connection = JSON.createObjectNode();
                connection.put("schemaVersion", "1.0");
                connection.put("connectionId", "bridge-" + request.path("clientAnalyzerId").asText());
                connection.put("clientAnalyzerId", request.path("clientAnalyzerId").asText());
                connection.set("profileRef", request.path("profileRef").deepCopy());
                connection.put("configRevision", 1);
                connections.put(connection.path("connectionId").asText(), connection);
                connectionValues.put(connection.path("connectionId").asText(),
                        (ObjectNode) request.path("values").deepCopy());
                return connection.deepCopy();
            }

            @Override
            public ObjectNode getConnection(String connectionId) {
                return connections.get(connectionId).deepCopy();
            }
        };
        AnalyzerMigrationRun.OpenElisGateway openElis = new AnalyzerMigrationRun.OpenElisGateway() {
            @Override
            public void attachBridgeConnection(String sourceId, ObjectNode profileRef, String connectionId,
                    String actor) {
                ObjectNode reference = JSON.createObjectNode();
                reference.put("sourceAnalyzerId", sourceId);
                reference.put("bridgeConnectionId", connectionId);
                reference.set("profileRef", profileRef.deepCopy());
                references.put(sourceId, reference);
            }

            @Override
            public ObjectNode getAnalyzerReference(String sourceId) {
                return references.get(sourceId).deepCopy();
            }
        };
        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner(), bridge, openElis);

        ObjectNode plan = run.plan("priority-plan", Instant.parse("2026-08-25T09:00:00Z"),
                Instant.parse("2026-08-25T09:00:01Z"), source, selections, profiles);
        ObjectNode apply = run.apply("priority-apply", Instant.parse("2026-08-25T09:01:00Z"),
                Instant.parse("2026-08-25T09:01:01Z"), source, selections, profiles);
        ObjectNode verify = run.verify("priority-verify", Instant.parse("2026-08-25T09:02:00Z"),
                Instant.parse("2026-08-25T09:02:01Z"), source, apply);

        MigrationContractAssertions.assertManifestConforms(plan);
        MigrationContractAssertions.assertManifestConforms(apply);
        MigrationContractAssertions.assertManifestConforms(verify);
        assertOutcomes(plan, "READY");
        assertOutcomes(apply, "MIGRATED");
        assertOutcomes(verify, "MIGRATED");

        JsonNode geneXpert = connectionValues.get("bridge-42");
        assertEquals("RS-232", geneXpert.path("transport").asText());
        assertEquals("/dev/ttyUSB0", geneXpert.path("serialPort").asText());
        assertFalse(geneXpert.has("baudRate"));
        JsonNode fluoro = connectionValues.get("bridge-43");
        assertEquals("/srv/analyzers/fluorocycler", fluoro.path("directory").asText());
        assertFalse(fluoro.has("fileFormat"));
        JsonNode quantStudio = connectionValues.get("bridge-44");
        assertEquals("/srv/analyzers/quantstudio", quantStudio.path("directory").asText());
        assertFalse(quantStudio.has("fileFormat"));
    }

    private static ObjectNode selections(ArrayNode profiles) {
        ObjectNode selections = JSON.createObjectNode();
        addSelection(selections, "42", profiles.path(0));
        addSelection(selections, "43", profiles.path(1));
        addSelection(selections, "44", profiles.path(2));
        return selections;
    }

    private static void addSelection(ObjectNode selections, String sourceId, JsonNode profile) {
        ObjectNode selection = selections.putObject(sourceId);
        selection.put("method", "EXPLICIT");
        ObjectNode profileRef = selection.putObject("profileRef");
        profileRef.put("profileId", profile.path("profileMeta").path("id").asText());
        profileRef.put("revision", profile.path("catalog").path("revision").asInt());
        profileRef.put("fingerprint", profile.path("catalog").path("revisionFingerprint").asText());
        selection.put("selectedBy", "migration-operator");
        selection.put("selectedAt", "2026-08-25T08:55:00Z");
        selection.putObject("connectionValues");
    }

    private static void assertOutcomes(ObjectNode manifest, String expected) {
        assertEquals(3, manifest.path("outcomes").size());
        manifest.path("outcomes").forEach(outcome -> assertEquals(expected, outcome.path("outcome").asText()));
    }

    private static ObjectNode profile(String profileId) throws Exception {
        Path path = Path.of("..", "openelis-analyzer-bridge", "src", "main", "resources", "analyzer-profiles",
                profileId + ".json");
        return (ObjectNode) JSON.readTree(Files.readString(path));
    }
}
