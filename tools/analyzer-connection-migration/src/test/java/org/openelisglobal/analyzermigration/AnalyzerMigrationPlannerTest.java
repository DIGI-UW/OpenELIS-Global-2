package org.openelisglobal.analyzermigration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class AnalyzerMigrationPlannerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final AnalyzerMigrationPlanner planner = new AnalyzerMigrationPlanner();

    @Test
    public void plansAFileConnectionFromReleasedValuesAndExplicitProfile() throws Exception {
        ObjectNode source = source("oe-analyzer-42");
        source.withObject("configuration").put("importDirectory", "/srv/analyzers/fluorocycler/incoming")
                .put("filePattern", "*.{ods,ODS,xlsx,XLSX,xls,XLS}").put("fileFormat", "XLSX").put("hasHeader", true);
        ObjectNode profile = profile("fluorocycler-xt");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection(profile), profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.READY, decision.outcome());
        assertEquals("/srv/analyzers/fluorocycler/incoming", decision.connectionValues().path("directory").asText());
        assertEquals("*.{ods,ODS,xlsx,XLSX,xls,XLS}", decision.connectionValues().path("filePattern").asText());
        assertTrue(decision.reasonCodes().isEmpty());
    }

    @Test
    public void requiresAnExplicitProfileSelection() throws Exception {
        ObjectNode source = source("oe-analyzer-43");
        ObjectNode profile = profile("genexpert-astm");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, null, profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.NEEDS_CORRECTION, decision.outcome());
        assertEquals("EXPLICIT_PROFILE_SELECTION_REQUIRED", decision.reasonCodes().get(0));
        assertTrue(decision.connectionValues().isEmpty());
    }

    @Test
    public void rejectsAReleasedValueHiddenByTheSelectedProfileDefaults() throws Exception {
        ObjectNode source = source("oe-analyzer-44");
        source.withObject("configuration").put("ipAddress", "10.42.0.7").put("port", 12000);
        ObjectNode profile = profile("genexpert-astm");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection(profile), profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.NEEDS_CORRECTION, decision.outcome());
        assertTrue(decision.reasonCodes().contains("SOURCE_VALUE_NOT_REPRESENTED:ipAddress"));
    }

    @Test
    public void appliesExplicitGenericOverridesWithoutInferringAnalyzerType() throws Exception {
        ObjectNode source = source("oe-analyzer-44");
        source.withObject("configuration").put("ipAddress", "10.42.0.7").put("port", 12000);
        ObjectNode profile = profile("genexpert-astm");
        ObjectNode selection = selection(profile);
        selection.withObject("connectionValues").put("connectionRole", "CLIENT");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection, profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.READY, decision.outcome());
        assertEquals("CLIENT", decision.connectionValues().path("connectionRole").asText());
        assertEquals("10.42.0.7", decision.connectionValues().path("host").asText());
        assertEquals(12000, decision.connectionValues().path("port").asInt());
        assertTrue(decision.reasonCodes().isEmpty());
    }

    @Test
    public void doesNotSilentlyDropAnUnrepresentableReleasedOverride() throws Exception {
        ObjectNode source = source("oe-analyzer-45");
        source.withObject("configuration").put("importDirectory", "/srv/analyzers/quantstudio")
                .put("filePattern", "*.csv").put("fileFormat", "CSV");
        ObjectNode profile = profile("quantstudio");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection(profile), profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.NEEDS_CORRECTION, decision.outcome());
        assertTrue(decision.reasonCodes().contains("SOURCE_VALUE_NOT_REPRESENTED:fileFormat"));
    }

    @Test
    public void plansAReleasedGeneXpertSerialConnectionAgainstProfileOwnedDefaults() throws Exception {
        ObjectNode source = source("oe-analyzer-46");
        source.withObject("configuration").put("transport", "RS-232").put("portName", "/dev/ttyUSB0")
                .put("baudRate", 9600).put("dataBits", 8).put("stopBits", "ONE").put("parity", "NONE")
                .put("flowControl", "NONE").put("protocolVersion", "ASTM_LIS2_A2").put("communicationMode", "BOTH")
                .put("identifierPattern", "GENEXPERT|CEPHEID");
        ObjectNode profile = profile("genexpert-astm");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection(profile), profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.READY, decision.outcome());
        assertEquals("RS-232", decision.connectionValues().path("transport").asText());
        assertEquals("/dev/ttyUSB0", decision.connectionValues().path("serialPort").asText());
        assertTrue(decision.reasonCodes().isEmpty());
    }

    @Test
    public void rejectsAReleasedSerialSettingThatDiffersFromTheSelectedProfile() throws Exception {
        ObjectNode source = source("oe-analyzer-47");
        source.withObject("configuration").put("transport", "RS-232").put("portName", "/dev/ttyUSB0").put("baudRate",
                19200);
        ObjectNode profile = profile("genexpert-astm");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection(profile), profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.NEEDS_CORRECTION, decision.outcome());
        assertTrue(decision.reasonCodes().contains("SOURCE_VALUE_NOT_REPRESENTED:baudRate"));
    }

    @Test
    public void reportsMalformedReleasedConfigurationFromTheFrozenExport() throws Exception {
        ObjectNode source = source("oe-analyzer-48");
        source.putArray("sourceErrors").add("COLUMN_MAPPINGS_INVALID_JSON");
        ObjectNode profile = profile("fluorocycler-xt");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection(profile), profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.NEEDS_CORRECTION, decision.outcome());
        assertTrue(decision.reasonCodes().contains("SOURCE_EXPORT_ERROR:COLUMN_MAPPINGS_INVALID_JSON"));
    }

    @Test
    public void requiresSelectionActorAndTimeForMigrationAudit() throws Exception {
        ObjectNode source = source("oe-analyzer-49");
        ObjectNode profile = profile("fluorocycler-xt");
        ObjectNode selection = selection(profile);
        selection.remove("selectedBy");

        AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection, profile);

        assertEquals(AnalyzerMigrationPlanner.Outcome.NEEDS_CORRECTION, decision.outcome());
        assertTrue(decision.reasonCodes().contains("SELECTION_AUDIT_REQUIRED"));
    }

    private static ObjectNode source(String analyzerId) {
        ObjectNode source = JSON.createObjectNode();
        source.put("sourceAnalyzerId", analyzerId);
        source.put("sourceConfigFingerprint", "sha256:" + "a".repeat(64));
        source.put("displayName", "Released analyzer " + analyzerId);
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
        selection.put("selectedAt", "2026-08-25T08:00:00Z");
        selection.putObject("connectionValues");
        return selection;
    }

    private static ObjectNode profile(String profileId) throws Exception {
        Path path = Path.of("..", "openelis-analyzer-bridge", "src", "main", "resources", "analyzer-profiles",
                profileId + ".json");
        return (ObjectNode) JSON.readTree(Files.readString(path));
    }
}
