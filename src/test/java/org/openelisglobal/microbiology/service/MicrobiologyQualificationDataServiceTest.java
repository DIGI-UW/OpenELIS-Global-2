package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;

@RunWith(MockitoJUnitRunner.class)
public class MicrobiologyQualificationDataServiceTest {

    @Mock
    private MicrobiologyUatScenarioService scenarioService;

    @Mock
    private MicrobiologyReferenceService referenceService;

    @Mock
    private MicroBreakpointService breakpointService;

    @Mock
    private MicrobiologyConfigurationService configurationService;

    @Mock
    private MicroIsolateService isolateService;

    @Mock
    private MicroAstService astService;

    @Mock
    private MicroCaseService caseService;

    private MicrobiologyQualificationDataService service;

    @Before
    public void setUp() {
        service = new MicrobiologyQualificationDataService(scenarioService, referenceService, breakpointService,
                configurationService, isolateService, astService, caseService, true);
    }

    @Test
    public void ordinaryDeploymentCannotBuildQualificationData() {
        MicrobiologyQualificationDataService disabled = new MicrobiologyQualificationDataService(scenarioService,
                referenceService, breakpointService, configurationService, isolateService, astService, caseService,
                false);

        try {
            disabled.buildWorklist("run-a", 200, "7");
            fail("Expected qualification data to require the explicit property");
        } catch (IllegalStateException expected) {
            assertEquals("MICROBIOLOGY_QUALIFICATION_DISABLED", expected.getMessage());
        }

        verify(scenarioService, never()).provision(any(), anyString());
    }

    @Test
    public void buildsTwoHundredWorklistCasesThroughTheScenarioService() {
        when(scenarioService.provision(any(MicrobiologyUatScenarioRequestForm.class), anyString()))
                .thenAnswer(invocation -> scenario(invocation.<MicrobiologyUatScenarioRequestForm>getArgument(0)));

        MicrobiologyQualificationDataService.WorklistDataset dataset = service.buildWorklist("run-a", 200, "7");

        assertEquals(200, dataset.caseIds().size());
        assertEquals(200, dataset.caseIds().stream().distinct().count());
        assertTrue(dataset.scenarioKeys().get(0).startsWith("qualification-run-a-worklist-"));
        verify(scenarioService, times(200)).provision(any(MicrobiologyUatScenarioRequestForm.class), anyString());
    }

    @Test
    public void buildsDenseCaseWithFiveIsolatesEightyReadingsAndAuditedTimeline() {
        when(scenarioService.provision(any(MicrobiologyUatScenarioRequestForm.class), anyString()))
                .thenReturn(scenario("qualification-run-a-dense"));
        MicroAstPanel panel = new MicroAstPanel();
        panel.setId("panel-generated");
        panel.setName("Gram negative AST panel (UAT)");
        when(referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY)).thenReturn(List.of(panel));
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setId("standard-generated");
        standard.setAuthority("CLSI");
        standard.setVersion("2026");
        when(breakpointService.getActiveStandards()).thenReturn(List.of(standard));
        AtomicInteger antibioticSequence = new AtomicInteger();
        when(configurationService.getOrCreateAntibiotic(anyString(), anyString(), anyString())).thenAnswer(
                invocation -> antibiotic("antibiotic-generated-" + antibioticSequence.incrementAndGet(),
                        invocation.getArgument(1)));

        AtomicInteger isolateSequence = new AtomicInteger();
        when(isolateService.createIsolate(anyString(), anyString(), any(), anyString(), any(), anyString()))
                .thenAnswer(invocation -> isolate("isolate-generated-" + isolateSequence.incrementAndGet()));
        AtomicInteger runSequence = new AtomicInteger();
        when(astService.startRun(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> run("run-generated-" + runSequence.incrementAndGet()));
        MicroCaseDetailForm detail = new MicroCaseDetailForm();
        detail.activities = new ArrayList<>();
        for (int index = 0; index < 91; index++) {
            detail.activities.add(new MicroCaseActivityForm());
        }
        when(caseService.getCaseDetail("case-qualification-run-a-dense")).thenReturn(detail);

        MicrobiologyQualificationDataService.DenseCaseDataset dataset = service.buildDenseCase("run-a", "7");

        assertEquals(5, dataset.isolateIds().size());
        assertEquals(80, dataset.readingCount());
        assertEquals(91, dataset.timelineEventCount());
        verify(isolateService, times(5)).createIsolate(anyString(), anyString(), any(), anyString(), any(), anyString());
        verify(astService, times(5)).startRun(anyString(), anyString(), anyString(), anyString());
        verify(astService, times(80)).recordReading(anyString(), anyString(), any(MicroAstMethod.class), any(),
                anyString());
        verify(configurationService, times(16)).getOrCreateAntibiotic(anyString(), anyString(), anyString());
        verify(configurationService, times(16)).getOrCreatePanelAntibiotic(anyString(), anyString(), anyInt());
        verify(configurationService, times(16)).getOrCreateBreakpointRule(any());
    }

    private MicrobiologyUatScenarioForm scenario(MicrobiologyUatScenarioRequestForm request) {
        return scenario(request.scenarioKey);
    }

    private MicrobiologyUatScenarioForm scenario(String key) {
        MicrobiologyUatScenarioForm form = new MicrobiologyUatScenarioForm();
        form.scenarioKey = key;
        form.caseId = "case-" + key;
        return form;
    }

    private MicroAntibiotic antibiotic(String id, String code) {
        MicroAntibiotic antibiotic = new MicroAntibiotic();
        antibiotic.setId(id);
        antibiotic.setWhonetCode(code);
        return antibiotic;
    }

    private MicroIsolate isolate(String id) {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId(id);
        return isolate;
    }

    private MicroAstRun run(String id) {
        MicroAstRun run = new MicroAstRun();
        run.setId(id);
        return run;
    }
}
