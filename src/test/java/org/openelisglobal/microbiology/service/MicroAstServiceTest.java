package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;

/**
 * A run started with an explicit standard interprets readings against that
 * standard's rules; a run started without one falls back to the configured
 * default so existing behavior is unchanged.
 */
@RunWith(MockitoJUnitRunner.class)
public class MicroAstServiceTest {

    @Mock
    private MicroAstRunDAO runDAO;

    @Mock
    private MicroAstReadingDAO readingDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroBreakpointService breakpointService;

    @Mock
    private MicroAstInterpretationService interpretationService;

    private MicroAstService service;

    @Before
    public void setUp() {
        service = new MicroAstServiceImpl(runDAO, readingDAO, isolateDAO, caseDAO, activityDAO, breakpointService,
                interpretationService);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(mutableCase()));
    }

    @Test
    public void startRunWithExplicitStandardPersistsItOnTheRun() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));

        MicroAstRun run = service.startRun("iso-1", "panel-1", "eucast-std", "1");

        assertEquals("eucast-std", run.getBreakpointStandardId());
        verify(runDAO).insert(run);
    }

    @Test
    public void startRunWithoutStandardLeavesItNull() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));

        MicroAstRun run = service.startRun("iso-1", "panel-1", "1");

        assertNull(run.getBreakpointStandardId());
    }

    @Test
    public void recordReadingInterpretsAgainstTheRunsSnapshottedStandard() {
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("iso-1");
        run.setBreakpointStandardId("eucast-std");
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setOrganismId("org-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setId("rule-eucast");
        when(breakpointService.findBreakpointRule("eucast-std", "org-1", null, "abx-1", "MIC", null, "MIC"))
                .thenReturn(rule);
        when(interpretationService.interpret(rule, MicroAstMethod.MIC, new BigDecimal("4")))
                .thenReturn(MicroAstInterpretation.SUSCEPTIBLE);

        service.recordReading("run-1", "abx-1", MicroAstMethod.MIC, new BigDecimal("4"), "1");

        verify(breakpointService).findBreakpointRule("eucast-std", "org-1", null, "abx-1", "MIC", null, "MIC");
        verify(breakpointService, org.mockito.Mockito.never()).getActiveStandard(any(String.class), any(String.class));
    }

    @Test
    public void recordReadingFallsBackToDefaultStandardWhenRunHasNone() {
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("iso-1");
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setOrganismId("org-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        MicroBreakpointStandard defaultStandard = new MicroBreakpointStandard();
        defaultStandard.setId("clsi-std");
        when(breakpointService.getActiveStandard("CLSI", "2026")).thenReturn(defaultStandard);
        MicroBreakpointRule rule = new MicroBreakpointRule();
        when(breakpointService.findBreakpointRule("clsi-std", "org-1", null, "abx-1", "MIC", null, "MIC"))
                .thenReturn(rule);
        ArgumentCaptor<BigDecimal> valueCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        when(interpretationService.interpret(any(MicroBreakpointRule.class), any(MicroAstMethod.class),
                valueCaptor.capture())).thenReturn(MicroAstInterpretation.SUSCEPTIBLE);

        service.recordReading("run-1", "abx-1", MicroAstMethod.MIC, new BigDecimal("4"), "1");

        verify(breakpointService).findBreakpointRule("clsi-std", "org-1", null, "abx-1", "MIC", null, "MIC");
    }

    @Test(expected = IllegalStateException.class)
    public void startRunRejectsFinalReleasedCases() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        MicroCase finalCase = mutableCase();
        finalCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        finalCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(finalCase));

        service.startRun("iso-1", "panel-1", "1");
    }

    @Test
    public void overrideReadingRejectsMissingInterpretationBeforeLoadingTheReading() {
        try {
            service.overrideReading("reading-1", null, "reason", "1");
            fail("Expected a missing override interpretation to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals("overrideInterpretation is required", exception.getMessage());
        }

        verify(readingDAO, never()).get("reading-1");
    }

    private MicroCase mutableCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        return microCase;
    }
}
