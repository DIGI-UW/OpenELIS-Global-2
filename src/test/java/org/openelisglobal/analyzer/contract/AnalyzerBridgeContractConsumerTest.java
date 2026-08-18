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

/** Consumer-side executable contract for Bridge-owned OGC-1054 v1 artifacts. */
public class AnalyzerBridgeContractConsumerTest {

    private static final Path CONTRACT_ROOT = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer",
            "v1");
    private static final Path FIXTURE_ROOT = CONTRACT_ROOT.resolve("fixtures");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMAS = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private static final FhirContext FHIR = FhirContext.forR4();
    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";
    private static final String RAW_CODE_SYSTEM = "https://openelis-global.org/fhir/CodeSystem/analyzer-raw-code";
    private static final String RAW_VALUE_EXTENSION = "https://openelis-global.org/fhir/StructureDefinition/analyzer-raw-value";
    private static final String RESULT_CLASSIFICATION_EXTENSION = "https://openelis-global.org/fhir/StructureDefinition/analyzer-result-classification";
    private static final String CONTROL_RECOGNITION_EXTENSION = "https://openelis-global.org/fhir/StructureDefinition/analyzer-control-recognition";

    @Test
    public void portableProfilesPinImmutableBridgeOwnedRecognition() throws IOException {
        JsonNode rules = fixture("portable-profile.json");
        JsonNode none = fixture("portable-profile-none.json");

        assertConforms("portable-profile.schema.json", rules);
        assertConforms("portable-profile.schema.json", none);
        assertTrue(rules.path("revisionFingerprint").asText().matches(FINGERPRINT_PATTERN));
        assertTrue(rules.path("controlResultRecognition").path("recognitionFingerprint").asText()
                .matches(FINGERPRINT_PATTERN));
        assertEquals("RULES", rules.path("controlResultRecognition").path("mode").asText());
        assertEquals("NONE", none.path("controlResultRecognition").path("mode").asText());
        assertTrue(none.path("controlResultRecognition").path("affirmedNoControlResults").asBoolean());
        assertFalse(rules.has("qcIdentification"));

        String schema = Files.readString(CONTRACT_ROOT.resolve("portable-profile.schema.json"));
        assertFalse(schema.contains("openelisTestId"));
        assertFalse(schema.contains("openelisResultOptionId"));
        assertFalse(schema.contains("labUnitId"));
    }

    @Test
    public void registrationAcknowledgesExactCandidateWithoutOpenElisQcState() throws IOException {
        JsonNode requested = fixture("registration-next.json");
        JsonNode result = fixture("registration-result.json");

        assertConforms("registration-sync.schema.json", requested);
        assertConforms("registration-sync-result.schema.json", result);

        JsonNode analyzer = requested.path("analyzers").path(0);
        JsonNode acknowledgement = result.path("registrations").path(0);
        assertTrue(analyzer.path("desiredStateFingerprint").asText().matches(FINGERPRINT_PATTERN));
        assertFalse(analyzer.has("siteBindingRevision"));
        assertFalse(analyzer.has("operationalQc"));
        assertFalse(analyzer.has("qcRules"));
        assertFalse(analyzer.has("controlLots"));
        assertEquals(analyzer.path("oeAnalyzerId"), acknowledgement.path("oeAnalyzerId"));
        assertEquals(analyzer.path("profileRef"), acknowledgement.path("profileRef"));
        assertEquals(analyzer.path("desiredStateFingerprint"), acknowledgement.path("desiredStateFingerprint"));
    }

    @Test
    public void normalizedTrafficIsStrictR4WithRawRecognitionEvidence() throws IOException {
        for (String name : new String[] { "normalized-known-test.fhir.json", "normalized-unknown-test.fhir.json",
                "normalized-unknown-value.fhir.json", "normalized-qc.fhir.json", "normalized-nonmatch.fhir.json",
                "normalized-none.fhir.json", "normalized-file.fhir.json" }) {
            JsonNode fixture = fixture(name);
            assertConforms("normalized-fhir-bundle.schema.json", fixture);

            Bundle bundle = FHIR.newJsonParser().setParserErrorHandler(new StrictErrorHandler())
                    .parseResource(Bundle.class, Files.readString(FIXTURE_ROOT.resolve(name)));
            assertEquals(Bundle.BundleType.TRANSACTION, bundle.getType());

            JsonNode observation = firstResource(fixture, "Observation");
            assertTrue(hasCodingSystem(observation, RAW_CODE_SYSTEM));
            assertTrue(hasExtension(observation, RAW_VALUE_EXTENSION));
            assertTrue(hasExtension(observation, RESULT_CLASSIFICATION_EXTENSION));
            assertTrue(hasExtension(observation, CONTROL_RECOGNITION_EXTENSION));
        }
    }

    @Test
    public void schemasRejectLocalOwnershipAndOperationalQcLeakage() throws IOException {
        JsonNode profile = fixture("portable-profile.json").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) profile.path("tests").path(0)).put("openelisTestId", "123");
        assertFalse(validationMessages("portable-profile.schema.json", profile).isEmpty());

        JsonNode registration = fixture("registration-initial.json").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) registration.path("analyzers").path(0)).set("operationalQc",
                JSON.createObjectNode());
        assertFalse(validationMessages("registration-sync.schema.json", registration).isEmpty());
    }

    private static void assertConforms(String schemaName, JsonNode fixture) throws IOException {
        Set<ValidationMessage> messages = validationMessages(schemaName, fixture);
        assertTrue(schemaName + " violations: " + messages, messages.isEmpty());
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
