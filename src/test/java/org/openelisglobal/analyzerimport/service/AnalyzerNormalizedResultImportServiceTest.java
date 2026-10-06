package org.openelisglobal.analyzerimport.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationService;
import org.openelisglobal.analyzer.service.AnalyzerMappingService;
import org.openelisglobal.analyzer.service.AnalyzerMappingSnapshot;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.QCResultProcessingService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.analyzerresults.service.AnalyzerResultPlacementService;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class AnalyzerNormalizedResultImportServiceTest {

    private static final Path FIXTURES = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1",
            "fixtures");
    private static final FhirContext FHIR = FhirContext.forR4();

    @Mock
    private AnalyzerService analyzerService;
    @Mock
    private AnalyzerMappingService mappingService;
    @Mock
    private AnalyzerMappingConfirmationService confirmationService;
    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;
    @Mock
    private AnalyzerResultsService analyzerResultsService;
    @Mock
    private TestResultService testResultService;
    @Mock
    private QCResultProcessingService qcResultProcessingService;
    @Mock
    private AnalyzerResultPlacementService placementService;
    @Mock
    private org.openelisglobal.analyzer.service.BridgeProfileCatalogService profileCatalogService;

    @Mock
    private org.openelisglobal.analyzerimport.dao.AnalyzerDeliveryReceiptDAO receiptDAO;

    private AnalyzerNormalizedResultImportServiceImpl service;
    private Analyzer analyzer;
    private AnalyzerMapping revision;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new AnalyzerNormalizedResultImportServiceImpl(analyzerService, mappingService, analyzerResultsService,
                testResultService, qcResultProcessingService, FHIR, receiptDAO, confirmationService,
                mappingCatalogService, placementService, profileCatalogService);
        when(placementService.accessionFor(any())).thenAnswer(call -> call.getArgument(0));
        when(receiptDAO.findByDelivery(any(), any())).thenReturn(Optional.empty());
        when(confirmationService.hasMatchingConfirmation(any(), any())).thenReturn(true);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(
                List.of(new AnalyzerMappingCatalogService.TestOption("501", "Numeric test", "NUM", List.of()),
                        new AnalyzerMappingCatalogService.TestOption("601", "Qualitative test", "QUAL", List.of())));
        when(mappingCatalogService.getActiveResultOptions("601"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("701", "9001", "Indeterminate"),
                        new AnalyzerMappingCatalogService.ResultOption("702", "9002", "Positive")));
        analyzer = analyzer("site.mock-hematology", 1);
        when(analyzerService.findByBridgeConnectionIdForUpdate("bridge-connection-7f3c"))
                .thenReturn(Optional.of(analyzer));
    }

    @Test
    public void knownNumericResultUsesTheMappingInForceAndPreservesRawContext() throws IOException {
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());

        AnalyzerNormalizedResultImportSummary summary = service.importBundle(fixture("normalized-known-test.fhir.json"),
                "7");

        assertEquals(1, summary.resultsStaged());
        assertEquals(0, summary.resultsHeld());
        AnalyzerResults row = capturedRow();
        assertEquals("42", row.getAnalyzerId());
        assertEquals("501", row.getTestId());
        assertEquals("WBC", row.getTestName());
        assertEquals("7.5", row.getResult());
        assertFalse(row.isReadOnly());
        assertNull(row.getImportIssueReason());
        assertEquals("known-astm-001", row.getSourceMessageId());
        assertEquals("bridge-connection-7f3c", row.getSourceConnectionId());
        assertEquals("site.mock-hematology", row.getSourceProfileId());
        assertEquals(Integer.valueOf(1), row.getSourceProfileRevision());
        assertEquals("WBC", row.getRawTestCode());
        assertEquals("7.5", row.getRawResultValue());
        assertEquals("PATIENT", row.getResultClassification());
        assertFalse(row.getSourcePayload().isBlank());
    }

    @Test
    public void aTubeIdIsKeptVerbatimBesideTheAccessionItResolvesTo() throws IOException {
        when(placementService.accessionFor("ACC-KNOWN-001")).thenReturn("ACC-KNOWN");
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());

        service.importBundle(fixture("normalized-known-test.fhir.json"), "7");

        AnalyzerResults row = capturedRow();
        assertEquals("ACC-KNOWN-001", row.getInstrumentSpecimenId());
        assertEquals("ACC-KNOWN", row.getAccessionNumber());
    }

    @Test
    public void theDeliverysBundleIsKeptOnItsReceipt() throws IOException {
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());

        service.importBundle(fixture("normalized-known-test.fhir.json"), "7");

        ArgumentCaptor<org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt> receipt = ArgumentCaptor
                .forClass(org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt.class);
        verify(receiptDAO).insert(receipt.capture());
        Bundle kept = FHIR.newJsonParser().parseResource(Bundle.class, receipt.getValue().getBundleJson());
        assertEquals(fixture("normalized-known-test.fhir.json").getEntry().size(), kept.getEntry().size());
        assertTrue(receipt.getValue().getBundleJson().contains("ACC-KNOWN-001"));
    }

    @Test
    public void aNumericResultBeyondTheMeasuringRangeKeepsItsComparator() throws IOException {
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        observations(bundle).forEach(observation -> observation.getValueQuantity()
                .setComparator(org.hl7.fhir.r4.model.Quantity.QuantityComparator.LESS_THAN));

        service.importBundle(bundle, "7");

        AnalyzerResults row = capturedRow();
        assertEquals("<7.5", row.getResult());
        assertEquals("the instrument's own value text is kept as sent", "7.5", row.getRawResultValue());
    }

    @Test
    public void aRawValueThatAlreadyCarriesItsComparatorIsNotPrefixedAgain() throws IOException {
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        observations(bundle).forEach(observation -> {
            observation.getValueQuantity()
                    .setComparator(org.hl7.fhir.r4.model.Quantity.QuantityComparator.GREATER_THAN);
            observation.getExtensionByUrl("https://openelis-global.org/fhir/StructureDefinition/analyzer-raw-value")
                    .setValue(new org.hl7.fhir.r4.model.StringType(">7.5"));
        });

        service.importBundle(bundle, "7");

        assertEquals(">7.5", capturedRow().getResult());
    }

    @Test
    public void theInstrumentReportedPatientIsKeptOnTheStagedRowAsReported() throws IOException {
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());
        Bundle bundle = fixture("normalized-known-test.fhir.json");
        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
        patient.addIdentifier().setValue("PAT-77");
        patient.addName().setText("Doe, Jane");
        patient.addExtension("https://openelis-global.org/fhir/StructureDefinition/analyzer-patient-source",
                new org.hl7.fhir.r4.model.StringType("instrument"));
        bundle.addEntry().setFullUrl("urn:uuid:patient-1").setResource(patient);
        bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(org.hl7.fhir.r4.model.Observation.class::isInstance)
                .map(org.hl7.fhir.r4.model.Observation.class::cast).forEach(observation -> observation
                        .setSubject(new org.hl7.fhir.r4.model.Reference("urn:uuid:patient-1")));

        service.importBundle(bundle, "7");

        AnalyzerResults row = capturedRow();
        assertEquals("PAT-77", row.getInstrumentPatientId());
        assertEquals("Doe, Jane", row.getInstrumentPatientName());
    }

    @Test
    public void anAccessionIsStoredAsReportedAndAsTheInstrumentsSpecimenId() throws IOException {
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());

        service.importBundle(fixture("normalized-known-test.fhir.json"), "7");

        AnalyzerResults row = capturedRow();
        assertEquals("ACC-KNOWN-001", row.getInstrumentSpecimenId());
        assertEquals("ACC-KNOWN-001", row.getAccessionNumber());
    }

    @Test
    public void incomingResultIsHeldUntilItsSelectedMappingIsConfirmed() throws IOException {
        when(confirmationService.hasMatchingConfirmation(any(), any())).thenReturn(false);
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());

        AnalyzerNormalizedResultImportSummary summary = service.importBundle(fixture("normalized-known-test.fhir.json"), "7");

        assertEquals(1, summary.resultsHeld());
        AnalyzerResults row = capturedRow();
        assertEquals(AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY, row.getImportIssueReason());
        assertNull(row.getTestId());
        assertEquals("7.5", row.getRawResultValue());
    }

    @Test
    public void unknownTestIsDurablyHeldInsteadOfUsingLoincOrNameFallback() throws IOException {
        analyzer = analyzer("site.unknown-capable", 3);
        when(analyzerService.findByBridgeConnectionIdForUpdate("bridge-connection-7f3c"))
                .thenReturn(Optional.of(analyzer));
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());

        AnalyzerNormalizedResultImportSummary summary = service
                .importBundle(fixture("normalized-unknown-test.fhir.json"), "7");

        assertEquals(1, summary.resultsHeld());
        AnalyzerResults row = capturedRow();
        assertTrue(row.isReadOnly());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST, row.getImportIssueReason());
        assertNull(row.getTestId());
        assertEquals("VENDOR-NEW-42", row.getRawTestCode());
    }

    @Test
    public void retryUpdatesTheReasonWhenAResultRemainsHeldUnderTheCurrentMapping() throws IOException {
        analyzer = analyzer("site.unknown-capable", 3);
        when(analyzerService.findByBridgeConnectionIdForUpdate("bridge-connection-7f3c"))
                .thenReturn(Optional.of(analyzer));
        arrangeBinding(List.of(), List.of());
        service.importBundle(fixture("normalized-unknown-test.fhir.json"), "7");
        AnalyzerResults held = capturedRow();
        held.setId("9001");
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST, held.getImportIssueReason());

        arrangeBinding(List.of(boundTest("VENDOR-NEW-42", "501")), List.of());
        when(confirmationService.hasMatchingConfirmation(any(), any())).thenReturn(false);
        when(analyzerService.getWithMapping("42")).thenReturn(Optional.of(analyzer));
        when(analyzerResultsService.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of(held));

        assertEquals(0, service.recoverHeldMappingResults("42", "7"));
        ArgumentCaptor<AnalyzerResults> updated = ArgumentCaptor.forClass(AnalyzerResults.class);
        verify(analyzerResultsService).update(updated.capture());
        assertEquals("9001", updated.getValue().getId());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_TEST_MAPPING_NOT_READY, updated.getValue().getImportIssueReason());
        assertTrue(updated.getValue().isReadOnly());
    }

    @Test
    public void unknownQualitativeValueIsHeldAgainstItsMappedTest() throws IOException {
        arrangeBinding(List.of(boundTest("HIV-INTERP", "601")), List.of(boundResult("HIV-INTERP", "POSITIVE", "701")));

        AnalyzerNormalizedResultImportSummary summary = service
                .importBundle(fixture("normalized-unknown-value.fhir.json"), "7");

        assertEquals(1, summary.resultsHeld());
        AnalyzerResults row = capturedRow();
        assertEquals("601", row.getTestId());
        assertTrue(row.isReadOnly());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE, row.getImportIssueReason());
        assertEquals("INDETERMINATE-VENDOR-X", row.getRawResultValue());
    }

    @Test
    public void intentionallyExcludedTestIsNotStagedForReview() throws IOException {
        arrangeBinding(List.of(excludedTest("WBC")), List.of());

        AnalyzerNormalizedResultImportSummary summary = service.importBundle(fixture("normalized-known-test.fhir.json"),
                "7");

        assertEquals(0, summary.resultsStaged());
        assertEquals(0, summary.resultsHeld());
        verify(analyzerResultsService, never()).insertAnalyzerResults(anyList(), eq("7"));
    }

    @Test
    public void aResultForAnAssayThisInstrumentDoesNotRunIsHeldNotDropped() throws IOException {
        AnalyzerMappingTest off = boundTest(revision, "WBC", "501");
        off.setEnabled(false);
        arrangeBinding(List.of(off), List.of());

        AnalyzerNormalizedResultImportSummary summary = service.importBundle(fixture("normalized-known-test.fhir.json"),
                "7");

        assertEquals(1, summary.resultsHeld());
        ArgumentCaptor<List<AnalyzerResults>> staged = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService).insertAnalyzerResults(staged.capture(), eq("7"));
        assertEquals(AnalyzerResults.IMPORT_ISSUE_ASSAY_NOT_ENABLED, staged.getValue().get(0).getImportIssueReason());
        assertTrue(AnalyzerResults.MAPPING_IMPORT_ISSUES.contains(AnalyzerResults.IMPORT_ISSUE_ASSAY_NOT_ENABLED));
    }

    @Test
    public void intentionallyExcludedQualitativeValueIsNotStagedForReview() throws IOException {
        arrangeBinding(List.of(boundTest("HIV-INTERP", "601")),
                List.of(excludedResult("HIV-INTERP", "INDETERMINATE-VENDOR-X")));

        AnalyzerNormalizedResultImportSummary summary = service
                .importBundle(fixture("normalized-unknown-value.fhir.json"), "7");

        assertEquals(0, summary.resultsStaged());
        assertEquals(0, summary.resultsHeld());
        verify(analyzerResultsService, never()).insertAnalyzerResults(anyList(), eq("7"));
    }

    @Test
    public void boundQualitativeValueUsesOnlyTheSelectedCatalogOption() throws IOException {
        arrangeBinding(List.of(boundTest("HIV-INTERP", "601")),
                List.of(boundResult("HIV-INTERP", "INDETERMINATE-VENDOR-X", "701")));
        TestResult option = new TestResult();
        option.setId("701");
        option.setValue("9001");
        option.setTestResultType("D");
        when(testResultService.get("701")).thenReturn(option);

        service.importBundle(fixture("normalized-unknown-value.fhir.json"), "7");

        AnalyzerResults row = capturedRow();
        assertFalse(row.isReadOnly());
        assertEquals("601", row.getTestId());
        assertEquals("9001", row.getResult());
        assertEquals("D", row.getResultType());
    }

    @Test
    public void incomingResultKeepsTheMappingInForceWhileANewerDraftExists() throws IOException {
        arrangeBinding(List.of(boundTest("HIV-INTERP", "601")), List.of(boundResult("HIV-INTERP", "POSITIVE", "702")));
        AnalyzerMapping acknowledgedRevision = revision;
        AnalyzerMapping currentRevision = new AnalyzerMapping();
        currentRevision.setId("revision-2");
        currentRevision.setRevisionNumber(2);
        currentRevision.setMappingFingerprint("sha256:" + "3".repeat(64));
        AnalyzerMappingTest test = boundTest(currentRevision, "HIV-INTERP", "601");
        AnalyzerMappingResult result = boundResult(currentRevision, "HIV-INTERP", "INDETERMINATE-VENDOR-X", "701");
        when(mappingService.findLatestByAnalyzerId("42"))
                .thenReturn(Optional.of(new AnalyzerMappingSnapshot(currentRevision, List.of(test), List.of(result))));
        TestResult option = new TestResult();
        option.setId("701");
        option.setValue("9001");
        option.setTestResultType("D");
        when(testResultService.get("701")).thenReturn(option);

        service.importBundle(fixture("normalized-unknown-value.fhir.json"), "7");

        AnalyzerResults row = capturedRow();
        assertTrue(row.isReadOnly());
        assertEquals(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE, row.getImportIssueReason());
        assertEquals("INDETERMINATE-VENDOR-X", row.getResult());
        verify(mappingService).findById(acknowledgedRevision.getId());
        verify(mappingService, never()).findLatestByAnalyzerId("42");
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void twoAnalyzersUseTheirOwnAdoptedMappingRevision() throws IOException {
        arrangeBinding(List.of(boundTest("HIV-INTERP", "601")), List.of(boundResult("HIV-INTERP", "POSITIVE", "702")));
        AnalyzerMapping successor = new AnalyzerMapping();
        successor.setId("revision-2");
        successor.setRevisionNumber(2);
        successor.setProfileId(revision.getProfileId());
        successor.setProfileRevision(revision.getProfileRevision());
        successor.setProfileFingerprint(revision.getProfileFingerprint());
        successor.setMappingFingerprint("sha256:" + "3".repeat(64));
        Analyzer second = new Analyzer();
        second.setId("43");
        second.setName("Second analyzer");
        second.setBridgeConnectionId("bridge-connection-second");
        second.setMapping(successor);
        when(analyzerService.findByBridgeConnectionIdForUpdate("bridge-connection-second"))
                .thenReturn(Optional.of(second));
        when(mappingService.findById("revision-2")).thenReturn(
                Optional.of(new AnalyzerMappingSnapshot(successor, List.of(boundTest(successor, "HIV-INTERP", "601")),
                        List.of(boundResult(successor, "HIV-INTERP", "INDETERMINATE-VENDOR-X", "701")))));
        TestResult option = new TestResult();
        option.setId("701");
        option.setValue("9001");
        option.setTestResultType("D");
        when(testResultService.get("701")).thenReturn(option);
        Bundle secondBundle = FHIR.newJsonParser().parseResource(Bundle.class,
                Files.readString(FIXTURES.resolve("normalized-unknown-value.fhir.json"))
                        .replace("bridge-connection-7f3c", "bridge-connection-second"));

        assertEquals(1, service.importBundle(fixture("normalized-unknown-value.fhir.json"), "7").resultsHeld());
        assertEquals(0, service.importBundle(secondBundle, "7").resultsHeld());

        ArgumentCaptor<List> staged = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService, times(2)).insertAnalyzerResults(staged.capture(), eq("7"));
        AnalyzerResults firstRow = (AnalyzerResults) staged.getAllValues().get(0).get(0);
        AnalyzerResults secondRow = (AnalyzerResults) staged.getAllValues().get(1).get(0);
        assertEquals("42", firstRow.getAnalyzerId());
        assertEquals("INDETERMINATE-VENDOR-X", firstRow.getResult());
        assertEquals("43", secondRow.getAnalyzerId());
        assertEquals("9001", secondRow.getResult());
    }

    @Test
    public void operationalQcFailureIsNotAcknowledgedAsAnAcceptedDelivery() throws IOException {
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());
        // The fixture supplies the timestamp; the failure is independent of its
        // timezone conversion.
        org.mockito.Mockito.doThrow(new IllegalStateException("QC storage unavailable")).when(qcResultProcessingService)
                .processQCResult(eq("42"), eq("501"), eq("QC-LOT-WBC-2026-08"), eq("LOT-WBC-2026-08"), eq("NORMAL"),
                        eq(new java.math.BigDecimal("7.1")), eq("10*3/uL"), any(java.time.LocalDateTime.class));
        Bundle bundle = fixture("normalized-qc.fhir.json");

        assertThrows(IllegalStateException.class, () -> service.importBundle(bundle, "7"));
    }

    @Test
    public void recognizedControlUsesThePinnedBindingThenEntersOperationalQc() throws IOException {
        arrangeBinding(List.of(boundTest("WBC", "501")), List.of());

        AnalyzerNormalizedResultImportSummary summary = service.importBundle(fixture("normalized-qc.fhir.json"), "7");

        assertEquals(1, summary.controlResultsProcessed());
        AnalyzerResults row = capturedRow();
        assertTrue(row.getIsControl());
        assertFalse(row.isReadOnly());
        assertEquals("CONTROL", row.getResultClassification());
        assertEquals("RULES", row.getRecognitionMode());
        assertEquals("MATCH", row.getRecognitionOutcome());
        assertEquals("LOT-WBC-2026-08", row.getLotNumber());
        assertEquals("NORMAL", row.getControlLevel());
        verify(qcResultProcessingService).processQCResult(eq("42"), eq("501"), eq("QC-LOT-WBC-2026-08"),
                eq("LOT-WBC-2026-08"), eq("NORMAL"), eq(new java.math.BigDecimal("7.1")), eq("10*3/uL"),
                any(java.time.LocalDateTime.class));
    }

    @Test
    public void unknownConnectionIsRejectedWithoutCreatingOrStagingAnything() throws IOException {
        when(analyzerService.findByBridgeConnectionIdForUpdate("bridge-connection-7f3c")).thenReturn(Optional.empty());

        AnalyzerNormalizedResultImportException error = assertThrows(AnalyzerNormalizedResultImportException.class,
                () -> service.importBundle(fixture("normalized-known-test.fhir.json"), "7"));

        assertEquals("analyzer.fhirImport.error.unknownConnection", error.getErrorKey());
        verify(analyzerResultsService, never()).insertAnalyzerResults(anyList(), eq("7"));
        verify(mappingService, never()).findById(any());
    }

    @Test
    public void mismatchedPinnedProfileIsRejectedBeforeMapping() throws IOException {
        analyzer = analyzer("different-profile", 1);
        when(analyzerService.findByBridgeConnectionIdForUpdate("bridge-connection-7f3c"))
                .thenReturn(Optional.of(analyzer));

        AnalyzerNormalizedResultImportException error = assertThrows(AnalyzerNormalizedResultImportException.class,
                () -> service.importBundle(fixture("normalized-known-test.fhir.json"), "7"));

        assertEquals("analyzer.fhirImport.error.profileMismatch", error.getErrorKey());
        verify(analyzerResultsService, never()).insertAnalyzerResults(anyList(), eq("7"));
    }

    private Analyzer analyzer(String profileId, int profileRevision) {
        revision = new AnalyzerMapping();
        revision.setId("revision-1");
        revision.setRevisionNumber(1);
        revision.setProfileId(profileId);
        revision.setProfileRevision(profileRevision);
        revision.setProfileFingerprint("sha256:" + "1".repeat(64));
        revision.setMappingFingerprint("sha256:" + "2".repeat(64));

        Analyzer value = new Analyzer();
        value.setId("42");
        value.setName("Lab analyzer");
        value.setBridgeConnectionId("bridge-connection-7f3c");
        value.setMapping(revision);
        return value;
    }

    private void arrangeBinding(List<AnalyzerMappingTest> tests, List<AnalyzerMappingResult> results) {
        AnalyzerMappingSnapshot snapshot = new AnalyzerMappingSnapshot(revision, tests, results);
        when(mappingService.findById(revision.getId())).thenReturn(Optional.of(snapshot));
    }

    private AnalyzerMappingTest boundTest(String sourceRowKey, String testId) {
        return boundTest(revision, sourceRowKey, testId);
    }

    private AnalyzerMappingTest boundTest(AnalyzerMapping bindingRevision, String sourceRowKey, String testId) {
        AnalyzerMappingTest row = new AnalyzerMappingTest();
        row.setId(new AnalyzerMappingTestPK(bindingRevision.getId(), sourceRowKey));
        row.setMapping(bindingRevision);
        row.setMappingState(AnalyzerMappingState.BOUND);
        row.setTestId(testId);
        return row;
    }

    private AnalyzerMappingTest excludedTest(String sourceRowKey) {
        AnalyzerMappingTest row = new AnalyzerMappingTest();
        row.setId(new AnalyzerMappingTestPK(revision.getId(), sourceRowKey));
        row.setMapping(revision);
        row.setMappingState(AnalyzerMappingState.EXCLUDED);
        return row;
    }

    private AnalyzerMappingResult boundResult(String sourceRowKey, String rawValue, String testResultId) {
        return boundResult(revision, sourceRowKey, rawValue, testResultId);
    }

    private AnalyzerMappingResult boundResult(AnalyzerMapping bindingRevision, String sourceRowKey, String rawValue,
            String testResultId) {
        AnalyzerMappingResult row = new AnalyzerMappingResult();
        row.setId(new AnalyzerMappingResultPK(bindingRevision.getId(), sourceRowKey, rawValue));
        row.setMapping(bindingRevision);
        row.setMappingState(AnalyzerMappingState.BOUND);
        row.setTestResultId(testResultId);
        return row;
    }

    private AnalyzerMappingResult excludedResult(String sourceRowKey, String rawValue) {
        AnalyzerMappingResult row = new AnalyzerMappingResult();
        row.setId(new AnalyzerMappingResultPK(revision.getId(), sourceRowKey, rawValue));
        row.setMapping(revision);
        row.setMappingState(AnalyzerMappingState.EXCLUDED);
        return row;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private AnalyzerResults capturedRow() {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService).insertAnalyzerResults(captor.capture(), eq("7"));
        return ((List<AnalyzerResults>) captor.getValue()).get(0);
    }

    private static java.util.stream.Stream<org.hl7.fhir.r4.model.Observation> observations(Bundle bundle) {
        return bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(org.hl7.fhir.r4.model.Observation.class::isInstance)
                .map(org.hl7.fhir.r4.model.Observation.class::cast);
    }

    private Bundle fixture(String name) throws IOException {
        return FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURES.resolve(name)));
    }
}
