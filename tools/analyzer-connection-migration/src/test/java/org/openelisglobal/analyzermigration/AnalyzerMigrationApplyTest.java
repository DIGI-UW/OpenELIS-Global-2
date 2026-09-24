package org.openelisglobal.analyzermigration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AnalyzerMigrationApplyTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    public void applyCreatesTheExactBridgeConnectionThenAttachesItsReferenceToOpenElis() throws Exception {
        ObjectNode profile = profile("fluorocycler-xt");
        ObjectNode sourceSnapshot = sourceSnapshot(profile);
        ObjectNode selections = selections(profile);
        List<ObjectNode> bridgeRequests = new ArrayList<>();
        List<ObjectNode> openElisAttachments = new ArrayList<>();
        AnalyzerMigrationRun.BridgeGateway bridge = request -> {
            bridgeRequests.add(request.deepCopy());
            ObjectNode response = JSON.createObjectNode();
            response.put("schemaVersion", "1.0");
            response.put("connectionId", "bridge-42");
            response.put("clientAnalyzerId", request.path("clientAnalyzerId").asText());
            response.set("profileRef", request.path("profileRef").deepCopy());
            response.put("configRevision", 1);
            return response;
        };
        AnalyzerMigrationRun.OpenElisGateway openElis = (sourceAnalyzerId, profileRef, bridgeConnectionId, actor) -> {
            ObjectNode attachment = JSON.createObjectNode();
            attachment.put("sourceAnalyzerId", sourceAnalyzerId);
            attachment.set("profileRef", profileRef.deepCopy());
            attachment.put("bridgeConnectionId", bridgeConnectionId);
            attachment.put("actor", actor);
            openElisAttachments.add(attachment);
        };

        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner(), bridge, openElis);
        ObjectNode manifest = run.apply("migration-run-apply", Instant.parse("2026-08-25T08:10:00Z"),
                Instant.parse("2026-08-25T08:10:02Z"), sourceSnapshot, selections, JSON.createArrayNode().add(profile));

        MigrationContractAssertions.assertManifestConforms(manifest);
        assertEquals("APPLY", manifest.path("mode").asText());
        assertEquals("MIGRATED", manifest.path("outcomes").path(0).path("outcome").asText());
        assertEquals("bridge-42", manifest.path("outcomes").path(0).path("bridgeConnectionId").asText());
        assertEquals(1, manifest.path("outcomes").path(0).path("bridgeConfigRevision").asInt());

        assertEquals(1, bridgeRequests.size());
        ObjectNode bridgeRequest = bridgeRequests.get(0);
        assertEquals("1.0", bridgeRequest.path("schemaVersion").asText());
        assertEquals("migration:oe-analyzer-42:aaaaaaaa", bridgeRequest.path("requestId").asText());
        assertEquals("oe-analyzer-42", bridgeRequest.path("clientAnalyzerId").asText());
        assertEquals("FluoroCycler XT", bridgeRequest.path("displayName").asText());
        assertEquals("/srv/analyzers/fluoro", bridgeRequest.path("values").path("directory").asText());
        assertFalse(bridgeRequest.path("values").has("fileFormat"));

        assertEquals(1, openElisAttachments.size());
        assertEquals("oe-analyzer-42", openElisAttachments.get(0).path("sourceAnalyzerId").asText());
        assertEquals("bridge-42", openElisAttachments.get(0).path("bridgeConnectionId").asText());
        assertEquals("migration-operator", openElisAttachments.get(0).path("actor").asText());
    }

    @Test
    public void applyDoesNotAttachBridgeEvidenceForAnotherAnalyzer() throws Exception {
        ObjectNode profile = profile("fluorocycler-xt");
        AnalyzerMigrationRun.BridgeGateway bridge = request -> {
            ObjectNode response = JSON.createObjectNode();
            response.put("schemaVersion", "1.0");
            response.put("connectionId", "bridge-wrong");
            response.put("clientAnalyzerId", "another-analyzer");
            response.set("profileRef", request.path("profileRef").deepCopy());
            response.put("configRevision", 1);
            return response;
        };
        List<String> attached = new ArrayList<>();
        AnalyzerMigrationRun.OpenElisGateway openElis = (sourceId, profileRef, connectionId, actor) -> attached
                .add(sourceId);

        AnalyzerMigrationRun run = new AnalyzerMigrationRun(new AnalyzerMigrationPlanner(), bridge, openElis);
        ObjectNode manifest = run.apply("migration-run-apply", Instant.parse("2026-08-25T08:10:00Z"),
                Instant.parse("2026-08-25T08:10:02Z"), sourceSnapshot(profile), selections(profile),
                JSON.createArrayNode().add(profile));

        MigrationContractAssertions.assertManifestConforms(manifest);
        JsonNode outcome = manifest.path("outcomes").path(0);
        assertEquals("NEEDS_CORRECTION", outcome.path("outcome").asText());
        assertEquals("BRIDGE_CONNECTION_EVIDENCE_MISMATCH", outcome.path("reasonCode").asText());
        assertEquals(0, attached.size());
    }

    private static ObjectNode sourceSnapshot(ObjectNode profile) {
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
        ObjectNode profileRef = selection.putObject("profileRef");
        profileRef.put("profileId", profile.path("profileMeta").path("id").asText());
        profileRef.put("revision", profile.path("catalog").path("revision").asInt());
        profileRef.put("fingerprint", profile.path("catalog").path("revisionFingerprint").asText());
        selection.put("selectedBy", "migration-operator");
        selection.put("selectedAt", "2026-08-25T07:55:00Z");
        selection.putObject("connectionValues");
        return selections;
    }

    private static ObjectNode profile(String profileId) throws Exception {
        Path path = Path.of("..", "openelis-analyzer-bridge", "src", "main", "resources", "analyzer-profiles",
                profileId + ".json");
        return (ObjectNode) JSON.readTree(Files.readString(path));
    }
}
