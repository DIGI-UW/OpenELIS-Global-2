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
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
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

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerTypeMappingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BridgeProfileCatalogService bridgeProfileCatalogService;

    @Mock
    private AnalyzerProfileBindingDAO profileBindingDAO;

    @Mock
    private AnalyzerSiteBindingService siteBindingService;

    @Mock
    private AnalyzerMappingCatalogService mappingCatalogService;

    @Mock
    private AnalyzerProfileBindingService profileBindingService;

    @Mock
    private AnalyzerSiteBindingConfirmationService confirmationService;

    @Mock
    private AnalyzerResultsService analyzerResultsService;

    private AnalyzerTypeMappingService service;

    @Before
    public void setUp() {
        service = new AnalyzerTypeMappingServiceImpl(bridgeProfileCatalogService, profileBindingDAO, siteBindingService,
                mappingCatalogService, profileBindingService, confirmationService, analyzerResultsService);
    }

    @Test
    public void getMappingPreservesEverySourceRowAndHydratesCurrentLocalChoices() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerSiteBindingSnapshot siteBinding = siteBinding(profileBinding);
        AnalyzerSiteBindingConfirmationView confirmation = new AnalyzerSiteBindingConfirmationView(
                AnalyzerSiteBindingConfirmationView.State.STALE, "site.mock-analyzer", 2,
                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", recognitionFingerprint(),
                "16", "Grace Hopper", null, List.of(), List.of());
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(siteBinding));
        when(confirmationService.getStatus(siteBinding, recognitionFingerprint())).thenReturn(confirmation);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        AnalyzerTypeMappingView view = service.getMapping("site.mock-analyzer", 2);

        assertEquals("site.mock-analyzer", view.profileId());
        assertEquals(2, view.profileRevision());
        assertEquals("sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                view.profileFingerprint());
        assertEquals("51", view.siteBindingId());
        assertEquals(4, view.siteBindingRevision());
        assertEquals(3, view.tests().size());

        AnalyzerTypeMappingView.TestRow first = view.tests().get(0);
        assertEquals("RAW-A", first.sourceRowKey());
        assertEquals("RAW-A", first.rawCode());
        assertEquals(List.of("RAW-A1", "RAW-A2"), first.aliases());
        assertEquals("First result", first.testNameHint());
        assertEquals("94500-6", first.loinc());
        assertEquals("https://loinc.org", first.normalizedCoding().system());
        assertEquals("94500-6", first.normalizedCoding().code());
        assertEquals(AnalyzerSiteBindingMappingState.BOUND, first.mappingState());
        assertEquals("9701", first.testId());
        assertEquals("SARS-CoV-2 RNA", first.selectedTest().name());
        assertEquals(2, first.results().size());
        assertEquals("POS", first.results().get(0).rawValue());
        assertEquals("811", first.results().get(0).resultOptionId());
        assertEquals("Positive", first.results().get(0).selectedOption().label());
        assertEquals(AnalyzerSiteBindingMappingState.EXCLUDED, first.results().get(1).mappingState());

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
        assertEquals(AnalyzerSiteBindingConfirmationView.State.STALE, view.confirmation().state());
        verify(profileBindingDAO).findByProfileIdAndRevision("site.mock-analyzer", 2);
        verify(siteBindingService).findCurrentByProfileBindingId("41");
    }

    @Test
    public void confirmMappingUsesTheExactCurrentProfileAndSiteBindingCandidate() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerSiteBindingSnapshot candidate = confirmableSiteBinding(profileBinding);
        AnalyzerSiteBindingConfirmationRequest request = new AnalyzerSiteBindingConfirmationRequest(
                candidate.revision().getBindingFingerprint(), recognitionFingerprint(),
                List.of(new AnalyzerSiteBindingSourceRow("RAW-A", null),
                        new AnalyzerSiteBindingSourceRow("RAW-A", "POS")),
                List.of(new AnalyzerSiteBindingSourceRow("RAW-A", "NEG"),
                        new AnalyzerSiteBindingSourceRow("RAW-B", null),
                        new AnalyzerSiteBindingSourceRow("RAW-C", null)));
        AnalyzerSiteBindingConfirmationView expected = AnalyzerSiteBindingConfirmationView.unconfirmed();
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(candidate));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive")));
        when(confirmationService.confirm(candidate, recognitionFingerprint(), request, "17")).thenReturn(expected);

        AnalyzerSiteBindingConfirmationView confirmed = service.confirmMapping("site.mock-analyzer", 2, request, "17");

        assertEquals(expected, confirmed);
        verify(confirmationService).confirm(candidate, recognitionFingerprint(), request, "17");
    }

    @Test
    public void getMappingReturnsUnresolvedRowsWithoutCreatingLocalState() throws Exception {
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2)).thenReturn(Optional.empty());
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());

        AnalyzerTypeMappingView view = service.getMapping("site.mock-analyzer", 2);

        assertNull(view.siteBindingId());
        assertEquals(0, view.siteBindingRevision());
        assertEquals(AnalyzerSiteBindingMappingState.UNRESOLVED, view.tests().get(0).mappingState());
        assertEquals(AnalyzerSiteBindingMappingState.UNRESOLVED,
                view.tests().get(0).results().get(0).mappingState());
        assertEquals("9701", view.tests().get(0).suggestedTest().id());
    }

    @Test
    public void getMappingIncludesHeldQualitativeValuesInTheSharedEditor() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerSiteBindingSnapshot siteBinding = siteBinding(profileBinding);
        AnalyzerResults held = new AnalyzerResults();
        held.setRawTestCode("RAW-A");
        held.setRawResultValue("INDETERMINATE-VENDOR-X");
        held.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(siteBinding));
        when(analyzerResultsService.findHeldResultValuesByProfile("site.mock-analyzer", 2)).thenReturn(List.of(held));
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        AnalyzerTypeMappingView view = service.getMapping("site.mock-analyzer", 2);

        assertEquals(3, view.tests().get(0).results().size());
        AnalyzerTypeMappingView.ResultRow observed = view.tests().get(0).results().get(2);
        assertEquals("INDETERMINATE-VENDOR-X", observed.rawValue());
        assertEquals(AnalyzerSiteBindingMappingState.UNRESOLVED, observed.mappingState());
    }

    @Test
    public void saveMappingAppendsAnAuditedRevisionAgainstTheLoadedFingerprint() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerSiteBindingSnapshot current = siteBinding(profileBinding);
        AnalyzerSiteBindingSnapshot saved = savedSiteBinding(current.binding());
        AnalyzerSiteBindingDraft draft = validDraft();
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(current));
        when(profileBindingService.resolveActiveRevision("site.mock-analyzer", 2, "17")).thenReturn(profileBinding);
        when(siteBindingService.appendRevision(eq(current.binding()), any(AnalyzerSiteBindingDraft.class), eq("17")))
                .thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        AnalyzerTypeMappingView view = service.saveMapping("site.mock-analyzer", 2, new AnalyzerTypeMappingUpdate(
                current.revision().getBindingFingerprint(), draft.tests(), draft.results()), "17");

        assertEquals(5, view.siteBindingRevision());
        assertEquals("sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                view.bindingFingerprint());
        assertEquals(AnalyzerSiteBindingMappingState.EXCLUDED, view.tests().get(1).mappingState());
        ArgumentCaptor<AnalyzerSiteBindingDraft> savedDraft = ArgumentCaptor.forClass(AnalyzerSiteBindingDraft.class);
        verify(siteBindingService).appendRevision(eq(current.binding()), savedDraft.capture(), eq("17"));
        assertEquals(draft, savedDraft.getValue());
    }

    @Test
    public void saveMappingAcceptsAnObservedValueOnlyForAProfileDefinedTest() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerSiteBindingSnapshot current = siteBinding(profileBinding);
        AnalyzerSiteBindingDraft base = validDraft();
        AnalyzerSiteBindingDraft withObservedValue = new AnalyzerSiteBindingDraft(base.tests(),
                List.of(base.results().get(0), base.results().get(1), new AnalyzerSiteBindingResultDraft("RAW-A",
                        "INDETERMINATE-VENDOR-X", AnalyzerSiteBindingMappingState.BOUND, "811")));
        AnalyzerSiteBindingSnapshot saved = savedSiteBinding(current.binding());
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(current));
        when(profileBindingService.resolveActiveRevision("site.mock-analyzer", 2, "17")).thenReturn(profileBinding);
        when(siteBindingService.appendRevision(eq(current.binding()), any(AnalyzerSiteBindingDraft.class), eq("17")))
                .thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        service.saveMapping("site.mock-analyzer", 2,
                new AnalyzerTypeMappingUpdate(current.revision().getBindingFingerprint(), withObservedValue.tests(),
                        withObservedValue.results()),
                "17");

        ArgumentCaptor<AnalyzerSiteBindingDraft> savedDraft = ArgumentCaptor.forClass(AnalyzerSiteBindingDraft.class);
        verify(siteBindingService).appendRevision(eq(current.binding()), savedDraft.capture(), eq("17"));
        assertEquals(withObservedValue, savedDraft.getValue());
    }

    @Test
    public void saveMappingCreatesTheSharedBindingForAnUnusedAnalyzerType() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerSiteBindingSnapshot initial = siteBinding(profileBinding);
        AnalyzerSiteBindingSnapshot saved = savedSiteBinding(initial.binding());
        AnalyzerSiteBindingDraft draft = validDraft();
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2)).thenReturn(Optional.empty());
        when(profileBindingService.resolveActiveRevision("site.mock-analyzer", 2, "17")).thenReturn(profileBinding);
        when(siteBindingService.resolveInitialRevision(eq(profileBinding), any(JsonNode.class), eq("17")))
                .thenReturn(initial);
        when(siteBindingService.appendRevision(eq(initial.binding()), any(AnalyzerSiteBindingDraft.class), eq("17")))
                .thenReturn(saved);
        when(mappingCatalogService.searchActiveTests(null)).thenReturn(activeTests());
        when(mappingCatalogService.getActiveResultOptions("9701"))
                .thenReturn(List.of(new AnalyzerMappingCatalogService.ResultOption("811", "1001", "Positive"),
                        new AnalyzerMappingCatalogService.ResultOption("812", "1002", "Negative")));

        AnalyzerTypeMappingView view = service.saveMapping("site.mock-analyzer", 2,
                new AnalyzerTypeMappingUpdate(null, draft.tests(), draft.results()), "17");

        assertEquals("51", view.siteBindingId());
        assertEquals(5, view.siteBindingRevision());
        verify(siteBindingService).resolveInitialRevision(eq(profileBinding), any(JsonNode.class), eq("17"));
        verify(siteBindingService).appendRevision(eq(initial.binding()), eq(draft), eq("17"));
    }

    @Test
    public void saveMappingRejectsOmittedOrInventedRowsBeforeCreatingLocalState() throws Exception {
        AnalyzerSiteBindingDraft valid = validDraft();
        AnalyzerTypeMappingUpdate omitted = new AnalyzerTypeMappingUpdate(null,
                valid.tests().stream().filter(row -> !"RAW-C".equals(row.sourceRowKey())).toList(), valid.results());
        AnalyzerTypeMappingUpdate invented = new AnalyzerTypeMappingUpdate(null,
                List.of(valid.tests().get(0), valid.tests().get(1), valid.tests().get(2),
                        new AnalyzerSiteBindingTestDraft("RAW-X", AnalyzerSiteBindingMappingState.EXCLUDED, null)),
                valid.results());
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());

        assertEquals("Mapping update test rows must exactly match profile revision",
                assertThrows(IllegalArgumentException.class,
                        () -> service.saveMapping("site.mock-analyzer", 2, omitted, "17")).getMessage());
        assertEquals("Mapping update test rows must exactly match profile revision",
                assertThrows(IllegalArgumentException.class,
                        () -> service.saveMapping("site.mock-analyzer", 2, invented, "17")).getMessage());
        verifyZeroInteractions(profileBindingService, siteBindingService);
    }

    @Test
    public void saveMappingRejectsAStaleLoadedFingerprintBeforeAppending() throws Exception {
        AnalyzerProfileBinding profileBinding = profileBinding();
        AnalyzerSiteBindingSnapshot current = siteBinding(profileBinding);
        AnalyzerSiteBindingDraft draft = validDraft();
        when(bridgeProfileCatalogService.getProfile("site.mock-analyzer", 2)).thenReturn(profileRevision());
        when(profileBindingDAO.findByProfileIdAndRevision("site.mock-analyzer", 2))
                .thenReturn(Optional.of(profileBinding));
        when(siteBindingService.findCurrentByProfileBindingId("41")).thenReturn(Optional.of(current));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveMapping("site.mock-analyzer", 2,
                        new AnalyzerTypeMappingUpdate(
                                "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                                draft.tests(), draft.results()),
                        "17"));

        assertEquals("Analyzer Type mappings changed after this editor was loaded", error.getMessage());
        verifyZeroInteractions(profileBindingService);
        verify(siteBindingService, never()).appendRevision(any(), any(), any());
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

    private AnalyzerSiteBindingSnapshot siteBinding(AnalyzerProfileBinding profileBinding) {
        AnalyzerSiteBinding binding = new AnalyzerSiteBinding();
        binding.setId("51");
        binding.setProfileBinding(profileBinding);
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("61");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(4);
        revision.setBindingFingerprint("sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        return new AnalyzerSiteBindingSnapshot(binding, revision,
                List.of(test(revision, "RAW-A", AnalyzerSiteBindingMappingState.BOUND, "9701"),
                        test(revision, "RAW-B", AnalyzerSiteBindingMappingState.UNRESOLVED, null),
                        test(revision, "RAW-C", AnalyzerSiteBindingMappingState.UNRESOLVED, null)),
                List.of(result(revision, "RAW-A", "POS", AnalyzerSiteBindingMappingState.BOUND, "811"),
                        result(revision, "RAW-A", "NEG", AnalyzerSiteBindingMappingState.EXCLUDED, null)));
    }

    private static AnalyzerSiteBindingTest test(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            AnalyzerSiteBindingMappingState state, String testId) {
        AnalyzerSiteBindingTest row = new AnalyzerSiteBindingTest();
        row.setId(new AnalyzerSiteBindingTestPK(revision.getId(), sourceRowKey));
        row.setSiteBindingRevision(revision);
        row.setMappingState(state);
        row.setTestId(testId);
        return row;
    }

    private static AnalyzerSiteBindingResult result(AnalyzerSiteBindingRevision revision, String sourceRowKey,
            String rawValue, AnalyzerSiteBindingMappingState state, String optionId) {
        AnalyzerSiteBindingResult row = new AnalyzerSiteBindingResult();
        row.setId(new AnalyzerSiteBindingResultPK(revision.getId(), sourceRowKey, rawValue));
        row.setSiteBindingRevision(revision);
        row.setMappingState(state);
        row.setTestResultId(optionId);
        return row;
    }

    private static AnalyzerSiteBindingDraft validDraft() {
        return new AnalyzerSiteBindingDraft(
                List.of(new AnalyzerSiteBindingTestDraft("RAW-A", AnalyzerSiteBindingMappingState.BOUND, "9701"),
                        new AnalyzerSiteBindingTestDraft("RAW-B", AnalyzerSiteBindingMappingState.EXCLUDED, null),
                        new AnalyzerSiteBindingTestDraft("RAW-C", AnalyzerSiteBindingMappingState.UNRESOLVED, null)),
                List.of(new AnalyzerSiteBindingResultDraft("RAW-A", "POS", AnalyzerSiteBindingMappingState.BOUND,
                        "811"),
                        new AnalyzerSiteBindingResultDraft("RAW-A", "NEG", AnalyzerSiteBindingMappingState.EXCLUDED,
                                null)));
    }

    private static AnalyzerSiteBindingSnapshot savedSiteBinding(AnalyzerSiteBinding binding) {
        AnalyzerSiteBindingRevision revision = new AnalyzerSiteBindingRevision();
        revision.setId("62");
        revision.setSiteBinding(binding);
        revision.setRevisionNumber(5);
        revision.setBindingFingerprint("sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        return new AnalyzerSiteBindingSnapshot(binding, revision,
                List.of(test(revision, "RAW-A", AnalyzerSiteBindingMappingState.BOUND, "9701"),
                        test(revision, "RAW-B", AnalyzerSiteBindingMappingState.EXCLUDED, null),
                        test(revision, "RAW-C", AnalyzerSiteBindingMappingState.UNRESOLVED, null)),
                List.of(result(revision, "RAW-A", "POS", AnalyzerSiteBindingMappingState.BOUND, "811"),
                        result(revision, "RAW-A", "NEG", AnalyzerSiteBindingMappingState.EXCLUDED, null)));
    }

    private AnalyzerSiteBindingSnapshot confirmableSiteBinding(AnalyzerProfileBinding profileBinding) {
        AnalyzerSiteBindingSnapshot candidate = siteBinding(profileBinding);
        candidate.tests().get(1).setMappingState(AnalyzerSiteBindingMappingState.EXCLUDED);
        candidate.tests().get(2).setMappingState(AnalyzerSiteBindingMappingState.EXCLUDED);
        return candidate;
    }

    private static List<AnalyzerMappingCatalogService.TestOption> activeTests() {
        return List.of(
                new AnalyzerMappingCatalogService.TestOption("9701", "SARS-CoV-2 RNA", "COVID19", List.of("94500-6")),
                new AnalyzerMappingCatalogService.TestOption("9702", "Ambiguous one", "AMB-1", List.of("77777-7")),
                new AnalyzerMappingCatalogService.TestOption("9703", "Ambiguous two", "AMB-2", List.of("77777-7")));
    }
}
