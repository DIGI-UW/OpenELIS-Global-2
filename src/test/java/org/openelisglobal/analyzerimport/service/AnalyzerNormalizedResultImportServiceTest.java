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
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingService;
import org.openelisglobal.analyzer.service.AnalyzerSiteBindingSnapshot;
import org.openelisglobal.analyzer.service.QCResultProcessingService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTestPK;
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
    private AnalyzerSiteBindingService siteBindingService;
    @Mock
    private AnalyzerResultsService analyzerResultsService;
    @Mock
    private TestResultService testResultService;
    @Mock
    private QCResultProcessingService qcResultProcessingService;

    private AnalyzerNormalizedResultImportServiceImpl service;
    private Analyzer analyzer;
    private AnalyzerSiteBindingRevision revision;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new AnalyzerNormalizedResultImportServiceImpl(analyzerService, siteBindingService,
                analyzerResultsService, testResultService, qcResultProcessingService, FHIR);
        analyzer = analyzer("site.mock-hematology", 1);
        when(analyzerService.findByBridgeConnectionId("bridge-connection-7f3c")).thenReturn(Optional.of(analyzer));
    }

    @Test
    public void knownNumericResultUsesCurrentSiteBindingAndPreservesRawContext() throws IOException {
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
    public void unknownTestIsDurablyHeldInsteadOfUsingLoincOrNameFallback() throws IOException {
        analyzer = analyzer("site.unknown-capable", 3);
        when(analyzerService.findByBridgeConnectionId("bridge-connection-7f3c")).thenReturn(Optional.of(analyzer));
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
    public void nextMessageUsesTheLatestSharedSiteBindingAfterValueResolution() throws IOException {
        AnalyzerSiteBindingRevision acknowledgedRevision = revision;
        AnalyzerSiteBindingRevision currentRevision = new AnalyzerSiteBindingRevision();
        currentRevision.setId("revision-2");
        currentRevision.setSiteBinding(acknowledgedRevision.getSiteBinding());
        currentRevision.setRevisionNumber(2);
        currentRevision.setBindingFingerprint("sha256:" + "3".repeat(64));
        AnalyzerSiteBindingTest test = boundTest(currentRevision, "HIV-INTERP", "601");
        AnalyzerSiteBindingResult result = boundResult(currentRevision, "HIV-INTERP", "INDETERMINATE-VENDOR-X", "701");
        when(siteBindingService.findCurrentByProfileBindingId("profile-binding-1"))
                .thenReturn(Optional.of(new AnalyzerSiteBindingSnapshot(currentRevision.getSiteBinding(),
                        currentRevision, List.of(test), List.of(result))));
        TestResult option = new TestResult();
        option.setId("701");
        option.setValue("9001");
        option.setTestResultType("D");
        when(testResultService.get("701")).thenReturn(option);

        service.importBundle(fixture("normalized-unknown-value.fhir.json"), "7");

        AnalyzerResults row = capturedRow();
        assertFalse(row.isReadOnly());
        assertEquals("9001", row.getResult());
        verify(siteBindingService, never()).findByRevisionId(acknowledgedRevision.getId());
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
        when(analyzerService.findByBridgeConnectionId("bridge-connection-7f3c")).thenReturn(Optional.empty());

        AnalyzerNormalizedResultImportException error = assertThrows(AnalyzerNormalizedResultImportException.class,
                () -> service.importBundle(fixture("normalized-known-test.fhir.json"), "7"));

        assertEquals("analyzer.fhirImport.error.unknownConnection", error.getErrorKey());
        verify(analyzerResultsService, never()).insertAnalyzerResults(anyList(), eq("7"));
        verify(siteBindingService, never()).findCurrentByProfileBindingId("profile-binding-1");
    }

    @Test
    public void mismatchedPinnedProfileIsRejectedBeforeMapping() throws IOException {
        analyzer = analyzer("different-profile", 1);
        when(analyzerService.findByBridgeConnectionId("bridge-connection-7f3c")).thenReturn(Optional.of(analyzer));

        AnalyzerNormalizedResultImportException error = assertThrows(AnalyzerNormalizedResultImportException.class,
                () -> service.importBundle(fixture("normalized-known-test.fhir.json"), "7"));

        assertEquals("analyzer.fhirImport.error.profileMismatch", error.getErrorKey());
        verify(analyzerResultsService, never()).insertAnalyzerResults(anyList(), eq("7"));
    }

    private Analyzer analyzer(String profileId, int profileRevision) {
        AnalyzerProfileBinding profile = new AnalyzerProfileBinding();
        profile.setId("profile-binding-1");
        profile.setProfileId(profileId);
        profile.setProfileRevision(profileRevision);
        profile.setProfileFingerprint("sha256:" + "1".repeat(64));

        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("site-binding-1");
        binding.setProfileBinding(profile);

        revision = new AnalyzerSiteBindingRevision();
        revision.setId("revision-1");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(1);
        revision.setBindingFingerprint("sha256:" + "2".repeat(64));

        Analyzer value = new Analyzer();
        value.setId("42");
        value.setName("Lab analyzer");
        value.setBridgeConnectionId("bridge-connection-7f3c");
        value.setSiteBindingRevision(revision);
        return value;
    }

    private void arrangeBinding(List<AnalyzerSiteBindingTest> tests, List<AnalyzerSiteBindingResult> results) {
        AnalyzerSiteBindingSnapshot snapshot = new AnalyzerSiteBindingSnapshot(revision.getSiteBinding(), revision,
                tests, results);
        when(siteBindingService.findCurrentByProfileBindingId("profile-binding-1")).thenReturn(Optional.of(snapshot));
    }

    private AnalyzerSiteBindingTest boundTest(String sourceRowKey, String testId) {
        return boundTest(revision, sourceRowKey, testId);
    }

    private AnalyzerSiteBindingTest boundTest(AnalyzerSiteBindingRevision bindingRevision, String sourceRowKey,
            String testId) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setId(new AnalyzerSiteBindingTestPK(bindingRevision.getId(), sourceRowKey));
        row.setSiteBindingRevision(bindingRevision);
        row.setMappingState(AnalyzerSiteBindingMappingState.BOUND);
        row.setTestId(testId);
        return row;
    }

    private AnalyzerSiteBindingTest excludedTest(String sourceRowKey) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRowKey));
        row.setSiteBindingRevision(revision);
        row.setMappingState(AnalyzerSiteBindingMappingState.EXCLUDED);
        return row;
    }

    private AnalyzerSiteBindingResult boundResult(String sourceRowKey, String rawValue, String testResultId) {
        return boundResult(revision, sourceRowKey, rawValue, testResultId);
    }

    private AnalyzerSiteBindingResult boundResult(AnalyzerSiteBindingRevision bindingRevision, String sourceRowKey,
            String rawValue, String testResultId) {
        AnalyzerSiteBindingResult row = new AnalyzerSiteBindingResult();
        row.setId(new AnalyzerSiteBindingResultPK(bindingRevision.getId(), sourceRowKey, rawValue));
        row.setSiteBindingRevision(bindingRevision);
        row.setMappingState(AnalyzerSiteBindingMappingState.BOUND);
        row.setTestResultId(testResultId);
        return row;
    }

    private AnalyzerSiteBindingResult excludedResult(String sourceRowKey, String rawValue) {
        AnalyzerSiteBindingResult row = new AnalyzerSiteBindingResult();
        row.setId(new AnalyzerSiteBindingResultPK(revision.getId(), sourceRowKey, rawValue));
        row.setSiteBindingRevision(revision);
        row.setMappingState(AnalyzerSiteBindingMappingState.EXCLUDED);
        return row;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private AnalyzerResults capturedRow() {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(analyzerResultsService).insertAnalyzerResults(captor.capture(), eq("7"));
        return ((List<AnalyzerResults>) captor.getValue()).get(0);
    }

    private Bundle fixture(String name) throws IOException {
        return FHIR.newJsonParser().parseResource(Bundle.class, Files.readString(FIXTURES.resolve(name)));
    }
}
