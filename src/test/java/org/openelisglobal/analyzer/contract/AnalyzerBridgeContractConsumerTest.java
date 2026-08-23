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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Test;
import org.openelisglobal.analyzer.service.BridgeAnalyzerProfile;

/**
 * Consumer-side executable contract for Bridge-owned OGC-1054 v1 artifacts.
 *
 * <p>
 * This validates schemas and representative profile-data fixtures. It does not
 * claim that the current Bridge runtime emits them; BR-M1, BR-M2, and BR-M4 own
 * that production conformance.
 */
public class AnalyzerBridgeContractConsumerTest {

    private static final Path CONTRACT_ROOT = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer",
            "v1");
    private static final Path FIXTURE_ROOT = CONTRACT_ROOT.resolve("fixtures");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMAS = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
            builder -> builder
                    .schemaMappers(mappers -> mappers.mapPrefix("https://openelis-global.org/contracts/analyzer/v1/",
                            CONTRACT_ROOT.toAbsolutePath().toUri().toString())));
    private static final FhirContext FHIR = FhirContext.forR4();
    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";
    private static final String RAW_CODE_SYSTEM = "https://openelis-global.org/fhir/CodeSystem/analyzer-raw-code";
    private static final String RAW_VALUE_EXTENSION = "https://openelis-global.org/fhir/StructureDefinition/analyzer-raw-value";
    private static final String RESULT_CLASSIFICATION_EXTENSION = "https://openelis-global.org/fhir/StructureDefinition/analyzer-result-classification";
    private static final String CONTROL_RECOGNITION_EXTENSION = "https://openelis-global.org/fhir/StructureDefinition/analyzer-control-recognition";

    @Test
    public void establishedProfilesRetainRuntimeCommunicationAndInstanceDefaults() throws IOException {
        JsonNode socketProfile = fixture("analyzer-profile-astm.json");
        JsonNode fileProfile = fixture("analyzer-profile-file.json");

        assertConforms("analyzer-profile.schema.json", socketProfile);
        assertConforms("analyzer-profile.schema.json", fileProfile);
        for (JsonNode profile : new JsonNode[] { socketProfile, fileProfile }) {
            assertTrue(profile.path("catalog").path("revisionFingerprint").asText().matches(FINGERPRINT_PATTERN));
            assertTrue(profile.path("catalog").path("recognitionFingerprint").asText().matches(FINGERPRINT_PATTERN));
            assertFalse(profile.path("protocol").path("name").asText().isBlank());
            assertTrue(profile.path("configDefaults").isObject());
            assertTrue(profile.path("configDefaults").size() > 0);
            assertFalse(profile.has("qcIdentification"));
        }
        assertTrue(socketProfile.path("communication").isObject());
        assertTrue(socketProfile.path("transport").size() > 0);
        assertTrue(socketProfile.path("default_test_mappings").size() > 0);
        assertTrue(fileProfile.path("supported_extensions").size() > 0);
        assertTrue(fileProfile.path("column_mapping").size() > 0);

        String schema = Files.readString(CONTRACT_ROOT.resolve("analyzer-profile.schema.json"));
        assertFalse(schema.contains("openelisTestId"));
        assertFalse(schema.contains("openelisResultOptionId"));
        assertFalse(schema.contains("labUnitId"));
    }

    @Test
    public void priorityProfilesExposeOnlyMockProvenDefaultMappings() throws IOException {
        JsonNode geneXpert = fixture("analyzer-profile-astm.json");
        JsonNode fluoroCycler = fixture("analyzer-profile-file.json");
        JsonNode quantStudio = fixture("analyzer-profile-quantstudio.json");

        assertMappingCodes(geneXpert, Set.of("MTB-RIF", "RIF", "HIV-VL", "COVID19"));
        assertMappingValues(geneXpert, "MTB-RIF", Set.of("MTB DETECTED", "NOT DETECTED", "HIGH", "MEDIUM", "LOW",
                "VERY LOW", "INDETERMINATE", "ERROR", "NO RESULT"));
        assertMappingValues(geneXpert, "RIF", Set.of("DETECTED", "NOT DETECTED", "INDETERMINATE"));
        assertMappingValues(geneXpert, "COVID19", Set.of("POSITIVE", "NEGATIVE", "INDETERMINATE", "ERROR"));
        assertMappingCodes(fluoroCycler, Set.of("VIH-1"));
        assertMappingCodes(quantStudio, Set.of("VIH-1", "IC"));
    }

    @Test
    public void priorityProfileValuesRemainOpaqueToOpenElis() throws IOException {
        BridgeAnalyzerProfile geneXpert = BridgeAnalyzerProfile.from(fixture("analyzer-profile-astm.json"));

        assertEquals("E-1394-97", geneXpert.protocolVersion());
        assertEquals("BOTH", geneXpert.communicationMode());
        assertEquals("TCP/IP", geneXpert.instanceDefaults().transport());
    }

    @Test
    public void profileCatalogComposesImmutablePortableProfileRevisions() throws IOException {
        JsonNode response = fixture("profile-catalog-response.json");

        assertConforms("profile-catalog-response.schema.json", response);
        assertEquals("1.0", response.path("schemaVersion").asText());
        assertTrue(response.path("catalogFingerprint").asText().matches(FINGERPRINT_PATTERN));

        JsonNode entry = response.path("profiles").path(0);
        assertEquals("SHIPPED", entry.path("publication").path("action").asText());
        assertEquals("fluorocycler-xt", entry.path("profile").path("profileMeta").path("id").asText());
        assertTrue(entry.path("profile").path("catalog").path("revisionFingerprint").asText()
                .matches(FINGERPRINT_PATTERN));
        assertFalse(entry.path("profile").has("openelisTestId"));
        assertFalse(entry.path("profile").has("qcIdentification"));
    }

    @Test
    public void durableConnectionContractsExposeGenericFieldsAndOptimisticConcurrency() throws IOException {
        JsonNode create = fixture("connection-create.json");
        JsonNode update = fixture("connection-update.json");
        JsonNode connection = fixture("analyzer-connection.json");

        assertConforms("connection-create.schema.json", create);
        assertConforms("connection-update.schema.json", update);
        assertConforms("analyzer-connection.schema.json", connection);

        assertEquals(create.path("clientAnalyzerId"), connection.path("clientAnalyzerId"));
        assertEquals(create.path("profileRef"), connection.path("profileRef"));
        assertEquals(update.path("expectedConfigRevision").asInt() + 1, connection.path("configRevision").asInt());
        assertTrue(connection.path("configFingerprint").asText().matches(FINGERPRINT_PATTERN));
        assertTrue(connection.path("fields").isArray());
        assertTrue(connection.path("fields").size() > 0);
        assertFalse(connection.has("connection"));
        assertFalse(connection.has("settings"));
    }

    @Test
    public void probeAndActivationAcknowledgeTheExactSavedConnection() throws IOException {
        JsonNode probeRequest = fixture("connection-probe-request.json");
        JsonNode probeResult = fixture("connection-probe-result.json");
        JsonNode command = fixture("connection-activate.json");
        JsonNode acknowledgement = fixture("connection-activate-ack.json");

        assertConforms("connection-probe-request.schema.json", probeRequest);
        assertConforms("connection-probe-result.schema.json", probeResult);
        assertConforms("connection-runtime-command.schema.json", command);
        assertConforms("connection-runtime-ack.schema.json", acknowledgement);

        assertEquals(probeRequest.path("requestId"), probeResult.path("requestId"));
        assertEquals(probeRequest.path("connectionId"), probeResult.path("connectionId"));
        assertTrue(probeResult.path("nonMutating").asBoolean());
        assertEquals(command.path("commandId"), acknowledgement.path("commandId"));
        assertEquals(command.path("connectionId"), acknowledgement.path("connectionId"));
        assertEquals(command.path("expectedConfigRevision"), acknowledgement.path("configRevision"));
        assertEquals("ACTIVE", acknowledgement.path("actualRuntimeState").asText());
        assertTrue(acknowledgement.path("runtimeFingerprint").asText().matches(FINGERPRINT_PATTERN));
    }

    @Test
    public void connectionProbeEvidenceIdentifiesTheExactRegisteredCandidate() throws IOException {
        JsonNode result = fixture("connection-probe-result.json");

        assertConforms("connection-probe-result.schema.json", result);
        assertEquals("77", result.path("analyzerId").asText());
        assertEquals("genexpert-astm", result.path("profileRef").path("profileId").asText());
        assertEquals(1, result.path("profileRef").path("revision").asInt());
        assertTrue(result.path("desiredStateFingerprint").asText().matches(FINGERPRINT_PATTERN));
        assertEquals("RECEIVER", result.path("connection").path("role").asText());
        assertEquals("TWO_WAY", result.path("dataFlow").asText());
        assertEquals("TIMEOUT", result.path("outcome").asText());
        assertTrue(result.path("resultsOnlyAvailable").asBoolean());
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
        JsonNode profile = fixture("analyzer-profile-astm.json").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) profile).put("labUnitId", "123");
        assertFalse(validationMessages("analyzer-profile.schema.json", profile).isEmpty());

        JsonNode connection = fixture("connection-create.json").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) connection).set("operationalQc", JSON.createObjectNode());
        assertFalse(validationMessages("connection-create.schema.json", connection).isEmpty());
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

    private static void assertMappingCodes(JsonNode profile, Set<String> expectedCodes) {
        Set<String> actualCodes = StreamSupport.stream(profile.path("default_test_mappings").spliterator(), false)
                .map(mapping -> mapping.path("test_code").asText()).collect(Collectors.toSet());
        assertEquals(expectedCodes, actualCodes);
        assertEquals(expectedCodes.size(), profile.path("default_test_mappings").size());
    }

    private static void assertMappingValues(JsonNode profile, String testCode, Set<String> expectedValues) {
        JsonNode mapping = StreamSupport.stream(profile.path("default_test_mappings").spliterator(), false)
                .filter(candidate -> testCode.equals(candidate.path("test_code").asText())).findFirst().orElseThrow();
        Set<String> actualValues = StreamSupport.stream(mapping.path("values").spliterator(), false)
                .map(JsonNode::asText).collect(Collectors.toSet());
        assertEquals(expectedValues, actualValues);
        assertEquals(expectedValues.size(), mapping.path("values").size());
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
