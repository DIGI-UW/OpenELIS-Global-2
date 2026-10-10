package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
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
    public void readsTheInstrumentReportedPatientFromTheObservationSubject() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        addPatient(bundle, "urn:uuid:patient-1", "PAT-77", "Doe, Jane", true);

        AnalyzerNormalizedResultContract.Result result = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results()
                .get(0);

        assertEquals("PAT-77", result.instrumentPatientId());
        assertEquals("Doe, Jane", result.instrumentPatientName());
    }

    @Test
    public void ignoresAPatientThatIsNotMarkedAsInstrumentReported() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        addPatient(bundle, "urn:uuid:patient-1", "PAT-77", "Doe, Jane", false);

        AnalyzerNormalizedResultContract.Result result = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results()
                .get(0);

        assertNull(result.instrumentPatientId());
        assertNull(result.instrumentPatientName());
    }

    @Test
    public void aResultWithoutAPatientCarriesNone() throws IOException {
        AnalyzerNormalizedResultContract.Result result = AnalyzerNormalizedResultContract
                .parse(fixture("normalized-known-test.fhir.json"), FHIR).results().get(0);

        assertNull(result.instrumentPatientId());
        assertNull(result.instrumentPatientName());
    }

    @Test
    public void aRunWithNoValueAndADataAbsentReasonIsAFailedRunCarryingTheInstrumentsNotes() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        Observation observation = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
        observation.setValue(null);
        observation.getDataAbsentReason().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/data-absent-reason").setCode("error");
        observation.addNote().setText("Error 2008: pressure abort");
        observation.addNote().setText("Repeat with a new cartridge");

        AnalyzerNormalizedResultContract.Result result = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results()
                .get(0);

        assertTrue(result.runFailed());
        assertEquals("Error 2008: pressure abort\nRepeat with a new cartridge", result.note());
    }

    @Test
    public void aResultWithAValueIsNotAFailedRunAndCarriesNoNoteWhenNoneIsSent() throws IOException {
        AnalyzerNormalizedResultContract.Result result = AnalyzerNormalizedResultContract
                .parse(fixture("normalized-known-test.fhir.json"), FHIR).results().get(0);

        assertFalse(result.runFailed());
        assertNull(result.note());
    }

    @Test
    public void readsTheInstrumentsFlagAssayAndOperatorAsSent() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        Observation observation = firstObservation(bundle);
        observation.addInterpretation().setText("Detected").addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation").setCode("DET");
        observation.addInterpretation().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation").setCode("H")
                .setDisplay("H");
        observation.getMethod().setText("Xpert HIV-1 Viral Load").addExtension(
                "https://openelis-global.org/fhir/StructureDefinition/analyzer-assay-version", new StringType("4"));
        observation.addPerformer().setDisplay("Operator 12");

        AnalyzerNormalizedResultContract.Result result = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results()
                .get(0);

        assertEquals("H", result.flags());
        assertEquals("Xpert HIV-1 Viral Load", result.assayName());
        assertEquals("4", result.assayVersion());
        assertEquals("Operator 12", result.operator());
    }

    @Test
    public void aResultWithoutInstrumentProvenanceCarriesNone() throws IOException {
        AnalyzerNormalizedResultContract.Result result = AnalyzerNormalizedResultContract
                .parse(fixture("normalized-known-test.fhir.json"), FHIR).results().get(0);

        assertNull(result.flags());
        assertNull(result.assayName());
        assertNull(result.assayVersion());
        assertNull(result.operator());
    }

    private static Observation firstObservation(Bundle bundle) {
        return bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
    }

    private static void addPatient(Bundle bundle, String fullUrl, String identifier, String name,
            boolean instrumentReported) {
        Patient patient = new Patient();
        patient.addIdentifier().setValue(identifier);
        patient.addName().setText(name);
        if (instrumentReported) {
            patient.addExtension("https://openelis-global.org/fhir/StructureDefinition/analyzer-patient-source",
                    new StringType("instrument"));
        }
        bundle.addEntry().setFullUrl(fullUrl).setResource(patient);
        bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).filter(Observation.class::isInstance)
                .map(Observation.class::cast).forEach(observation -> observation.setSubject(new Reference(fullUrl)));
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
