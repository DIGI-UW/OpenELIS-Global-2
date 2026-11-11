package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.math.BigDecimal;
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
    public void versionTwelveRequiresExactlyOneValidReceiptPurpose() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        Device device = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Device.class::isInstance).map(Device.class::cast).findFirst().orElseThrow();
        String root = "https://openelis-global.org/fhir/StructureDefinition/";
        device.getExtensionByUrl(root + "analyzer-contract-version").setValue(new StringType("1.2"));
        // Existing flag evidence keeps its explicit role under the newer contract.
        for (var entry : bundle.getEntry()) {
            if (entry.getResource() instanceof Observation observation) {
                observation.getInterpretation().forEach(value -> value.addExtension(root + "analyzer-result-part",
                        new org.hl7.fhir.r4.model.CodeType("flag")));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));
        var purpose = device.addExtension().setUrl(root + "analyzer-delivery-purpose")
                .setValue(new org.hl7.fhir.r4.model.CodeType("SETUP"));
        assertEquals("SETUP", AnalyzerNormalizedResultContract.parse(bundle, FHIR).deliveryPurpose());
        purpose.setValue(new org.hl7.fhir.r4.model.CodeType("unknown"));
        assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));
        purpose.setValue(new org.hl7.fhir.r4.model.CodeType("SETUP"));
        device.addExtension(root + "analyzer-delivery-purpose", new org.hl7.fhir.r4.model.CodeType("ROUTINE"));
        assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));
    }

    @Test
    public void newContractDoesNotGuessAnInterpretationRoleFromItsCode() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        Device device = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Device.class::isInstance).map(Device.class::cast).findFirst().orElseThrow();
        String versionUrl = "https://openelis-global.org/fhir/StructureDefinition/analyzer-contract-version";
        device.getExtension().removeIf(extension -> versionUrl.equals(extension.getUrl()));
        device.addExtension(versionUrl, new StringType("1.1"));
        Observation observation = observation(bundle);
        observation.addInterpretation().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation").setCode("POS");
        assertEquals("Version 1.1 interpretations require an explicit call or flag role",
                assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR))
                        .getMessage());
    }

    @Test
    public void unsupportedValueCannotFallBackToRawEvidence() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        observation(bundle).setValue(new org.hl7.fhir.r4.model.BooleanType(true));
        assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));
    }

    @Test
    public void recognitionFingerprintIsOptionalAuditEvidence() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        observation(bundle)
                .getExtensionByUrl("https://openelis-global.org/fhir/StructureDefinition/analyzer-control-recognition")
                .getExtension().removeIf(extension -> "recognitionFingerprint".equals(extension.getUrl()));
        var result = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results().get(0);
        assertEquals("PATIENT", result.classification());
        assertEquals("7.5", result.number());
        assertNull(result.recognitionFingerprint());
    }

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
    public void aWarningTheInstrumentRaisedAboutTheSampleIsAFlagNotAResult() throws IOException {
        AnalyzerNormalizedResultContract contract = AnalyzerNormalizedResultContract
                .parse(fixture("normalized-sample-flag.fhir.json"), FHIR);

        assertEquals(1, contract.results().size());
        assertEquals("WBC", contract.results().get(0).rawTestCode());
        assertEquals(java.util.List.of("Anemia"), contract.sampleFlags());
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
    public void parsesBridgeRecognizedControlWithoutInventingALotNumber() throws IOException {
        AnalyzerNormalizedResultContract contract = AnalyzerNormalizedResultContract
                .parse(fixture("normalized-qc.fhir.json"), FHIR);

        AnalyzerNormalizedResultContract.Result result = contract.results().get(0);
        assertEquals("CONTROL", result.classification());
        assertEquals("MATCH", result.recognitionOutcome());
        assertTrue(result.recognitionFingerprint().startsWith("sha256:"));
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

    @Test
    public void rejectsANumericResultWithAHugeExponent() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        observation(bundle).getValueQuantity().setValue(new BigDecimal("1E+2000000000"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));

        assertEquals("A numeric analyzer result must be a bounded decimal", error.getMessage());
    }

    @Test
    public void theStoredNumberIsTheQuantityNotTheRawText() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        observation(bundle).getExtension().stream()
                .filter(extension -> extension.getUrl().endsWith("/analyzer-raw-value")).findFirst().orElseThrow()
                .setValue(new StringType("7.5; java.lang.System.exit(0)"));

        AnalyzerNormalizedResultContract.Result result = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results()
                .get(0);

        assertEquals("7.5", result.number());
    }

    private static Observation observation(Bundle bundle) {
        return bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).map(Observation.class::cast).findFirst().orElseThrow();
    }

    @Test
    public void explicitCallRoleDoesNotDependOnAStandardVocabulary() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        Observation observation = (Observation) bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).findFirst().orElseThrow();
        observation.addExtension("https://openelis-global.org/fhir/StructureDefinition/analyzer-canonical-answer",
                new StringType("REACTIVE"));
        var call = observation.addInterpretation().setText("REACTIVE");
        call.addExtension("https://openelis-global.org/fhir/StructureDefinition/analyzer-result-part",
                new StringType("call"));
        var flag = observation.addInterpretation().setText("Instrument warning");
        flag.addExtension("https://openelis-global.org/fhir/StructureDefinition/analyzer-result-part",
                new StringType("flag"));

        var result = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results().get(0);
        assertEquals("7.5", result.number());
        assertEquals("REACTIVE", result.call());
        assertEquals("Instrument warning", result.flags());
    }

    @Test
    public void canonicalAnswerIsUsedWhileVendorTextRemainsEvidence() throws IOException {
        Bundle bundle = fixture("normalized-unknown-value.fhir.json");
        Observation observation = (Observation) bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).findFirst().orElseThrow();
        observation.addExtension("https://openelis-global.org/fhir/StructureDefinition/analyzer-canonical-answer",
                new StringType("INDETERMINATE"));
        observation.setValue(new StringType("INDETERMINATE"));
        var result = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results().get(0);
        assertEquals("INDETERMINATE", result.call());
        assertEquals("INDETERMINATE-VENDOR-X", result.rawValue());
    }

    @Test
    public void aResultCannotClaimBothAValueAndAFailedRun() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        Observation observation = (Observation) bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance).findFirst().orElseThrow();
        observation.getDataAbsentReason().addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/data-absent-reason").setCode("error");
        assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));
    }

    @Test
    public void readsTheSharedVersion11FixtureWithExplicitRoles() throws IOException {
        var result = AnalyzerNormalizedResultContract.parse(fixture("normalized-explicit-parts.fhir.json"), FHIR)
                .results().get(0);
        assertEquals("7.5", result.number());
        assertEquals("REACTIVE", result.call());
        assertEquals("Instrument warning", result.flags());
    }

    @Test
    public void rejectsAContradictoryCanonicalAnswer() throws IOException {
        Bundle bundle = fixture("normalized-explicit-parts.fhir.json");
        observation(bundle).getInterpretationFirstRep().setText("NONREACTIVE");
        assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));
    }

    @Test
    public void rejectsAnUnknownRoleInsteadOfGuessingCallOrFlag() throws IOException {
        Bundle bundle = fixture("normalized-explicit-parts.fhir.json");
        observation(bundle).getInterpretationFirstRep().getExtensionFirstRep()
                .setValue(new org.hl7.fhir.r4.model.CodeType("other"));
        assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));
    }

    @Test
    public void patientClassificationCannotContradictMatchedControlEvidence() throws IOException {
        Bundle bundle = fixture("normalized-qc.fhir.json");
        observation(bundle).getExtension().stream()
                .filter(extension -> extension.getUrl().endsWith("/analyzer-result-classification")).findFirst()
                .orElseThrow().setValue(new org.hl7.fhir.r4.model.CodeType("PATIENT"));
        assertThrows(IllegalArgumentException.class, () -> AnalyzerNormalizedResultContract.parse(bundle, FHIR));
    }

    @Test
    public void preservesAnswerCodingsForBothQualitativeAndCombinedResults() throws IOException {
        for (String name : java.util.List.of("normalized-unknown-value.fhir.json",
                "normalized-explicit-parts.fhir.json")) {
            Bundle bundle = fixture(name);
            Observation observation = observation(bundle);
            if (observation.hasValueQuantity()) {
                observation.getInterpretationFirstRep().addCoding().setSystem("http://snomed.info/sct")
                        .setCode("10828004");
            } else {
                observation.setValue(new org.hl7.fhir.r4.model.CodeableConcept().setText("POSITIVE")
                        .addCoding(new org.hl7.fhir.r4.model.Coding("http://snomed.info/sct", "10828004", null)));
            }
            assertTrue(AnalyzerNormalizedResultContract.parse(bundle, FHIR).results().get(0).answerCodings().stream()
                    .anyMatch(code -> code
                            .sameAs(new org.openelisglobal.terminology.Coding("http://snomed.info/sct", "10828004"))));
        }
    }

    @Test
    public void legacyCallRoleIsNotAClinicalAnswerCodeButActualCodingsSurvive() throws IOException {
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        Observation observation = firstObservation(bundle);
        var call = observation.addInterpretation().setText("DETECTED");
        call.addCoding().setSystem("http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation").setCode("DET");
        assertTrue(AnalyzerNormalizedResultContract.parse(bundle, FHIR).results().get(0).answerCodings().isEmpty());
        call.addCoding().setSystem("http://snomed.info/sct").setCode("260373001");
        var codings = AnalyzerNormalizedResultContract.parse(bundle, FHIR).results().get(0).answerCodings();
        assertEquals(1, codings.size());
        assertTrue(codings.get(0)
                .sameAs(new org.openelisglobal.terminology.Coding("http://snomed.info/sct", "260373001")));
        call.addExtension("https://openelis-global.org/fhir/StructureDefinition/analyzer-result-part",
                new StringType("call"));
        assertEquals(2, AnalyzerNormalizedResultContract.parse(bundle, FHIR).results().get(0).answerCodings().size());
    }

    private static Bundle fixture(String name) throws IOException {
        return FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURES.resolve(name)));
    }
}
