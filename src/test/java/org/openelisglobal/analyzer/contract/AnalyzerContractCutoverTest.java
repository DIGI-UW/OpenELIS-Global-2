package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Executable OE-E0 contract for curation, clean consumption, and one-way
 * cutover.
 */
public class AnalyzerContractCutoverTest {

    private static final Path ARTIFACT_ROOT = Path.of("specs", "015-ogc-1054-analyzer-contract-migration");
    private static final Path CONTRACT_ROOT = ARTIFACT_ROOT.resolve("contracts");
    private static final Path FIXTURE_ROOT = ARTIFACT_ROOT.resolve("fixtures");
    private static final Path SOURCE_PROFILE_ROOT = Path.of("projects", "analyzer-profiles");
    private static final Path CURATION = ARTIFACT_ROOT.resolve("profile-curation-dispositions.json");
    private static final Path ADR = ARTIFACT_ROOT.resolve("adr-001-analyzer-profile-site-binding-boundary.md");
    private static final Path CUTOVER = ARTIFACT_ROOT.resolve("cutover-contract.md");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMAS = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    @Test
    public void everyEstablishedProfileHasOneEvidenceBackedDisposition() throws Exception {
        Map<String, String> sourceProfiles = sourceProfiles();
        assertEquals("the established corpus contains 20 profiles", 20, sourceProfiles.size());

        JsonNode document = JSON.readTree(CURATION.toFile());
        assertEquals("1.0", document.path("schemaVersion").asText());
        JsonNode dispositions = document.path("profiles");
        assertTrue(dispositions.isArray());
        assertEquals(sourceProfiles.size(), dispositions.size());

        Set<String> allowed = Set.of("RETAIN", "CORRECT", "ALIAS", "SPLIT", "REMOVE");
        Map<String, String> manifestedProfiles = new LinkedHashMap<>();
        Set<String> profileIds = new HashSet<>();
        for (JsonNode disposition : dispositions) {
            String sourcePath = disposition.path("sourcePath").asText();
            String profileId = disposition.path("profileId").asText();
            assertTrue("unknown disposition for " + profileId,
                    allowed.contains(disposition.path("disposition").asText()));
            assertTrue("missing evidence for " + profileId, disposition.path("evidence").size() > 0);
            assertTrue("missing explicit required changes for " + profileId,
                    disposition.path("requiredChanges").isArray());
            assertFalse("duplicate source path " + sourcePath, manifestedProfiles.put(sourcePath, profileId) != null);
            assertTrue("duplicate profile ID " + profileId, profileIds.add(profileId));
        }

        assertEquals(sourceProfiles, manifestedProfiles);
        String serialized = document.toString();
        assertFalse(serialized.contains("LEGACY_UNBOUND"));
        assertFalse(serialized.contains("PRESERVE_EVERY_ROW"));
        assertFalse(serialized.contains("MIGRATE_ANALYZER_QC_RULE"));
    }

    @Test
    public void cleanConsumerStoresAPinSiteStateVerificationAndExactCandidate() throws Exception {
        JsonNode candidate = JSON.readTree(FIXTURE_ROOT.resolve("openelis-analyzer-candidate.json").toFile());
        JsonSchema schema = SCHEMAS
                .getSchema(JSON.readTree(CONTRACT_ROOT.resolve("openelis-analyzer-candidate.schema.json").toFile()));
        Set<ValidationMessage> violations = schema.validate(candidate);
        assertTrue(violations.toString(), violations.isEmpty());

        assertFalse(candidate.path("profileRef").path("id").asText().isBlank());
        assertTrue(candidate.path("profileRef").path("revision").asInt() > 0);
        assertTrue(candidate.path("instance").path("labUnitIds").size() > 0);
        assertTrue(candidate.path("siteBinding").path("tests").isObject());
        assertFalse(candidate.path("verification").path("verifiedBy").asText().isBlank());
        assertFalse(candidate.path("verification").path("verifiedAt").asText().isBlank());
        assertFalse(candidate.path("desiredRegistrationFingerprint").asText().isBlank());

        String schemaText = Files.readString(CONTRACT_ROOT.resolve("openelis-analyzer-candidate.schema.json"));
        for (String forbidden : new String[] { "profileSnapshot", "profileDocument", "defaultConfigId",
                "AnalyzerQcRule", "qcRules", "controlLots", "westgard" }) {
            assertFalse(forbidden, schemaText.contains(forbidden));
        }
    }

    @Test
    public void cutoverDeletesSupersededPathsWithoutACompatibilityRuntime() throws Exception {
        String adr = Files.readString(ADR);
        String cutover = Files.readString(CUTOVER);

        assertTrue(adr.contains("**Status:** Accepted"));
        assertTrue(adr.contains("established Bridge-owned profile"));
        assertTrue(adr.contains("runtime communication"));
        assertTrue(adr.contains("instance defaults"));
        assertTrue(adr.contains("OpenELIS-owned site binding"));
        assertTrue(adr.contains("No copied profile authority"));

        for (String target : new String[] { "`defaultConfigId`", "copied plugin/profile JSON",
                "OE profile serving/application", "per-analyzer copied mappings", "`AnalyzerQcRule`",
                "raw analyzer import routes" }) {
            assertTrue("missing deletion target " + target, cutover.contains(target));
        }
        assertTrue(cutover.contains("No runtime adapter"));
        assertTrue(cutover.contains("No compatibility reader"));
        assertTrue(cutover.contains("No dual write"));

        for (String rejected : new String[] { "LEGACY_UNBOUND", "READ_LEGACY_WRITE_LEGACY",
                "DISCARD_AFTER_EXACT_PROFILE_PIN", "CREATE_SITE_PROFILE_BEFORE_PIN",
                "READY_AFTER_SITE_PROFILE_PUBLISH" }) {
            assertFalse(rejected, cutover.contains(rejected));
        }
    }

    private Map<String, String> sourceProfiles() throws IOException {
        Map<String, String> profiles = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.walk(SOURCE_PROFILE_ROOT, 2)) {
            paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> Set.of("astm", "hl7", "file").contains(path.getParent().getFileName().toString()))
                    .sorted().forEach(path -> {
                        try {
                            String profileId = JSON.readTree(path.toFile()).path("profileMeta").path("id").asText();
                            profiles.put(path.toString(), profileId);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
        return profiles;
    }
}
