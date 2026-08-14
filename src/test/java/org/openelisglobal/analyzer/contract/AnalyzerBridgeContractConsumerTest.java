package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.StrictErrorHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Test;

/**
 * Consumer-side executable contract for the Bridge-owned OGC-1054 v1 artifacts.
 */
public class AnalyzerBridgeContractConsumerTest {

    private static final Path CONTRACT_ROOT = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer",
            "v1");
    private static final Path FIXTURE_ROOT = CONTRACT_ROOT.resolve("fixtures");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMAS = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private static final FhirContext FHIR = FhirContext.forR4();

    @Test
    public void bridgeOwnedContractArtifactsArePinnedAndConsumable() throws IOException {
        assertTrue("Initialize and pin the Bridge submodule revision containing analyzer contract v1",
                Files.isDirectory(CONTRACT_ROOT));

        assertConforms("portable-profile.schema.json", "portable-profile.json");
        assertConforms("profile-catalog-entry.schema.json", "profile-catalog-entry.json");
        assertTrue(validationMessages("portable-profile.schema.json",
                fixture("profile-catalog-entry.json").path("profile")).isEmpty());
        assertConforms("registration-sync.schema.json", "registration-initial.json");
        assertConforms("registration-sync.schema.json", "registration-next.json");
        assertConforms("registration-sync-result.schema.json", "registration-result.json");
        assertConforms("legacy-registration.schema.json", "legacy-registration.json");
    }

    @Test
    public void portableProfileCannotOwnOpenElisCatalogBindings() throws IOException {
        String schema = Files.readString(CONTRACT_ROOT.resolve("portable-profile.schema.json"));

        assertFalse(schema.contains("openelisTestId"));
        assertFalse(schema.contains("openelisResultOptionId"));
        assertFalse(schema.contains("labUnitId"));

        JsonNode invalid = fixture("portable-profile.json").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.path("tests").path(0)).put("openelisTestId", "123");
        assertFalse(validationMessages("portable-profile.schema.json", invalid).isEmpty());
    }

    @Test
    public void registrationCarriesProfileAndSiteStateWithoutQcRuleDefinitions() throws IOException {
        JsonNode registration = fixture("registration-initial.json");
        JsonNode analyzer = registration.path("analyzers").path(0);

        assertEquals("1.0", registration.path("schemaVersion").asText());
        assertFalse(analyzer.path("profileRef").path("profileId").asText().isBlank());
        assertTrue(analyzer.path("profileRef").path("revision").asInt() > 0);
        assertFalse(analyzer.path("siteBindingRevision").asText().isBlank());
        assertTrue(analyzer.path("operationalQc").path("activeRuleIds").isArray());
        assertTrue(analyzer.path("operationalQc").path("controlLots").isArray());

        String schema = Files.readString(CONTRACT_ROOT.resolve("registration-sync.schema.json"));
        assertFalse(schema.contains("WESTGARD"));
        assertFalse(schema.contains("standardDeviation"));
    }

    @Test
    public void normalizedTrafficFixturesPreserveRawContextAndParseAsStrictR4() throws IOException {
        for (String name : new String[] { "normalized-known-test.fhir.json", "normalized-unknown-test.fhir.json",
                "normalized-unknown-value.fhir.json", "normalized-qc.fhir.json", "normalized-file.fhir.json" }) {
            assertConforms("normalized-fhir-bundle.schema.json", name);
            Bundle bundle = FHIR.newJsonParser().setParserErrorHandler(new StrictErrorHandler())
                    .parseResource(Bundle.class, Files.readString(FIXTURE_ROOT.resolve(name)));
            assertEquals(Bundle.BundleType.TRANSACTION, bundle.getType());

            JsonNode observation = firstResource(fixture(name), "Observation");
            assertTrue(hasCodingSystem(observation, "https://openelis-global.org/fhir/CodeSystem/analyzer-raw-code"));
            assertTrue(hasExtension(observation,
                    "https://openelis-global.org/fhir/StructureDefinition/analyzer-raw-value"));
            assertTrue(hasExtension(observation,
                    "https://openelis-global.org/fhir/StructureDefinition/analyzer-source-transport"));
        }
    }

    private static void assertConforms(String schemaName, String fixtureName) throws IOException {
        Set<ValidationMessage> messages = validationMessages(schemaName, fixture(fixtureName));
        assertTrue(fixtureName + " violates " + schemaName + ": " + messages, messages.isEmpty());
    }

    private static Set<ValidationMessage> validationMessages(String schemaName, JsonNode instance) throws IOException {
        JsonSchema schema = SCHEMAS.getSchema(JSON.readTree(CONTRACT_ROOT.resolve(schemaName).toFile()));
        return schema.validate(instance);
    }

    private static JsonNode fixture(String name) throws IOException {
        return JSON.readTree(FIXTURE_ROOT.resolve(name).toFile());
    }

    private static JsonNode firstResource(JsonNode bundle, String resourceType) {
        return StreamSupport.stream(bundle.path("entry").spliterator(), false).map(entry -> entry.path("resource"))
                .filter(resource -> resourceType.equals(resource.path("resourceType").asText())).findFirst()
                .orElseThrow();
    }

    private static boolean hasCodingSystem(JsonNode observation, String system) {
        return StreamSupport.stream(observation.path("code").path("coding").spliterator(), false)
                .anyMatch(coding -> system.equals(coding.path("system").asText()));
    }

    private static boolean hasExtension(JsonNode observation, String url) {
        return StreamSupport.stream(observation.path("extension").spliterator(), false)
                .anyMatch(extension -> url.equals(extension.path("url").asText()));
    }
}
