package org.openelisglobal.analyzermigration;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AnalyzerMigrationCliTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void planWritesAContractValidManifestFromExplicitFiles() throws Exception {
        Path directory = temporaryFolder.newFolder("migration").toPath();
        Path sourceFile = directory.resolve("source.json");
        Path selectionsFile = directory.resolve("selections.json");
        Path outputFile = directory.resolve("plan.json");
        ObjectNode profile = profile("fluorocycler-xt");
        ObjectNode source = JSON.createObjectNode();
        ObjectNode analyzer = source.putArray("analyzers").addObject();
        analyzer.put("sourceAnalyzerId", "42");
        analyzer.put("sourceConfigFingerprint", "sha256:" + "a".repeat(64));
        analyzer.put("displayName", "FluoroCycler XT");
        analyzer.withObject("configuration").put("importDirectory", "/srv/analyzers/fluoro")
                .put("filePattern", "*.{ods,ODS,xlsx,XLSX,xls,XLS}").put("fileFormat", "XLSX").put("hasHeader", true);
        ObjectNode selections = JSON.createObjectNode();
        ObjectNode selection = selections.putObject("42");
        selection.put("method", "EXPLICIT");
        ObjectNode profileRef = selection.putObject("profileRef");
        profileRef.put("profileId", profile.path("profileMeta").path("id").asText());
        profileRef.put("revision", profile.path("catalog").path("revision").asInt());
        profileRef.put("fingerprint", profile.path("catalog").path("revisionFingerprint").asText());
        selection.put("selectedBy", "migration-operator");
        selection.put("selectedAt", "2026-08-25T07:55:00Z");
        selection.putObject("connectionValues");
        JSON.writerWithDefaultPrettyPrinter().writeValue(sourceFile.toFile(), source);
        JSON.writerWithDefaultPrettyPrinter().writeValue(selectionsFile.toFile(), selections);

        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exitCode = new AnalyzerMigrationCli().run(
                new String[] { "plan", "--source", sourceFile.toString(), "--selections", selectionsFile.toString(),
                        "--profiles",
                        Path.of("..", "openelis-analyzer-bridge", "src", "main", "resources", "analyzer-profiles")
                                .toString(),
                        "--output", outputFile.toString(), "--run-id", "cli-plan-1" },
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(errors));

        assertEquals(errors.toString(StandardCharsets.UTF_8), 0, exitCode);
        ObjectNode manifest = (ObjectNode) JSON.readTree(outputFile.toFile());
        MigrationContractAssertions.assertManifestConforms(manifest);
        assertEquals("PLAN", manifest.path("mode").asText());
        assertEquals("READY", manifest.path("outcomes").path(0).path("outcome").asText());
    }

    @Test
    public void applyAndVerifyUseTheConfiguredGatewaysAndCookieFile() throws Exception {
        Path directory = temporaryFolder.newFolder("apply-verify").toPath();
        Path sourceFile = directory.resolve("source.json");
        Path selectionsFile = directory.resolve("selections.json");
        Path cookieFile = directory.resolve("oe.cookie");
        Path applyFile = directory.resolve("apply.json");
        Path verifyFile = directory.resolve("verify.json");
        ObjectNode profile = writeInputs(sourceFile, selectionsFile);
        Files.writeString(cookieFile, "JSESSIONID=cli-session\n");
        AtomicReference<String> configuredCookie = new AtomicReference<>();
        ObjectNode connection = connection(profile);
        AnalyzerMigrationRun.BridgeGateway bridge = new AnalyzerMigrationRun.BridgeGateway() {
            @Override
            public ObjectNode createConnection(ObjectNode request) {
                return connection.deepCopy();
            }

            @Override
            public ObjectNode getConnection(String connectionId) {
                return connection.deepCopy();
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
                reference.set("profileRef", connection.path("profileRef").deepCopy());
                return reference;
            }
        };
        AnalyzerMigrationCli cli = new AnalyzerMigrationCli((bridgeUrl, openElisUrl, cookie) -> {
            configuredCookie.set(cookie);
            return new AnalyzerMigrationCli.Gateways(bridge, openElis);
        });
        String profiles = Path.of("..", "openelis-analyzer-bridge", "src", "main", "resources", "analyzer-profiles")
                .toString();

        int applyExit = cli.run(
                new String[] { "apply", "--source", sourceFile.toString(), "--selections", selectionsFile.toString(),
                        "--profiles", profiles, "--output", applyFile.toString(), "--run-id", "cli-apply-1",
                        "--bridge-url", "http://bridge", "--openelis-url", "https://openelis/api",
                        "--openelis-cookie-file", cookieFile.toString() },
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));

        assertEquals(0, applyExit);
        assertEquals("JSESSIONID=cli-session", configuredCookie.get());
        ObjectNode apply = (ObjectNode) JSON.readTree(applyFile.toFile());
        MigrationContractAssertions.assertManifestConforms(apply);
        assertEquals("MIGRATED", apply.path("outcomes").path(0).path("outcome").asText());

        int verifyExit = cli.run(
                new String[] { "verify", "--source", sourceFile.toString(), "--apply-manifest", applyFile.toString(),
                        "--output", verifyFile.toString(), "--run-id", "cli-verify-1", "--bridge-url", "http://bridge",
                        "--openelis-url", "https://openelis/api", "--openelis-cookie-file", cookieFile.toString() },
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream()));

        assertEquals(0, verifyExit);
        ObjectNode verify = (ObjectNode) JSON.readTree(verifyFile.toFile());
        MigrationContractAssertions.assertManifestConforms(verify);
        assertEquals("VERIFY", verify.path("mode").asText());
        assertEquals("MIGRATED", verify.path("outcomes").path(0).path("outcome").asText());
    }

    private static ObjectNode writeInputs(Path sourceFile, Path selectionsFile) throws Exception {
        ObjectNode profile = profile("fluorocycler-xt");
        ObjectNode source = JSON.createObjectNode();
        ObjectNode analyzer = source.putArray("analyzers").addObject();
        analyzer.put("sourceAnalyzerId", "42");
        analyzer.put("sourceConfigFingerprint", "sha256:" + "a".repeat(64));
        analyzer.put("displayName", "FluoroCycler XT");
        analyzer.withObject("configuration").put("importDirectory", "/srv/analyzers/fluoro")
                .put("filePattern", "*.{ods,ODS,xlsx,XLSX,xls,XLS}").put("fileFormat", "XLSX").put("hasHeader", true);
        ObjectNode selections = JSON.createObjectNode();
        ObjectNode selection = selections.putObject("42");
        selection.put("method", "EXPLICIT");
        selection.set("profileRef", profileReference(profile));
        selection.put("selectedBy", "migration-operator");
        selection.put("selectedAt", "2026-08-25T07:55:00Z");
        selection.putObject("connectionValues");
        JSON.writerWithDefaultPrettyPrinter().writeValue(sourceFile.toFile(), source);
        JSON.writerWithDefaultPrettyPrinter().writeValue(selectionsFile.toFile(), selections);
        return profile;
    }

    private static ObjectNode connection(ObjectNode profile) {
        ObjectNode connection = JSON.createObjectNode();
        connection.put("schemaVersion", "1.0");
        connection.put("connectionId", "bridge-42");
        connection.put("clientAnalyzerId", "42");
        connection.set("profileRef", profileReference(profile));
        connection.put("configRevision", 1);
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
