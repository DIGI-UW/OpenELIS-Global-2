package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.junit.Test;

public class AnalyzerNormalizedResultContractTest {

    private static final Path FIXTURES = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures");
    private static final FhirContext FHIR = FhirContext.forR4();

    @Test
    public void parsesExactConnectionProfileAndRawPatientContext() throws IOException {
        AnalyzerNormalizedResultContract contract = AnalyzerNormalizedResultContract
                .parse(fixture("normalized-known-test.fhir.json"), FHIR);

        assertEquals("known-astm-001", contract.messageId());
        assertEquals("bridge-connection-7f3c", contract.bridgeConnectionId());
        assertEquals("site.mock-hematology", contract.profileId());
        assertEquals(1, contract.profileRevision());
        assertEquals("ASTM", contract.sourceProtocol());
        assertEquals(1, contract.results().size());

        AnalyzerNormalizedResultContract.Result result = contract.results().get(0);
        assertEquals("ACC-KNOWN-001", result.accessionNumber());
        assertEquals("WBC", result.rawTestCode());
        assertEquals("7.5", result.rawValue());
        assertEquals("10*3/uL", result.units());
        assertEquals("TCP", result.sourceTransport());
        assertEquals("PATIENT", result.classification());
        assertEquals("RULES", result.recognitionMode());
        assertEquals("NO_MATCH", result.recognitionOutcome());
        assertFalse(result.sourcePayload().isBlank());
    }

    @Test
    public void parsesBridgeRecognizedControlEvidence() throws IOException {
        AnalyzerNormalizedResultContract contract = AnalyzerNormalizedResultContract
                .parse(fixture("normalized-qc.fhir.json"), FHIR);

        AnalyzerNormalizedResultContract.Result result = contract.results().get(0);
        assertEquals("CONTROL", result.classification());
        assertEquals("MATCH", result.recognitionOutcome());
        assertTrue(result.recognitionFingerprint().startsWith("sha256:"));
        assertEquals("LOT-WBC-2026-08", result.lotNumber());
        assertEquals("NORMAL", result.controlLevel());
    }

    @Test
    public void rejectsLocalAnalyzerIdWhenExactBridgeConnectionIdIsAbsent() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        Device device = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Device.class::isInstance).map(Device.class::cast).findFirst().orElseThrow();
        device.getIdentifier().removeIf(
                identifier -> "https://openelis-global.org/fhir/analyzer-connection-id".equals(identifier.getSystem()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));

        assertEquals("Normalized analyzer traffic requires one Bridge connection ID", error.getMessage());
    }

    private static Bundle fixture(String name) throws IOException {
        return FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURES.resolve(name)));
    }
}
