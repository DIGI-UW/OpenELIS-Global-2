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
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResultPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTestPK;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingEditorServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BridgeProfileCatalogService bridgeProfileCatalogService;

    @Mock
    private AnalyzerProfileBindingDAO profileBindingDAO;

    @Mock
    private AnalyzerMappingService siteBindingService;

    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;

    @Mock
    private AnalyzerProfileBindingService profileBindingService;

    @Mock
    private AnalyzerMappingConfirmationService confirmationService;

    @Mock
    private AnalyzerResultsService analyzerResultsService;

    @Mock
    private TestResultService testResultService;

    private AnalyzerMappingEditorService service;

    @Before
    public void setUp() {
        service = new AnalyzerMappingEditorServiceImpl(bridgeProfileCatalogService, profileBindingDAO,
                siteBindingService, mappingCatalogService, profileBindingService, confirmationService,
                analyzerResultsService, new AnalyzerMappingDefaults(mappingCatalogService, testResultService));
    }

    @Test
    public void getMappingPreservesEverySourceRowAndHydratesCurrentLocalChoices() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerMappingSnapshot siteBinding = siteBinding(profileBinding);
        AnalyzerMappingConfirmationView confirmation = new AnalyzerMappingConfirmationView(
                AnalyzerMappingConfirmationView.State.STALE, "site.mock-analyzer", 2,
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", recognitionFingerprint(),
                "16", "Grace Hopper", null, List.of(), List.of());
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(siteBinding));
        when(confirmationService.getStatus(siteBinding, recognitionFingerprint())).thenReturn(confirmation);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(testResultService.getActiveTestResultsByTest("9701")).thenReturn(List.of(numericResult()));
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        AnalyzerMappingView view = service.getMapping("site.mock-analyzer", 2);

        assertEquals("site.mock-analyzer", view.profileId());
        assertEquals(2, view.profileRevision());
        assertEquals("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                view.profileFingerprint());
        assertEquals("51", view.siteBindingId());
        assertEquals(4, view.siteBindingRevision());
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
        assertEquals("9701", first.testId());
        assertEquals("SARS-CoV-2 RNA", first.selectedTest().name());
        assertEquals(2, first.results().size());
        assertEquals("POS", first.results().get(0).rawValue());
        assertEquals("811", first.results().get(0).resultOptionId());
        assertEquals("Positive", first.results().get(0).selectedOption().label());
        assertEquals(AnalyzerMappingState.EXCLUDED, first.results().get(1).mappingState());

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
        verify(profileBindingDAO).findByProfileIdAndRevision("site.mock-analyzer", 2);
        verify(siteBindingService).findCurrentByProfileBindingId("41");
    }

    @Test
    public void confirmMappingKeepsUnresolvedRowsSeparateFromConfirmedDecisions() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerMappingSnapshot candidate = siteBinding(profileBinding);
        AnalyzerMappingConfirmationRequest request = new AnalyzerMappingConfirmationRequest(
                candidate.revision().getBindingFingerprint(), recognitionFingerprint(),
                List.of(new AnalyzerMappingSourceRow("RAW-A", null), new AnalyzerMappingSourceRow("RAW-A", "POS")),
                List.of(new AnalyzerMappingSourceRow("RAW-A", "NEG")));
        AnalyzerMappingConfirmationView expected = AnalyzerMappingConfirmationView.unconfirmed();
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(candidate));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive")));
        when(confirmationService.confirm(candidate, recognitionFingerprint(), request, "17")).thenReturn(expected);

        AnalyzerMappingConfirmationView confirmed = service.confirmMapping("site.mock-analyzer", 2, request, "17");

        assertEquals(expected, confirmed);
        verify(confirmationService).confirm(candidate, recognitionFingerprint(), request, "17");
    }

    @Test
    public void getMappingReturnsUnresolvedRowsWithoutCreatingLocalState() throws Exception {
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2)).thenReturn(Optional.empty());
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(coveredAnswers());

        AnalyzerMappingView view = service.getMapping("site.mock-analyzer", 2);

        assertNull(view.siteBindingId());
        assertEquals(0, view.siteBindingRevision());
        assertEquals(AnalyzerMappingState.UNRESOLVED, view.tests().get(0).mappingState());
        assertEquals(AnalyzerMappingState.UNRESOLVED,
                view.tests().get(0).results().get(0).mappingState());
        assertEquals("9701", view.tests().get(0).suggestedTest().id());
    }

    @Test
    public void getMappingIncludesHeldQualitativeValuesInTheSharedEditor() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerMappingSnapshot siteBinding = siteBinding(profileBinding);
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("RAW-A");
        held.setRawResultValue("INDETERMINATE-VENDOR-X");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(siteBinding));
        when(analyzerResultsService.findHeldMappingResultsByProfile("site.mock-analyzer", 2)).thenReturn(List.of(held));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        AnalyzerMappingView view = service.getMapping("site.mock-analyzer", 2);

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
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2)).thenReturn(Optional.empty());
        when(analyzerResultsService.findHeldMappingResultsByProfile("site.mock-analyzer", 2)).thenReturn(List.of(held));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());

        AnalyzerMappingView view = service.getMapping("site.mock-analyzer", 2);

        assertEquals(4, view.tests().size());
        AnalyzerMappingView.TestRow observed = view.tests().get(3);
        assertEquals("VENDOR-NEW-42", observed.rawCode());
        assertEquals(AnalyzerMappingState.UNRESOLVED, observed.mappingState());
        assertEquals("INDETERMINATE", observed.results().get(0).rawValue());
        assertNull(observed.normalizedCoding());
        verifyZeroInteractions(profileBindingService);
    }

    @Test
    public void observedNumericTestDoesNotCreateMappingsForIndividualReadings() throws Exception {
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("NEW-NUMERIC");
        held.setRawResultValue("7.5");
        held.setResultType("N");
        held.setUnits("mg/L");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2)).thenReturn(Optional.empty());
        when(analyzerResultsService.findHeldMappingResultsByProfile("site.mock-analyzer", 2)).thenReturn(List.of(held));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());

        AnalyzerMappingView.TestRow observed = service.getMapping("site.mock-analyzer", 2).tests().stream()
                .filter(row -> "NEW-NUMERIC".equals(row.rawCode())).findFirst().orElseThrow();

        assertEquals("mg/L", observed.unit());
        assertEquals("N", observed.resultType());
        assertEquals(List.of(), observed.results());
    }

    @Test
    public void saveMappingAppendsAnAuditedRevisionAgainstTheLoadedFingerprint() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerMappingSnapshot current = siteBinding(profileBinding);
        AnalyzerMappingSnapshot saved = savedSiteBinding(current.binding());
        AnalyzerMappingDraft draft = validDraft();
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(current));
        when(profileBindingService.resolveActiveRevision("site.mock-analyzer", 2, "17")).thenReturn(profileBinding);
        when(siteBindingService.appendRevision(eq(current.binding()), any(AnalyzerMappingDraft.class), eq("17")))
                .thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        AnalyzerMappingView view = service.saveMapping("site.mock-analyzer", 2,
                new AnalyzerMappingUpdate(current.revision().getBindingFingerprint(), draft.tests(), draft.results()),
                "17");

        assertEquals(5, view.siteBindingRevision());
        assertEquals("sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                view.bindingFingerprint());
        assertEquals(AnalyzerMappingState.EXCLUDED, view.tests().get(1).mappingState());
        ArgumentCaptor<AnalyzerMappingDraft> savedDraft = ArgumentCaptor.forClass(AnalyzerMappingDraft.class);
        verify(siteBindingService).appendRevision(eq(current.binding()), savedDraft.capture(), eq("17"));
        assertEquals(draft, savedDraft.getValue());
    }

    @Test
    public void saveMappingAcceptsAnObservedValueOnlyForAProfileDefinedTest() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerMappingSnapshot current = siteBinding(profileBinding);
        AnalyzerMappingDraft base = validDraft();
        AnalyzerMappingDraft withObservedValue = new AnalyzerMappingDraft(base.tests(), List.of(base.results().get(0),
                base.results().get(1),
                new AnalyzerMappingResultDraft("RAW-A", "INDETERMINATE-VENDOR-X", AnalyzerMappingState.BOUND, "811")));
        AnalyzerMappingSnapshot saved = savedSiteBinding(current.binding());
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(current));
        when(profileBindingService.resolveActiveRevision("site.mock-analyzer", 2, "17")).thenReturn(profileBinding);
        when(siteBindingService.appendRevision(eq(current.binding()), any(AnalyzerMappingDraft.class), eq("17")))
                .thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        service.saveMapping("site.mock-analyzer", 2,
                new AnalyzerMappingUpdate(current.revision().getBindingFingerprint(), withObservedValue.tests(),
                        withObservedValue.results()),
                "17");

        ArgumentCaptor<AnalyzerMappingDraft> savedDraft = ArgumentCaptor.forClass(AnalyzerMappingDraft.class);
        verify(siteBindingService).appendRevision(eq(current.binding()), savedDraft.capture(), eq("17"));
        assertEquals(withObservedValue, savedDraft.getValue());
    }

    @Test
    public void saveAndReopenRetainsAnObservedTestAfterItsHeldRowsAreGone() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerMappingSnapshot current = siteBinding(profileBinding);
        AnalyzerMappingSnapshot savedBase = savedSiteBinding(current.binding());
        List<AnalyzerMappingTest> savedTests = new ArrayList<>(savedBase.tests());
        savedTests.add(test(savedBase.revision(), "NEW-TEST", AnalyzerMappingState.BOUND, "9701"));
        AnalyzerMappingSnapshot saved = new AnalyzerMappingSnapshot(savedBase.binding(), savedBase.revision(),
                savedTests, savedBase.results());
        AnalyzerMappingDraft base = validDraft();
        List<AnalyzerMappingTestDraft> tests = new ArrayList<>(base.tests());
        tests.add(new AnalyzerMappingTestDraft("NEW-TEST", AnalyzerMappingState.BOUND, "9701"));
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("NEW-TEST");
        held.setRawResultValue("7.5");
        held.setResultType("N");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(current));
        when(profileBindingService.resolveActiveRevision("site.mock-analyzer", 2, "17")).thenReturn(profileBinding);
        when(siteBindingService.appendRevision(eq(current.binding()), any(AnalyzerMappingDraft.class), eq("17")))
                .thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));
        when(analyzerResultsService.findHeldMappingResultsByProfile("site.mock-analyzer", 2)).thenReturn(List.of(held));

        service.saveMapping("site.mock-analyzer", 2,
                new AnalyzerMappingUpdate(current.revision().getBindingFingerprint(), tests, base.results()), "17");
        when(analyzerResultsService.findHeldMappingResultsByProfile("site.mock-analyzer", 2)).thenReturn(List.of());
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(saved));
        AnalyzerMappingView.TestRow reopened = service.getMapping("site.mock-analyzer", 2).tests().stream()
                .filter(row -> "NEW-TEST".equals(row.rawCode())).findFirst().orElseThrow();

        assertEquals("9701", reopened.selectedTest().id());
        assertEquals(AnalyzerMappingState.BOUND, reopened.mappingState());
        ArgumentCaptor<AnalyzerMappingDraft> draft = ArgumentCaptor.forClass(AnalyzerMappingDraft.class);
        verify(siteBindingService).appendRevision(eq(current.binding()), draft.capture(), eq("17"));
        assertEquals(tests, draft.getValue().tests());
    }

    @Test
    public void saveMappingCreatesTheSharedBindingForAnUnusedAnalyzerType() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerMappingSnapshot initial = siteBinding(profileBinding);
        AnalyzerMappingSnapshot saved = savedSiteBinding(initial.binding());
        AnalyzerMappingDraft draft = validDraft();
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2)).thenReturn(Optional.empty());
        when(profileBindingService.resolveActiveRevision("site.mock-analyzer", 2, "17")).thenReturn(profileBinding);
        when(siteBindingService.resolveInitialRevision(eq(profileBinding), any(JsonNode.class), eq("17")))
                .thenReturn(initial);
        when(siteBindingService.appendRevision(eq(initial.binding()), any(AnalyzerMappingDraft.class), eq("17")))
                .thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        AnalyzerMappingView view = service.saveMapping("site.mock-analyzer", 2,
                new AnalyzerMappingUpdate(null, draft.tests(), draft.results()), "17");

        assertEquals("51", view.siteBindingId());
        assertEquals(5, view.siteBindingRevision());
        verify(siteBindingService).resolveInitialRevision(eq(profileBinding), any(JsonNode.class), eq("17"));
        verify(siteBindingService).appendRevision(eq(initial.binding()), eq(draft), eq("17"));
    }

    @Test
    public void saveMappingRejectsOmittedOrInventedRowsBeforeCreatingLocalState() throws Exception {
        AnalyzerMappingDraft valid = validDraft();
        AnalyzerMappingUpdate omitted = new AnalyzerMappingUpdate(null,
                valid.tests().stream().filter(row -> !"RAW-C".equals(row.sourceRowKey())).toList(), valid.results());
        AnalyzerMappingUpdate invented = new AnalyzerMappingUpdate(null,
                List.of(valid.tests().get(0), valid.tests().get(1), valid.tests().get(2),
                        new AnalyzerMappingTestDraft("RAW-X", AnalyzerMappingState.EXCLUDED, null)),
                valid.results());
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());

        assertEquals("Mapping update must retain declared and saved tests and may add only received test codes",
                assertThrows(IllegalArgumentException.class,
                        () -> service.saveMapping("site.mock-analyzer", 2, omitted, "17")).getMessage());
        assertEquals("Mapping update must retain declared and saved tests and may add only received test codes",
                assertThrows(IllegalArgumentException.class,
                        () -> service.saveMapping("site.mock-analyzer", 2, invented, "17")).getMessage());
        verifyZeroInteractions(profileBindingService, siteBindingService);
    }

    @Test
    public void saveMappingRejectsAStaleLoadedFingerprintBeforeAppending() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerMappingSnapshot current = siteBinding(profileBinding);
        AnalyzerMappingDraft draft = validDraft();
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(current));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveMapping("site.mock-analyzer", 2,
                        new AnalyzerMappingUpdate(
                                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                                draft.tests(), draft.results()),
                        "17"));

        assertEquals("Analyzer Type mappings changed after this editor was loaded", error.getMessage());
        verifyZeroInteractions(profileBindingService);
        verify(siteBindingService, never()).appendRevision(any(), any(), any());
    }

    @Test
    public void suggestsExactlyWhatTheResolverResolvesForTheSameCatalog() throws Exception {
        unboundMapping();
        when(mappingCatalogService.getActiveResultOptions("9701")).thenReturn(coveredAnswers());
        when(testResultService.getActiveTestResultsByTest("9701")).thenReturn(List.of(numericResult()));
        when(testResultService.getActiveTestResultsByTest("9702")).thenReturn(List.of(numericResult()));
        when(testResultService.getActiveTestResultsByTest("9703")).thenReturn(List.of(numericResult()));

        AnalyzerMappingView view = service.getMapping("site.mock-analyzer", 2);

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
        unboundMapping();
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(List.of(
                new AnalyzerMappingCatalogService.TestOption("9801", "Second result", "RAW-B", List.of("12345-6"))));

        AnalyzerMappingView view = service.getMapping("site.mock-analyzer", 2);

        assertNull(view.tests().get(1).suggestedTest());
        assertEquals(AnalyzerUnresolvedReason.NO_MATCH, view.tests().get(1).unresolvedReason());
    }

    @Test
    public void everyUnresolvedAnswerCarriesItsReasonWhenTheLocalAnswersCarryNoCode() throws Exception {
        unboundMapping();
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive")));

        AnalyzerMappingView view = service.getMapping("site.mock-analyzer", 2);

        assertEquals("9701", view.tests().get(0).suggestedTest().id());
        for (var answer : view.tests().get(0).results()) {
            assertNull(answer.suggestedOption());
            assertEquals(AnalyzerUnresolvedReason.NO_MATCH, answer.unresolvedReason());
        }
    }

    private void unboundMapping() throws Exception {
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2)).thenReturn(Optional.empty());
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
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

    private AnalyzerProfileBinding profileBinding() {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setId("41");
        binding.setProfileId("site.mock-analyzer");
        binding.setProfileRevision(2);
        binding.setProfileFingerprint("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        return binding;
    }

    private AnalyzerMappingSnapshot siteBinding(AnalyzerProfileBinding profileBinding) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("51");
        binding.setProfileBinding(profileBinding);
        AnalyzerMapping revision = new AnalyzerMapping();
        revision.setId("61");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(4);
        revision.setBindingFingerprint("sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        return new AnalyzerMappingSnapshot(binding, revision,
                List.of(test(revision, "RAW-A", AnalyzerMappingState.BOUND, "9701"),
                        test(revision, "RAW-B", AnalyzerMappingState.UNRESOLVED, null),
                        test(revision, "RAW-C", AnalyzerMappingState.UNRESOLVED, null)),
                List.of(result(revision, "RAW-A", "POS", AnalyzerMappingState.BOUND, "811"),
                        result(revision, "RAW-A", "NEG", AnalyzerMappingState.EXCLUDED, null)));
    }

    private static AnalyzerMappingTest test(AnalyzerMapping revision, String sourceRowKey, AnalyzerMappingState state,
            String testId) {
        AnalyzerMappingTest row = new AnalyzerMappingTest();
        row.setId(new AnalyzerMappingTestPK(revision.getId(), sourceRowKey));
        row.setSiteBindingRevision(revision);
        row.setMappingState(state);
        row.setTestId(testId);
        return row;
    }

    private static AnalyzerMappingResult result(AnalyzerMapping revision, String sourceRowKey, String rawValue,
            AnalyzerMappingState state, String optionId) {
        AnalyzerMappingResult row = new AnalyzerMappingResult();
        row.setId(new AnalyzerMappingResultPK(revision.getId(), sourceRowKey, rawValue));
        row.setSiteBindingRevision(revision);
        row.setMappingState(state);
        row.setTestResultId(optionId);
        return row;
    }

    private static AnalyzerMappingDraft validDraft() {
        return new AnalyzerMappingDraft(
                List.of(new AnalyzerMappingTestDraft("RAW-A", AnalyzerMappingState.BOUND, "9701"),
                        new AnalyzerMappingTestDraft("RAW-B", AnalyzerMappingState.EXCLUDED, null),
                        new AnalyzerMappingTestDraft("RAW-C", AnalyzerMappingState.UNRESOLVED, null)),
                List.of(new AnalyzerMappingResultDraft("RAW-A", "POS", AnalyzerMappingState.BOUND, "811"),
                        new AnalyzerMappingResultDraft("RAW-A", "NEG", AnalyzerMappingState.EXCLUDED, null)));
    }

    private static AnalyzerMappingSnapshot savedSiteBinding(AnalyzerSiteBinding binding) {
        AnalyzerMapping revision = new AnalyzerMapping();
        revision.setId("62");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(5);
        revision.setBindingFingerprint("sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        return new AnalyzerMappingSnapshot(binding, revision,
                List.of(test(revision, "RAW-A", AnalyzerMappingState.BOUND, "9701"),
                        test(revision, "RAW-B", AnalyzerMappingState.EXCLUDED, null),
                        test(revision, "RAW-C", AnalyzerMappingState.UNRESOLVED, null)),
                List.of(result(revision, "RAW-A", "POS", AnalyzerMappingState.BOUND, "811"),
                        result(revision, "RAW-A", "NEG", AnalyzerMappingState.EXCLUDED, null)));
    }

    private AnalyzerMappingSnapshot confirmableSiteBinding(AnalyzerProfileBinding profileBinding) {
        AnalyzerMappingSnapshot candidate = siteBinding(profileBinding);
        candidate.tests().get(1).setMappingState(AnalyzerMappingState.EXCLUDED);
        candidate.tests().get(2).setMappingState(AnalyzerMappingState.EXCLUDED);
        return candidate;
    }

    private static List<AnalyzerMappingCatalogService.TestOption> activeTests() {
        return List.of(
                new AnalyzerMappingCatalogService.TestOption("9701", "SARS-CoV-2 RNA", "COVID19", List.of("94500-6")),
                new AnalyzerMappingCatalogService.TestOption("9702", "Ambiguous one", "AMB-1", List.of("77777-7")),
                new AnalyzerMappingCatalogService.TestOption("9703", "Ambiguous two", "AMB-2", List.of("77777-7")));
    }
}
