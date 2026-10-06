package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingOrigin;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingEditorServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private BridgeProfileCatalogService bridgeProfileCatalogService;

    @Mock
    private AnalyzerMappingService mappingService;

    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;

    @Mock
    private AnalyzerMappingConfirmationService confirmationService;

    @Mock
    private AnalyzerResultsService analyzerResultsService;

    @Mock
    private TestResultService testResultService;

    private AnalyzerMappingEditorService service;

    @Before
    public void setUp() {
        service = new AnalyzerMappingEditorServiceImpl(analyzerService, bridgeProfileCatalogService, mappingService,
                mappingCatalogService, confirmationService, analyzerResultsService,
                new AnalyzerMappingDefaults(mappingCatalogService, testResultService));
    }

    @Test
    public void getMappingPreservesEverySourceRowAndHydratesCurrentLocalChoices() throws Exception {
        AnalyzerMappingSnapshot current = currentMapping();
        AnalyzerMappingConfirmationView confirmation = new AnalyzerMappingConfirmationView(
                AnalyzerMappingConfirmationView.State.STALE, "site.mock-analyzer", 2,
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", recognitionFingerprint(),
                "16", "Grace Hopper", null, List.of(), List.of());
        analyzerWithLatest(current);
        when(confirmationService.getStatus(current, recognitionFingerprint())).thenReturn(confirmation);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(testResultService.getActiveTestResultsByTest("9701")).thenReturn(List.of(numericResult()));
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(positiveAndNegative());

        AnalyzerMappingView view = service.getMapping("42");

        assertEquals("42", view.analyzerId());
        assertEquals("site.mock-analyzer", view.profileId());
        assertEquals(2, view.profileRevision());
        assertEquals("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                view.profileFingerprint());
        assertEquals("61", view.mappingId());
        assertEquals(4, view.mappingRevision());
        assertEquals(3, view.tests().size());

        AnalyzerMappingView.TestRow first = view.tests().get(0);
        assertEquals("RAW-A", first.sourceRowKey());
        assertEquals("RAW-A", first.rawCode());
        assertEquals(List.of("RAW-A1", "RAW-A2"), first.aliases());
        assertEquals("First result", first.testNameHint());
        assertEquals("94500-6", first.loinc());
        assertEquals("https://loinc.org", first.normalizedCoding().system());
        assertEquals("94500-6", first.normalizedCoding().code());
        assertEquals(AnalyzerMappingState.BOUND, first.mappingState());
        assertEquals(AnalyzerMappingOrigin.DEFAULT, first.origin());
        assertEquals("9701", first.testId());
        assertEquals("SARS-CoV-2 RNA", first.selectedTest().name());
        assertEquals(2, first.results().size());
        assertEquals("POS", first.results().get(0).rawValue());
        assertEquals("811", first.results().get(0).resultOptionId());
        assertEquals("Positive", first.results().get(0).selectedOption().label());
        assertEquals(AnalyzerMappingState.EXCLUDED, first.results().get(1).mappingState());
        assertEquals(AnalyzerMappingOrigin.OVERRIDE, first.results().get(1).origin());

        assertEquals("RAW-B", view.tests().get(1).sourceRowKey());
        assertEquals("94500-6", view.tests().get(1).loinc());
        assertEquals("9701", view.tests().get(1).suggestedTest().id());
        assertEquals("RAW-C", view.tests().get(2).sourceRowKey());
        assertNull(view.tests().get(2).suggestedTest());

        assertEquals("RULES", view.controlRecognition().mode());
        assertEquals("Specimen ID starts with QC-", view.controlRecognition().conditions().get(0).description());
        assertEquals("SPECIMEN_ID_STARTS_WITH", view.controlRecognition().conditions().get(0).kind());
        assertEquals("Specimen ID", view.controlRecognition().conditions().get(0).sourceLabel());
        assertEquals("QC-", view.controlRecognition().conditions().get(0).value());
        assertEquals(AnalyzerMappingConfirmationView.State.STALE, view.confirmation().state());
        verify(mappingService).findLatestByAnalyzerId("42");
    }

    @Test
    public void confirmMappingKeepsUnresolvedRowsSeparateFromConfirmedDecisions() throws Exception {
        AnalyzerMappingSnapshot candidate = currentMapping();
        AnalyzerMappingConfirmationRequest request = new AnalyzerMappingConfirmationRequest(
                candidate.mapping().getMappingFingerprint(), recognitionFingerprint(),
                List.of(new AnalyzerMappingSourceRow("RAW-A", null), new AnalyzerMappingSourceRow("RAW-A", "POS")),
                List.of(new AnalyzerMappingSourceRow("RAW-A", "NEG")));
        AnalyzerMappingConfirmationView expected = AnalyzerMappingConfirmationView.unconfirmed();
        analyzerWithLatest(candidate);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(List.of(positive()));
        when(confirmationService.confirm(candidate, recognitionFingerprint(), request, "17")).thenReturn(expected);

        AnalyzerMappingConfirmationView confirmed = service.confirmMapping("42", request, "17");

        assertEquals(expected, confirmed);
        verify(confirmationService).confirm(candidate, recognitionFingerprint(), request, "17");
    }

    @Test
    public void getDefaultsPreviewsWhatANewAnalyzerWouldBindWithoutSavingAnything() throws Exception {
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(coveredAnswers());
        when(testResultService.getActiveTestResultsByTest("9701")).thenReturn(List.of(numericResult()));
        when(testResultService.getActiveTestResultsByTest("9702")).thenReturn(List.of(numericResult()));
        when(testResultService.getActiveTestResultsByTest("9703")).thenReturn(List.of(numericResult()));

        AnalyzerMappingView view = service.getDefaults("site.mock-analyzer", 2);

        assertNull(view.analyzerId());
        assertNull(view.mappingId());
        assertEquals(0, view.mappingRevision());
        AnalyzerMappingView.TestRow first = view.tests().get(0);
        assertEquals(AnalyzerMappingState.BOUND, first.mappingState());
        assertEquals(AnalyzerMappingOrigin.DEFAULT, first.origin());
        assertEquals("9701", first.testId());
        assertEquals("811", first.results().get(0).resultOptionId());
        assertEquals("812", first.results().get(1).resultOptionId());
        AnalyzerMappingView.TestRow ambiguous = view.tests().get(2);
        assertEquals(AnalyzerMappingState.UNRESOLVED, ambiguous.mappingState());
        assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, ambiguous.unresolvedReason());
        verifyZeroInteractions(mappingService, analyzerService);
    }

    @Test
    public void getMappingRejectsAnAnalyzerThatHasNoMapping() {
        Analyzer analyzer = analyzer();
        when(analyzerService.getWithMapping("42")).thenReturn(Optional.of(analyzer));
        when(mappingService.findLatestByAnalyzerId("42")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getMapping("42"));
    }

    @Test
    public void getMappingIncludesHeldQualitativeValuesInTheSharedEditor() throws Exception {
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("RAW-A");
        held.setRawResultValue("INDETERMINATE-VENDOR-X");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
        analyzerWithLatest(currentMapping());
        when(analyzerResultsService.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of(held));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(positiveAndNegative());

        AnalyzerMappingView view = service.getMapping("42");

        assertEquals(3, view.tests().get(0).results().size());
        AnalyzerMappingView.ResultRow observed = view.tests().get(0).results().get(2);
        assertEquals("INDETERMINATE-VENDOR-X", observed.rawValue());
        assertEquals(AnalyzerMappingState.UNRESOLVED, observed.mappingState());
    }

    @Test
    public void getMappingIncludesAnObservedTestAbsentFromTheBridgeProfile() throws Exception {
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("VENDOR-NEW-42");
        held.setRawResultValue("INDETERMINATE");
        held.setResultType("A");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        analyzerWithLatest(currentMapping());
        when(analyzerResultsService.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of(held));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());

        AnalyzerMappingView view = service.getMapping("42");

        assertEquals(4, view.tests().size());
        AnalyzerMappingView.TestRow observed = view.tests().get(3);
        assertEquals("VENDOR-NEW-42", observed.rawCode());
        assertEquals(AnalyzerMappingState.UNRESOLVED, observed.mappingState());
        assertEquals("INDETERMINATE", observed.results().get(0).rawValue());
        assertNull(observed.normalizedCoding());
        verify(mappingService, never()).appendRevision(any(), any(), any());
    }

    @Test
    public void observedNumericTestDoesNotCreateMappingsForIndividualReadings() throws Exception {
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("NEW-NUMERIC");
        held.setRawResultValue("7.5");
        held.setResultType("N");
        held.setUnits("mg/L");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        analyzerWithLatest(currentMapping());
        when(analyzerResultsService.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of(held));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());

        AnalyzerMappingView.TestRow observed = service.getMapping("42").tests().stream()
                .filter(row -> "NEW-NUMERIC".equals(row.rawCode())).findFirst().orElseThrow();

        assertEquals("mg/L", observed.unit());
        assertEquals("N", observed.resultType());
        assertEquals(List.of(), observed.results());
    }

    @Test
    public void saveMappingAppendsARevisionAgainstTheLoadedFingerprintAndMarksOnlyChangedRowsAsOverrides()
            throws Exception {
        AnalyzerMappingSnapshot current = currentMapping();
        AnalyzerMappingSnapshot saved = savedMapping();
        AnalyzerMappingDraft draft = validDraft();
        Analyzer analyzer = savableWithLatest(current);
        when(mappingService.appendRevision(eq(analyzer), any(AnalyzerMappingDraft.class), eq("17"))).thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(positiveAndNegative());

        AnalyzerMappingView view = service.saveMapping("42",
                new AnalyzerMappingUpdate(current.mapping().getMappingFingerprint(), draft.tests(), draft.results()),
                "17");

        assertEquals(5, view.mappingRevision());
        assertEquals("sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                view.mappingFingerprint());
        assertEquals(AnalyzerMappingState.EXCLUDED, view.tests().get(1).mappingState());
        ArgumentCaptor<AnalyzerMappingDraft> savedDraft = ArgumentCaptor.forClass(AnalyzerMappingDraft.class);
        verify(mappingService).appendRevision(eq(analyzer), savedDraft.capture(), eq("17"));
        assertEquals(
                List.of(AnalyzerMappingOrigin.DEFAULT, AnalyzerMappingOrigin.OVERRIDE, AnalyzerMappingOrigin.DEFAULT),
                savedDraft.getValue().tests().stream().map(AnalyzerMappingTestDraft::origin).toList());
        assertEquals(List.of(AnalyzerMappingOrigin.DEFAULT, AnalyzerMappingOrigin.OVERRIDE),
                savedDraft.getValue().results().stream().map(AnalyzerMappingResultDraft::origin).toList());
    }

    @Test
    public void saveMappingAcceptsAnObservedValueOnlyForAProfileDefinedTest() throws Exception {
        AnalyzerMappingSnapshot current = currentMapping();
        AnalyzerMappingDraft base = validDraft();
        AnalyzerMappingDraft withObservedValue = new AnalyzerMappingDraft(base.tests(), List.of(base.results().get(0),
                base.results().get(1),
                new AnalyzerMappingResultDraft("RAW-A", "INDETERMINATE-VENDOR-X", AnalyzerMappingState.BOUND, "811")));
        Analyzer analyzer = savableWithLatest(current);
        when(mappingService.appendRevision(eq(analyzer), any(AnalyzerMappingDraft.class), eq("17")))
                .thenReturn(savedMapping());
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(positiveAndNegative());

        service.saveMapping("42", new AnalyzerMappingUpdate(current.mapping().getMappingFingerprint(),
                withObservedValue.tests(), withObservedValue.results()), "17");

        ArgumentCaptor<AnalyzerMappingDraft> savedDraft = ArgumentCaptor.forClass(AnalyzerMappingDraft.class);
        verify(mappingService).appendRevision(eq(analyzer), savedDraft.capture(), eq("17"));
        assertEquals("INDETERMINATE-VENDOR-X", savedDraft.getValue().results().get(2).rawValue());
        assertEquals(AnalyzerMappingOrigin.OVERRIDE, savedDraft.getValue().results().get(2).origin());
    }

    @Test
    public void saveAndReopenRetainsAnObservedTestAfterItsHeldRowsAreGone() throws Exception {
        AnalyzerMappingSnapshot current = currentMapping();
        AnalyzerMappingSnapshot savedBase = savedMapping();
        List<AnalyzerMappingTest> savedTests = new ArrayList<>(savedBase.tests());
        savedTests.add(test(savedBase.mapping(), "NEW-TEST", AnalyzerMappingState.BOUND, "9701"));
        AnalyzerMappingSnapshot saved = new AnalyzerMappingSnapshot(savedBase.mapping(), savedTests,
                savedBase.results());
        AnalyzerMappingDraft base = validDraft();
        List<AnalyzerMappingTestDraft> tests = new ArrayList<>(base.tests());
        tests.add(new AnalyzerMappingTestDraft("NEW-TEST", AnalyzerMappingState.BOUND, "9701"));
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("NEW-TEST");
        held.setRawResultValue("7.5");
        held.setResultType("N");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        Analyzer analyzer = savableWithLatest(current);
        when(mappingService.appendRevision(eq(analyzer), any(AnalyzerMappingDraft.class), eq("17"))).thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(positiveAndNegative());
        when(analyzerResultsService.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of(held));

        service.saveMapping("42",
                new AnalyzerMappingUpdate(current.mapping().getMappingFingerprint(), tests, base.results()), "17");
        when(analyzerResultsService.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of());
        when(mappingService.findLatestByAnalyzerId("42")).thenReturn(Optional.of(saved));
        AnalyzerMappingView.TestRow reopened = service.getMapping("42").tests().stream()
                .filter(row -> "NEW-TEST".equals(row.rawCode())).findFirst().orElseThrow();

        assertEquals("9701", reopened.selectedTest().id());
        assertEquals(AnalyzerMappingState.BOUND, reopened.mappingState());
        ArgumentCaptor<AnalyzerMappingDraft> draft = ArgumentCaptor.forClass(AnalyzerMappingDraft.class);
        verify(mappingService).appendRevision(eq(analyzer), draft.capture(), eq("17"));
        assertEquals(tests.stream().map(AnalyzerMappingTestDraft::sourceRowKey).toList(),
                draft.getValue().tests().stream().map(AnalyzerMappingTestDraft::sourceRowKey).toList());
    }

    @Test
    public void saveMappingRejectsOmittedOrInventedRowsBeforeAppending() throws Exception {
        AnalyzerMappingSnapshot current = currentMapping();
        String loaded = current.mapping().getMappingFingerprint();
        AnalyzerMappingDraft valid = validDraft();
        AnalyzerMappingUpdate omitted = new AnalyzerMappingUpdate(loaded,
                valid.tests().stream().filter(row -> !"RAW-C".equals(row.sourceRowKey())).toList(), valid.results());
        AnalyzerMappingUpdate invented = new AnalyzerMappingUpdate(loaded,
                List.of(valid.tests().get(0), valid.tests().get(1), valid.tests().get(2),
                        new AnalyzerMappingTestDraft("RAW-X", AnalyzerMappingState.EXCLUDED, null)),
                valid.results());
        savableWithLatest(current);

        assertEquals("Mapping update must retain declared and saved tests and may add only received test codes",
                assertThrows(IllegalArgumentException.class, () -> service.saveMapping("42", omitted, "17"))
                        .getMessage());
        assertEquals("Mapping update must retain declared and saved tests and may add only received test codes",
                assertThrows(IllegalArgumentException.class, () -> service.saveMapping("42", invented, "17"))
                        .getMessage());
        verify(mappingService, never()).appendRevision(any(), any(), any());
    }

    @Test
    public void saveMappingRejectsAStaleLoadedFingerprintBeforeAppending() throws Exception {
        AnalyzerMappingDraft draft = validDraft();
        savableWithLatest(currentMapping());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveMapping("42",
                        new AnalyzerMappingUpdate(
                                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                                draft.tests(), draft.results()),
                        "17"));

        assertEquals("The analyzer's mapping changed after this editor was loaded", error.getMessage());
        verify(mappingService, never()).appendRevision(any(), any(), any());
    }

    @Test
    public void suggestsExactlyWhatTheResolverResolvesForTheSameCatalog() throws Exception {
        unresolvedMapping();
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(coveredAnswers());
        when(testResultService.getActiveTestResultsByTest("9701")).thenReturn(List.of(numericResult()));
        when(testResultService.getActiveTestResultsByTest("9702")).thenReturn(List.of(numericResult()));
        when(testResultService.getActiveTestResultsByTest("9703")).thenReturn(List.of(numericResult()));

        AnalyzerMappingView view = service.getMapping("42");

        assertEquals("9701", view.tests().get(0).suggestedTest().id());
        assertEquals("811", view.tests().get(0).results().get(0).suggestedOption().id());
        assertEquals("812", view.tests().get(0).results().get(1).suggestedOption().id());
        assertEquals("9701", view.tests().get(1).suggestedTest().id());
        assertNull(view.tests().get(2).suggestedTest());
        assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, view.tests().get(2).unresolvedReason());
        AnalyzerMappingDraft resolved = new AnalyzerMappingDefaults(mappingCatalogService, testResultService)
                .resolve(BridgeAnalyzerProfile.from(profileRevision().profile()));
        for (int row = 0; row < resolved.tests().size(); row++) {
            var editor = view.tests().get(row);
            assertEquals(resolved.tests().get(row).testId(),
                    editor.suggestedTest() == null ? null : editor.suggestedTest().id());
            assertEquals(resolved.tests().get(row).unresolvedReason(), editor.unresolvedReason());
        }
    }

    @Test
    public void doesNotSuggestATestThatOnlySharesTheRawCodeOrName() throws Exception {
        unresolvedMapping();
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of(
                new AnalyzerMappingCatalogService.TestOption("9801", "Second result", "RAW-B", List.of("12345-6"))));

        AnalyzerMappingView view = service.getMapping("42");

        assertNull(view.tests().get(1).suggestedTest());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, view.tests().get(1).unresolvedReason());
    }

    @Test
    public void everyUnresolvedAnswerCarriesItsReasonWhenTheLocalAnswersCarryNoCode() throws Exception {
        unresolvedMapping();
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(List.of(positive()));

        AnalyzerMappingView view = service.getMapping("42");

        assertEquals("9701", view.tests().get(0).suggestedTest().id());
        for (var answer : view.tests().get(0).results()) {
            assertNull(answer.suggestedOption());
            assertEquals(AnalyzerUnresolvedReason.NO_MATCH, answer.unresolvedReason());
        }
    }

    @Test
    public void getDefaultsShowsEachRecordOfATestAsItsOwnRow() throws Exception {
        when(bridgeProfileCatalogService.getProfile("site.viral-load", 1)).thenReturn(viralLoadRevision());
        viralLoadCatalog();

        AnalyzerMappingView view = service.getDefaults("site.viral-load", 1);

        AnalyzerMappingView.TestRow main = recordRow(view, "");
        assertEquals("9701", main.testId());
        assertNull(main.componentId());
        assertEquals("comp-call", main.callComponentId());
        assertEquals("opt-detected", main.results().get(0).resultOptionId());
        AnalyzerMappingView.TestRow log = recordRow(view, "&LOG");
        assertEquals("HIVVL", log.rawCode());
        assertEquals("comp-LOG", log.componentId());
        assertEquals("call", main.callComponentCode());
        assertNull(main.componentCode());
        assertEquals("LOG", log.componentCode());
        assertNull(log.callComponentCode());
    }

    @Test
    public void aMappingWithTwoRecordsUnderOneCodeOpensAndSavesWithoutLosingEither() throws Exception {
        AnalyzerMapping mapping = revision("71", 1, "sha256:" + "f".repeat(64));
        mapping.setProfileId("site.viral-load");
        mapping.setProfileRevision(1);
        AnalyzerMappingSnapshot latest = new AnalyzerMappingSnapshot(mapping,
                List.of(testRow(mapping, "", null, "comp-call"), testRow(mapping, "&LOG", "comp-LOG", null)),
                List.of());
        Analyzer analyzer = analyzer();
        analyzer.setMapping(mapping);
        when(analyzerService.getWithMapping("42")).thenReturn(Optional.of(analyzer));
        when(analyzerService.findByIdForUpdate("42")).thenReturn(Optional.of(analyzer));
        when(mappingService.findLatestByAnalyzerId("42")).thenReturn(Optional.of(latest));
        when(bridgeProfileCatalogService.getProfile("site.viral-load", 1)).thenReturn(viralLoadRevision());
        when(mappingService.appendRevision(eq(analyzer), any(AnalyzerMappingDraft.class), eq("17"))).thenReturn(latest);
        viralLoadCatalog();

        AnalyzerMappingView view = service.getMapping("42");
        assertEquals("comp-LOG", recordRow(view, "&LOG").componentId());

        service.saveMapping("42",
                new AnalyzerMappingUpdate(mapping.getMappingFingerprint(),
                        List.of(new AnalyzerMappingTestDraft(
                                "HIVVL", AnalyzerMappingState.BOUND, "9701", null, null, null, "", "comp-call"),
                                new AnalyzerMappingTestDraft("HIVVL", AnalyzerMappingState.BOUND, "9701", "comp-LOG",
                                        null, null, "&LOG", null)),
                        List.of(new AnalyzerMappingResultDraft("HIVVL", "DETECTED", AnalyzerMappingState.UNRESOLVED,
                                null),
                                new AnalyzerMappingResultDraft("HIVVL", "DÉTECTÉ", AnalyzerMappingState.UNRESOLVED,
                                        null))),
                "17");

        ArgumentCaptor<AnalyzerMappingDraft> saved = ArgumentCaptor.forClass(AnalyzerMappingDraft.class);
        verify(mappingService).appendRevision(eq(analyzer), saved.capture(), eq("17"));
        assertEquals(List.of("", "&LOG"),
                saved.getValue().tests().stream().map(AnalyzerMappingTestDraft::subIdentity).toList());
        assertEquals("comp-call", saved.getValue().tests().get(0).callComponentId());
    }

    @Test
    public void aRecordWhoseComponentIsUnknownIsOfferedNoAnswerFromAnotherComponent() throws Exception {
        when(bridgeProfileCatalogService.getProfile("site.viral-load", 1)).thenReturn(viralLoadRevision());
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(
                List.of(new AnalyzerMappingCatalogService.TestOption("9701", "HIV-1 viral load", null,
                        List.of("20447-9"))));
        when(testResultService.getActiveTestResultsByTest("9701")).thenReturn(List.of(numericResult()));
        when(mappingCatalogService.getActiveComponents("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ComponentOption("comp-LOG", "LOG", "Log viral load")));
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("opt-detected", "1301", "Detected", "LA11882-0",
                        "comp-qualitative")));

        AnalyzerMappingView.TestRow main = recordRow(service.getDefaults("site.viral-load", 1), "");

        assertEquals(AnalyzerMappingState.UNRESOLVED, main.mappingState());
        assertNull(main.results().get(0).suggestedOption());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, main.results().get(0).unresolvedReason());
    }

    @Test
    public void aHeldRecordWithASubIdentityShowsAsItsOwnRowAndCanBeAdded() throws Exception {
        AnalyzerMapping mapping = revision("71", 1, "sha256:" + "f".repeat(64));
        mapping.setProfileId("site.viral-load");
        mapping.setProfileRevision(1);
        AnalyzerMappingSnapshot latest = new AnalyzerMappingSnapshot(mapping,
                List.of(testRow(mapping, "", null, "comp-call"), testRow(mapping, "&LOG", "comp-LOG", null)),
                List.of());
        Analyzer analyzer = analyzer();
        analyzer.setMapping(mapping);
        when(analyzerService.getWithMapping("42")).thenReturn(Optional.of(analyzer));
        when(analyzerService.findByIdForUpdate("42")).thenReturn(Optional.of(analyzer));
        when(mappingService.findLatestByAnalyzerId("42")).thenReturn(Optional.of(latest));
        when(bridgeProfileCatalogService.getProfile("site.viral-load", 1)).thenReturn(viralLoadRevision());
        when(mappingService.appendRevision(eq(analyzer), any(AnalyzerMappingDraft.class), eq("17"))).thenReturn(latest);
        viralLoadCatalog();
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("HIVVL");
        held.setRawSubIdentity("HIV-1&EndPt");
        held.setRawResultValue("257.0");
        held.setResultType("N");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        when(analyzerResultsService.findHeldMappingResultsByAnalyzer("42")).thenReturn(List.of(held));

        AnalyzerMappingView view = service.getMapping("42");

        assertEquals(3, view.tests().size());
        assertEquals(AnalyzerMappingState.UNRESOLVED, recordRow(view, "HIV-1&EndPt").mappingState());
        assertEquals("comp-call", recordRow(view, "").callComponentId());

        service.saveMapping("42",
                new AnalyzerMappingUpdate(mapping.getMappingFingerprint(),
                        List.of(new AnalyzerMappingTestDraft(
                                "HIVVL", AnalyzerMappingState.BOUND, "9701", null, null, null, "", "comp-call"),
                                new AnalyzerMappingTestDraft(
                                        "HIVVL", AnalyzerMappingState.BOUND, "9701", "comp-LOG", null, null, "&LOG",
                                        null),
                                new AnalyzerMappingTestDraft("HIVVL", AnalyzerMappingState.EXCLUDED, null, null, null,
                                        null, "HIV-1&EndPt", null)),
                        List.of(new AnalyzerMappingResultDraft("HIVVL", "DETECTED", AnalyzerMappingState.UNRESOLVED,
                                null),
                                new AnalyzerMappingResultDraft("HIVVL", "DÉTECTÉ", AnalyzerMappingState.UNRESOLVED,
                                        null))),
                "17");

        ArgumentCaptor<AnalyzerMappingDraft> saved = ArgumentCaptor.forClass(AnalyzerMappingDraft.class);
        verify(mappingService).appendRevision(eq(analyzer), saved.capture(), eq("17"));
        assertEquals(AnalyzerMappingOrigin.OVERRIDE, saved.getValue().tests().get(2).origin());
    }

    private void viralLoadCatalog() {
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(
                List.of(new AnalyzerMappingCatalogService.TestOption("9701", "HIV-1 viral load", null,
                        List.of("20447-9"))));
        when(testResultService.getActiveTestResultsByTest("9701")).thenReturn(List.of(numericResult()));
        when(mappingCatalogService.getActiveComponents("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ComponentOption("comp-call", "call", "Call"),
                        new AnalyzerMappingCatalogService.ComponentOption("comp-LOG", "LOG", "Log viral load")));
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(List.of(
                new AnalyzerMappingCatalogService.ResultOption("opt-detected", "1301", "Detected", "LA11882-0",
                        "comp-call")));
    }

    private BridgeProfileCatalog.ProfileRevision viralLoadRevision() throws Exception {
        JsonNode profile = objectMapper.readTree("""
                {
                  "profileMeta":{"id":"site.viral-load","displayName":"Viral load"},
                  "protocol":{"name":"ASTM"},
                  "catalog":{
                    "revision":1,
                    "revisionFingerprint":"sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                    "source":"SHIPPED",
                    "status":"ACTIVE"
                  },
                  "default_test_mappings":[
                    {
                      "test_code":"HIVVL",
                      "loinc":"20447-9",
                      "unit":"copies/mL",
                      "result_type":"quantitative",
                      "values":["DETECTED"],
                      "value_codes":{"DETECTED":{"system":"http://loinc.org","code":"LA11882-0"}},
                      "translations":{"DETECTED":["DÉTECTÉ"]},
                      "call_component":"call",
                      "components":[
                        {"code":"call","result_type":"qualitative"},
                        {"code":"LOG","sub_identity":"&LOG","result_type":"quantitative","unit":"log copies/mL"}
                      ]
                    }
                  ]
                }
                """);
        BridgeProfileCatalog.ControlRecognitionSummary recognition = new BridgeProfileCatalog.ControlRecognitionSummary(
                recognitionFingerprint(), "RULES", "Control results match any configured condition.", false, List.of());
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode(), recognition);
    }

    private static org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest testRow(AnalyzerMapping mapping,
            String subIdentity, String componentId, String callComponentId) {
        var row = new org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest();
        row.setId(new org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK(mapping.getId(), "HIVVL",
                subIdentity));
        row.setMapping(mapping);
        row.setMappingState(AnalyzerMappingState.BOUND);
        row.setOrigin(AnalyzerMappingOrigin.DEFAULT);
        row.setTestId("9701");
        row.setComponentId(componentId);
        row.setCallComponentId(callComponentId);
        return row;
    }

    private static AnalyzerMappingView.TestRow recordRow(AnalyzerMappingView view, String subIdentity) {
        return view.tests().stream().filter(row -> subIdentity.equals(row.subIdentity())).findFirst()
                .orElseThrow(() -> new AssertionError("no editor row for '" + subIdentity + "'"));
    }

    private Analyzer savableWithLatest(AnalyzerMappingSnapshot latest) throws Exception {
        Analyzer analyzer = analyzerWithLatest(latest);
        when(analyzerService.findByIdForUpdate("42")).thenReturn(Optional.of(analyzer));
        return analyzer;
    }

    private Analyzer analyzerWithLatest(AnalyzerMappingSnapshot latest) throws Exception {
        Analyzer analyzer = analyzer();
        analyzer.setMapping(latest.mapping());
        when(analyzerService.getWithMapping("42")).thenReturn(Optional.of(analyzer));
        when(mappingService.findLatestByAnalyzerId("42")).thenReturn(Optional.of(latest));
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        return analyzer;
    }

    /**
     * A saved revision whose rows were never resolved, so the editor can only
     * suggest.
     */
    private void unresolvedMapping() throws Exception {
        AnalyzerMapping revision = revision("61", 4, "sha256:" + "b".repeat(64));
        analyzerWithLatest(new AnalyzerMappingSnapshot(revision,
                List.of(test(revision, "RAW-A", AnalyzerMappingState.UNRESOLVED, null),
                        test(revision, "RAW-B", AnalyzerMappingState.UNRESOLVED, null),
                        test(revision, "RAW-C", AnalyzerMappingState.UNRESOLVED, null)),
                List.of()));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
    }

    private static Analyzer analyzer() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("42");
        analyzer.setName("Mock analyzer");
        return analyzer;
    }

    private static AnalyzerMappingCatalogService.ResultOption positive() {
        return new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive", null);
    }

    private static List<AnalyzerMappingCatalogService.ResultOption> positiveAndNegative() {
        return List.of(positive(), new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative", null));
    }

    private static List<AnalyzerMappingCatalogService.ResultOption> coveredAnswers() {
        return List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Reactive", "LA6576-8"),
                new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Non-reactive", "LA6577-6"));
    }

    private static TestResult numericResult() {
        TestResult number = new TestResult();
        number.setTestResultType("N");
        return number;
    }

    private BridgeProfileCatalog.ProfileRevision profileRevision() throws Exception {
        JsonNode profile = objectMapper.readTree("""
                {
                  "profileMeta":{"id":"site.mock-analyzer","displayName":"Mock Analyzer"},
                  "protocol":{"name":"ASTM","version":"LIS2-A2"},
                  "transport":["TCP/IP"],
                  "communication":{"mode":"ANALYZER_INITIATED","supports_lis_initiated":false},
                  "capabilities":{"inboundResults":true,"outboundOrders":false,"connectionTest":true},
                  "configDefaults":{"connectionRole":"SERVER","transport":"TCP/IP","aggregationMode":"PER_MESSAGE"},
                  "catalog":{
                    "revision":2,
                    "revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "source":"SITE",
                    "status":"ACTIVE"
                  },
                  "default_test_mappings":[
                    {
                      "test_code":"RAW-A",
                      "aliases":["RAW-A1","RAW-A2"],
                      "test_name_hint":"First result",
                      "loinc":"94500-6",
                      "unit":"copies/mL",
                      "result_type":"qualitative",
                      "values":["POS","NEG"],
                      "value_codes":{
                        "POS":{"system":"http://loinc.org","code":"LA6576-8"},
                        "NEG":{"system":"http://loinc.org","code":"LA6577-6"}
                      },
                      "normalized_coding":{
                        "system":"https://loinc.org",
                        "code":"94500-6",
                        "display":"SARS-CoV-2 RNA"
                      }
                    },
                    {
                      "test_code":"RAW-B",
                      "test_name_hint":"Second result",
                      "loinc":"94500-6",
                      "result_type":"quantitative"
                    },
                    {
                      "test_code":"RAW-C",
                      "test_name_hint":"Ambiguous result",
                      "loinc":"77777-7",
                      "result_type":"quantitative"
                    }
                  ]
                }
                """);
        BridgeProfileCatalog.ControlRecognitionSummary recognition = new BridgeProfileCatalog.ControlRecognitionSummary(
                recognitionFingerprint(), "RULES", "Control results match any configured condition.", false,
                List.of(new BridgeProfileCatalog.ControlRecognitionSummary.Condition("qc-prefix",
                        "SPECIMEN_ID_STARTS_WITH", "Specimen ID", "QC-", "Specimen ID starts with QC-", "QC", null)));
        return new BridgeProfileCatalog.ProfileRevision(profile, JsonNodeFactory.instance.objectNode(), recognition);
    }

    private static String recognitionFingerprint() {
        return "sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
    }

    private static AnalyzerMapping revision(String id, int number, String fingerprint) {
        AnalyzerMapping revision = new AnalyzerMapping();
        revision.setId(id);
        revision.setRevisionNumber(number);
        revision.setProfileId("site.mock-analyzer");
        revision.setProfileRevision(2);
        revision.setProfileFingerprint("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        revision.setMappingFingerprint(fingerprint);
        return revision;
    }

    /**
     * RAW-A bound by default, its NEG answer excluded by the operator, the rest
     * unresolved.
     */
    private static AnalyzerMappingSnapshot currentMapping() {
        AnalyzerMapping revision = revision("61", 4,
                "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        AnalyzerMappingResult excluded = result(revision, "RAW-A", "NEG", AnalyzerMappingState.EXCLUDED, null);
        excluded.setOrigin(AnalyzerMappingOrigin.OVERRIDE);
        return new AnalyzerMappingSnapshot(revision,
                List.of(test(revision, "RAW-A", AnalyzerMappingState.BOUND, "9701"),
                        test(revision, "RAW-B", AnalyzerMappingState.UNRESOLVED, null),
                        test(revision, "RAW-C", AnalyzerMappingState.UNRESOLVED, null)),
                List.of(result(revision, "RAW-A", "POS", AnalyzerMappingState.BOUND, "811"), excluded));
    }

    private static AnalyzerMappingSnapshot savedMapping() {
        AnalyzerMapping revision = revision("62", 5,
                "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        return new AnalyzerMappingSnapshot(revision,
                List.of(test(revision, "RAW-A", AnalyzerMappingState.BOUND, "9701"),
                        test(revision, "RAW-B", AnalyzerMappingState.EXCLUDED, null),
                        test(revision, "RAW-C", AnalyzerMappingState.UNRESOLVED, null)),
                List.of(result(revision, "RAW-A", "POS", AnalyzerMappingState.BOUND, "811"),
                        result(revision, "RAW-A", "NEG", AnalyzerMappingState.EXCLUDED, null)));
    }

    private static AnalyzerMappingTest test(AnalyzerMapping revision, String sourceRowKey, AnalyzerMappingState state,
            String testId) {
        AnalyzerMappingTest row = new AnalyzerMappingTest();
        row.setId(new AnalyzerMappingTestPK(revision.getId(), sourceRowKey));
        row.setMapping(revision);
        row.setMappingState(state);
        row.setTestId(testId);
        return row;
    }

    private static AnalyzerMappingResult result(AnalyzerMapping revision, String sourceRowKey, String rawValue,
            AnalyzerMappingState state, String optionId) {
        AnalyzerMappingResult row = new AnalyzerMappingResult();
        row.setId(new AnalyzerMappingResultPK(revision.getId(), sourceRowKey, rawValue));
        row.setMapping(revision);
        row.setMappingState(state);
        row.setTestResultId(optionId);
        return row;
    }

    /** The edit that excludes RAW-B and leaves every other row as it was. */
    private static AnalyzerMappingDraft validDraft() {
        return new AnalyzerMappingDraft(
                List.of(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.BOUND, "9701"),
                        new AnalyzerMappingTestDraft("RAW-B", AnalyzerMappingState.EXCLUDED, null),
                        new AnalyzerMappingTestDraft("RAW-C", AnalyzerMappingState.UNRESOLVED, null)),
                List.of(new AnalyzerMappingResultDraft("RAW-A", "POS", AnalyzerMappingState.BOUND, "811"),
                        new AnalyzerMappingResultDraft("RAW-A", "NEG", AnalyzerMappingState.EXCLUDED, null)));
    }

    private static List<AnalyzerMappingCatalogService.TestOption> activeTests() {
        return List.of(
                new AnalyzerMappingCatalogService.TestOption("9701", "SARS-CoV-2 RNA", "COVID19", List.of("94500-6")),
                new AnalyzerMappingCatalogService.TestOption("9702", "Ambiguous one", "AMB-1", List.of("77777-7")),
                new AnalyzerMappingCatalogService.TestOption("9703", "Ambiguous two", "AMB-2", List.of("77777-7")));
    }
}
