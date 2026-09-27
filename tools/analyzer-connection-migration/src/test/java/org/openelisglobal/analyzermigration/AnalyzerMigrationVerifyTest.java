package org.openelisglobal.analyzermigration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.Test;

public class AnalyzerMigrationVerifyTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    public void verifyRequiresExactBridgeAndOpenElisReferences() throws Exception {
        ObjectNode profile = profile("fluorocycler-xt");
        ObjectNode sourceSnapshot = sourceSnapshot();
        ObjectNode selections = selections(profile);
        ObjectNode expectedConnection = connection(profile, "oe-analyzer-42", "bridge-42", 1);
        AnalyzerMigrationRun.BridgeGateway bridge = new AnalyzerMigrationRun.BridgeGateway() {
            @Override
            public ObjectNode createConnection(ObjectNode request) {
                return expectedConnection.deepCopy();
            }

            @Override
            public ObjectNode getConnection(String connectionId) {
                return expectedConnection.deepCopy();
            }
        };
        AnalyzerMigrationRun.OpenElisGateway openElis = new AnalyzerMigrationRun.OpenElisGateway() {
            @Override
            public void attachBridgeConnection(String sourceId, ObjectNode profileRef, String connectionId,
                    String actor) {
            }

            @Override
            public ObjectNode getAnalyzerReference(String sourceId) {
                ObjectNode reference = JSON.createObjectNode();
                reference.put("sourceAnalyzerId", sourceId);
                reference.put("bridgeConnectionId", "bridge-42");
                reference.set("profileRef", expectedConnection.path("profileRef").deepCopy());
                return reference;
            }
        };
        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner(), bridge, openElis);
        ObjectNode apply = run.apply("migration-apply", Instant.parse("2026-08-25T08:10:00Z"),
                Instant.parse("2026-08-25T08:10:02Z"), sourceSnapshot, selections, JSON.createArrayNode().add(profile));

        ObjectNode verify = run.verify("migration-verify", Instant.parse("2026-08-25T08:20:00Z"),
                Instant.parse("2026-08-25T08:20:02Z"), sourceSnapshot, apply);

        MigrationContractAssertions.assertManifestConforms(verify);
        assertEquals("VERIFY", verify.path("mode").asText());
        assertEquals("MIGRATED", verify.path("outcomes").path(0).path("outcome").asText());
        assertEquals("bridge-42", verify.path("outcomes").path(0).path("bridgeConnectionId").asText());
    }

    @Test
    public void verifyReportsAChangedOpenElisReference() throws Exception {
        ObjectNode profile = profile("fluorocycler-xt");
        ObjectNode expectedConnection = connection(profile, "oe-analyzer-42", "bridge-42", 1);
        AnalyzerMigrationRun.BridgeGateway bridge = new AnalyzerMigrationRun.BridgeGateway() {
            @Override
            public ObjectNode createConnection(ObjectNode request) {
                return expectedConnection.deepCopy();
            }

            @Override
            public ObjectNode getConnection(String connectionId) {
                return expectedConnection.deepCopy();
            }
        };
        AnalyzerMigrationRun.OpenElisGateway openElis = new AnalyzerMigrationRun.OpenElisGateway() {
            @Override
            public void attachBridgeConnection(String sourceId, ObjectNode profileRef, String connectionId,
                    String actor) {
            }

            @Override
            public ObjectNode getAnalyzerReference(String sourceId) {
                ObjectNode reference = JSON.createObjectNode();
                reference.put("sourceAnalyzerId", sourceId);
                reference.put("bridgeConnectionId", "another-connection");
                reference.set("profileRef", expectedConnection.path("profileRef").deepCopy());
                return reference;
            }
        };
        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner(), bridge, openElis);
        ObjectNode sourceSnapshot = sourceSnapshot();
        ObjectNode apply = run.apply("migration-apply", Instant.parse("2026-08-25T08:10:00Z"),
                Instant.parse("2026-08-25T08:10:02Z"), sourceSnapshot, selections(profile),
                JSON.createArrayNode().add(profile));

        ObjectNode verify = run.verify("migration-verify", Instant.parse("2026-08-25T08:20:00Z"),
                Instant.parse("2026-08-25T08:20:02Z"), sourceSnapshot, apply);

        MigrationContractAssertions.assertManifestConforms(verify);
        assertEquals("NEEDS_CORRECTION", verify.path("outcomes").path(0).path("outcome").asText());
        assertEquals("OPENELIS_REFERENCE_MISMATCH", verify.path("outcomes").path(0).path("reasonCode").asText());
    }

    @Test
    public void verifyRejectsAChangedFrozenSourceSnapshot() throws Exception {
        ObjectNode profile = profile("fluorocycler-xt");
        ObjectNode expectedConnection = connection(profile, "oe-analyzer-42", "bridge-42", 1);
        AnalyzerMigrationRun.BridgeGateway bridge = request -> expectedConnection.deepCopy();
        AnalyzerMigrationRun.OpenElisGateway openElis = (sourceId, profileRef, connectionId, actor) -> {
        };
        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner(), bridge, openElis);
        ObjectNode sourceSnapshot = sourceSnapshot();
        ObjectNode apply = run.apply("migration-apply", Instant.parse("2026-08-25T08:10:00Z"),
                Instant.parse("2026-08-25T08:10:02Z"), sourceSnapshot, selections(profile),
                JSON.createArrayNode().add(profile));
        sourceSnapshot.path("analyzers").path(0).withObject("configuration").put("importDirectory",
                "/srv/analyzers/changed");

        assertThrows(IllegalArgumentException.class, () -> run.verify("migration-verify",
                Instant.parse("2026-08-25T08:20:00Z"), Instant.parse("2026-08-25T08:20:02Z"), sourceSnapshot, apply));
    }

    private static ObjectNode sourceSnapshot() {
        ObjectNode snapshot = JSON.createObjectNode();
        ObjectNode source = snapshot.putArray("analyzers").addObject();
        source.put("sourceAnalyzerId", "oe-analyzer-42");
        source.put("sourceConfigFingerprint", "sha256:" + "a".repeat(64));
        source.put("displayName", "FluoroCycler XT");
        source.withObject("configuration").put("importDirectory", "/srv/analyzers/fluoro")
                .put("filePattern", "*.{ods,ODS,xlsx,XLSX,xls,XLS}").put("fileFormat", "XLSX").put("hasHeader", true);
        return snapshot;
    }

    private static ObjectNode selections(ObjectNode profile) {
        ObjectNode selections = JSON.createObjectNode();
        ObjectNode selection = selections.putObject("oe-analyzer-42");
        selection.put("method", "EXPLICIT");
        selection.set("profileRef", profileReference(profile));
        selection.put("selectedBy", "migration-operator");
        selection.put("selectedAt", "2026-08-25T07:55:00Z");
        selection.putObject("connectionValues");
        return selections;
    }

    private static ObjectNode connection(ObjectNode profile, String sourceId, String connectionId, int revision) {
        ObjectNode connection = JSON.createObjectNode();
        connection.put("schemaVersion", "1.0");
        connection.put("connectionId", connectionId);
        connection.put("clientAnalyzerId", sourceId);
        connection.set("profileRef", profileReference(profile));
        connection.put("configRevision", revision);
        return connection;
    }

    private static ObjectNode profileReference(ObjectNode profile) {
        ObjectNode profileRef = JSON.createObjectNode();
        profileRef.put("profileId", profile.path("profileMeta").path("id").asText());
        profileRef.put("revision", profile.path("catalog").path("revision").asInt());
        profileRef.put("fingerprint", profile.path("catalog").path("revisionFingerprint").asText());
        return profileRef;
    }

    private static ObjectNode profile(String profileId) throws Exception {
        Path path = Path.of("..", "openelis-analyzer-bridge", "src", "main", "resources", "analyzer-profiles",
                profileId + ".json");
        return (ObjectNode) JSON.readTree(Files.readString(path));
    }
}
