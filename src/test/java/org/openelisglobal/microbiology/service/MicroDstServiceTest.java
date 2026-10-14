package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroDstForm;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.result.service.ResultService;

public class MicroDstServiceTest {
    @Mock
    private MicroIsolateDAO isolates;
    @Mock
    private MicroOrganismDAO organisms;
    @Mock
    private MicroAntibioticDAO antibiotics;
    @Mock
    private MicroAstRunDAO runs;
    @Mock
    private MicroAstReportChoiceService choices;
    @Mock
    private MicroCaseAnalysisDAO links;
    @Mock
    private MicroMolecularDstBindingDAO bindings;
    @Mock
    private MicroDstResolutionDAO resolutions;
    @Mock
    private ResultService results;
    @Mock
    private IStatusService statuses;
    @Mock
    private MicroCaseAuthorizationService authorization;
    @InjectMocks
    private MicroDstService service;
    private MicroOrganism organism;
    private org.openelisglobal.result.valueholder.Result molecular;
    private MicroCaseAnalysis link;
    private MicroAstRun run;
    private final List<MicroDstResolution> history = new ArrayList<>();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        var isolate = new MicroIsolate();
        isolate.setId("i");
        isolate.setOrganismId("o");
        isolate.setIdentificationStatus("CONFIRMED");
        organism = new MicroOrganism();
        organism.setSusceptibilityProtocol("TB");
        when(isolates.getByCaseId("c")).thenReturn(List.of(isolate));
        when(organisms.get("o")).thenReturn(Optional.of(organism));
        var drug = new MicroAntibiotic();
        drug.setId("rif");
        drug.setDisplayName("Configured agent");
        drug.setResistanceRole("RIFAMPICIN");
        when(antibiotics.getActiveAntibiotics()).thenReturn(List.of(drug));
        run = new MicroAstRun();
        run.setId("run");
        run.setStatus("REVIEWED");
        when(runs.getByIsolateId("i")).thenReturn(List.of(run));
        var reading = new MicroAstReading();
        reading.setId("reading");
        reading.setAntibioticId("rif");
        reading.setInterpretation("SUSCEPTIBLE");
        when(choices.latest("run")).thenReturn(List.of(reading));
        var test = new org.openelisglobal.test.valueholder.Test();
        test.setId("test");
        var analysis = new org.openelisglobal.analysis.valueholder.Analysis();
        analysis.setId("a");
        analysis.setTest(test);
        analysis.setStatusId("final");
        link = new MicroCaseAnalysis();
        link.setPlacement("INITIAL");
        when(links.getAnalyses("c")).thenReturn(List.of(analysis));
        when(links.getByCaseAndAnalysis("c", "a")).thenReturn(link);
        var option = new org.openelisglobal.testresult.valueholder.TestResult();
        option.setComponentId("component");
        molecular = new org.openelisglobal.result.valueholder.Result();
        molecular.setId("molecular");
        molecular.setTestResult(option);
        molecular.setValue("coded-resistant");
        when(results.getResultsByAnalysis(analysis)).thenReturn(List.of(molecular));
        when(statuses.getAnalysisStatusForID("final")).thenReturn(AnalysisStatus.Finalized);
        var binding = new MicroMolecularDstBinding();
        binding.setTestId("test");
        binding.setComponentId("component");
        binding.setAntibioticId("rif");
        binding.setResistantValue("coded-resistant");
        binding.setSusceptibleValue("coded-susceptible");
        when(bindings.getBindings()).thenReturn(List.of(binding));
        when(resolutions.getByCaseId("c")).thenReturn(history);
        doAnswer(invocation -> {
            history.add(invocation.getArgument(0));
            return "resolution";
        }).when(resolutions).insert(any());
    }

    @Test
    public void exactDrugConflictNeedsReasonAndBecomesUnresolvedWhenEvidenceChanges() {
        var first = service.evaluate("c");
        assertTrue(first.unresolvedDiscordance);
        assertEquals("DISCORDANT", first.drugs.get(0).interpretation());
        var request = new MicroDstForm.ResolutionRequest();
        request.antibioticId = "rif";
        request.evidenceFingerprint = first.drugs.get(0).evidenceFingerprint();
        request.interpretation = "RESISTANT";
        request.reason = "Reviewed the molecular and phenotypic evidence";
        var resolved = service.resolve("c", request, "supervisor");
        assertFalse(resolved.unresolvedDiscordance);
        assertEquals("RR", resolved.classification);
        verify(authorization).requireWrite("c", "supervisor",
                org.openelisglobal.common.constants.Constants.ROLE_VALIDATION);
        molecular.setId("replacement-result");
        assertTrue(service.evaluate("c").unresolvedDiscordance);
        assertEquals("DST_EVIDENCE_CHANGED",
                assertThrows(MicroAstConflictException.class, () -> service.resolve("c", request, "supervisor"))
                        .getMessage());
        assertEquals(1, history.size());
    }

    @Test
    public void pendingPhenotypicEvidenceCannotBeResolved() {
        run.setStatus("RESULTS_IN");
        var pending = service.evaluate("c");
        var request = new MicroDstForm.ResolutionRequest();
        request.antibioticId = "rif";
        request.evidenceFingerprint = pending.drugs.get(0).evidenceFingerprint();
        request.interpretation = "RESISTANT";
        request.reason = "Reviewed";
        assertEquals("DST_EVIDENCE_NOT_VALIDATED",
                assertThrows(MicroAstConflictException.class, () -> service.resolve("c", request, "supervisor"))
                        .getMessage());
        assertTrue(history.isEmpty());
    }

    @Test
    public void indeterminateMolecularResultDoesNotBecomeSusceptibleFromPhenotypicResultAlone() {
        molecular.setValue("indeterminate-code");
        assertEquals("INDETERMINATE", service.evaluate("c").drugs.get(0).interpretation());
        link.setPlacement("ADDITIONAL");
        assertEquals("SUSCEPTIBLE", service.evaluate("c").drugs.get(0).interpretation());
    }

    @Test public void validatedInitialMolecularResistanceIsAvailableBeforeCultureIdentification() {
        when(isolates.getByCaseId("c")).thenReturn(List.of());
        var state=service.evaluate("c");assertEquals("RR",state.classification);assertEquals("MOLECULAR",state.drugs.get(0).evidence().get(0).source());
    }

    @Test
    public void deactivatingAnAgentDoesNotHideItsRecordedDiscordance() {
        var drug = new MicroAntibiotic();
        drug.setId("rif");
        drug.setResistanceRole("RIFAMPICIN");
        when(antibiotics.getActiveAntibiotics()).thenReturn(List.of());
        when(antibiotics.get("rif")).thenReturn(Optional.of(drug));
        var state = service.evaluate("c");
        assertTrue(state.unresolvedDiscordance);
        assertEquals("DISCORDANT", state.drugs.get(0).interpretation());
    }

    @Test
    public void ntmDoesNotUseTbBindingsOrClassification() {
        organism.setSusceptibilityProtocol("NTM");
        var result = service.evaluate("c");
        assertEquals("NTM", result.classification);
        assertTrue(result.drugs.isEmpty());
        verifyZeroInteractions(results, bindings);
    }
}
