package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

/**
 * M-05: per-run breakpoint-standard selection. A run started with an explicit
 * standard interprets readings against that standard's rules; a run started
 * without one falls back to the configured default so existing behavior is
 * unchanged.
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

    @Mock
    private MicroCaseAmendmentDAO amendmentDAO;

    @Mock
    private MicroReagentLotService reagentLotService;

    @Mock
    private MicroOrganismDAO organismDAO;

    @Mock
    private MicroAstPanelDAO panelDAO;

    private MicroAstService service;

    @Before
    public void setUp() {
        service = new MicroAstServiceImpl(runDAO, readingDAO, isolateDAO, caseDAO, activityDAO, breakpointService,
                interpretationService, amendmentDAO, reagentLotService, organismDAO, panelDAO);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(mutableCase()));
        MicroOrganism organism = new MicroOrganism();
        organism.setId("org-1");
        organism.setDefaultAstPanelId("panel-1");
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism));
        MicroAstPanel panel = panel("panel-1", "GN-STD", 3);
        when(panelDAO.get("panel-1")).thenReturn(Optional.of(panel));
        MicroBreakpointStandard standard = standard("eucast-std", "EUCAST", "2025");
        when(breakpointService.getStandard("eucast-std")).thenReturn(standard);
        when(breakpointService.getActiveStandards()).thenReturn(List.of(standard));
    }

    @Test
    public void startRunWithExplicitStandardPersistsItOnTheRun() {
        MicroIsolate isolate = identifiedIsolate();
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));

        MicroAstRun run = service.startRun("iso-1", "panel-1", "eucast-std", "1");

        assertEquals("eucast-std", run.getBreakpointStandardId());
        assertEquals("2025", run.getBreakpointVersion());
        assertEquals(Integer.valueOf(3), run.getPanelVersion());
        assertEquals("ORGANISM_DEFAULT", run.getPanelProvenance());
        verify(runDAO).insert(run);
    }

    @Test
    public void startRunWithoutStandardSnapshotsTheActiveStandard() {
        MicroIsolate isolate = identifiedIsolate();
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));

        MicroAstRun run = service.startRun("iso-1", "panel-1", "1");

        assertEquals("eucast-std", run.getBreakpointStandardId());
        assertEquals("2025", run.getBreakpointVersion());
    }

    @Test
    public void setupReturnsTheIdentifiedOrganismsOrderedPanel() {
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));

        MicroAstSetupForm setup = service.getSetup("iso-1");

        assertEquals("panel-1", setup.orderedPanelId);
        assertEquals("GN-STD", setup.orderedPanelLabel);
        assertEquals(Integer.valueOf(3), setup.orderedPanelVersion);
        assertEquals("ORGANISM_DEFAULT", setup.panelProvenance);
    }

    @Test
    public void adjustedPanelRequiresReasonAndPreservesProvenance() {
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));
        when(panelDAO.get("panel-2")).thenReturn(Optional.of(panel("panel-2", "URINE-GN", 2)));

        try {
            service.startRun("iso-1", "panel-2", "eucast-std", null, List.of(), "1");
            fail("Expected an adjustment reason");
        } catch (IllegalArgumentException expected) {
            assertEquals("AST_PANEL_ADJUSTMENT_REASON_REQUIRED", expected.getMessage());
        }

        MicroAstRun adjusted = service.startRun("iso-1", "panel-2", "eucast-std",
                "Urine-specific panel required", List.of(), "1");

        assertEquals("panel-2", adjusted.getPanelId());
        assertEquals(Integer.valueOf(2), adjusted.getPanelVersion());
        assertEquals("ADJUSTED", adjusted.getPanelProvenance());
        assertEquals("Urine-specific panel required", adjusted.getPanelAdjustmentReason());
    }

    @Test(expected = IllegalStateException.class)
    public void startRunRejectsAnIsolatePendingIdentification() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.PRELIMINARY.name());
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));

        service.startRun("iso-1", "panel-1", "1");
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
        MicroIsolate isolate = identifiedIsolate();
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        MicroCase finalCase = mutableCase();
        finalCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        finalCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(finalCase));

        service.startRun("iso-1", "panel-1", "1");
    }

    @Test
    public void amendmentRunIsLinkedAndPriorRunCannotBeChanged() {
        MicroCase amendmentCase = mutableCase();
        amendmentCase.setStage(MicroCaseStage.AMENDED.name());
        amendmentCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(amendmentCase));
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setId("amendment-1");
        amendment.setCaseId("case-1");
        when(amendmentDAO.getOpenByCaseId("case-1")).thenReturn(amendment);
        MicroIsolate isolate = identifiedIsolate();
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));

        MicroAstRun amendmentRun = service.startRun("iso-1", "panel-1", "1");

        assertEquals("amendment-1", amendmentRun.getAmendmentId());

        MicroAstRun priorRun = new MicroAstRun();
        priorRun.setId("run-prior");
        priorRun.setIsolateId("iso-1");
        when(runDAO.get("run-prior")).thenReturn(Optional.of(priorRun));
        try {
            service.reviewRun("run-prior", "1");
            org.junit.Assert.fail("Expected amendment to require a new AST run");
        } catch (MicroAmendmentConflictException expected) {
            assertEquals("AMENDMENT_NEW_AST_RUN_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void repeatRunRequiresReviewedSourceAndReasonAndPreservesProvenance() {
        MicroIsolate isolate = isolate();
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        MicroAstRun source = reviewedRun("run-1");
        source.setPanelId("panel-1");
        source.setBreakpointStandardId("standard-1");
        source.setReportable(true);
        when(runDAO.get("run-1")).thenReturn(Optional.of(source));

        MicroAstRun repeat = service.startRepeatRun("run-1", MicroAstAttemptType.REPEAT,
                "  Discordant automated result  ", MicroAstMethod.MIC, "7");

        assertEquals("run-1", repeat.getSourceRunId());
        assertEquals(MicroAstAttemptType.REPEAT.name(), repeat.getAttemptType());
        assertEquals("Discordant automated result", repeat.getAttemptReason());
        assertEquals(MicroAstMethod.MIC.name(), repeat.getMethod());
        assertEquals("panel-1", repeat.getPanelId());
        assertEquals("standard-1", repeat.getBreakpointStandardId());
        assertFalse(repeat.isReportable());
        assertEquals(MicroAstRunStatus.REVIEWED.name(), source.getStatus());
        assertTrue(source.isReportable());
        verify(runDAO).insert(repeat);
        verify(runDAO, never()).update(source);
    }

    @Test
    public void repeatRunRejectsMissingReasonAndUnreviewedSource() {
        try {
            service.startRepeatRun("run-1", MicroAstAttemptType.REPEAT, " ", MicroAstMethod.MIC, "7");
            fail("Expected a repeat reason");
        } catch (IllegalArgumentException expected) {
            assertEquals("AST_ATTEMPT_REASON_REQUIRED", expected.getMessage());
        }

        MicroAstRun unreviewed = reviewedRun("run-2");
        unreviewed.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        when(runDAO.get("run-2")).thenReturn(Optional.of(unreviewed));
        try {
            service.startRepeatRun("run-2", MicroAstAttemptType.RETEST, "Control failed", MicroAstMethod.ZONE, "7");
            fail("Expected a reviewed source run");
        } catch (IllegalStateException expected) {
            assertEquals("AST_SOURCE_RUN_REVIEW_REQUIRED", expected.getMessage());
        }
    }

    @Test
    public void secondReviewedAttemptRequiresExplicitReportableSelection() {
        MicroIsolate isolate = isolate();
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        MicroAstRun original = reviewedRun("run-1");
        original.setReportable(true);
        MicroAstRun repeat = new MicroAstRun();
        repeat.setId("run-2");
        repeat.setIsolateId("iso-1");
        repeat.setAttemptType(MicroAstAttemptType.REPEAT.name());
        when(runDAO.get("run-2")).thenReturn(Optional.of(repeat));
        when(runDAO.getByIsolateId("iso-1")).thenReturn(List.of(original, repeat));
        when(runDAO.update(any(MicroAstRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.reviewRun("run-2", "7");

        assertFalse(original.isReportable());
        assertFalse(repeat.isReportable());
        verify(runDAO).update(original);
        assertEquals(MicroAstRunStatus.REVIEWED.name(), repeat.getStatus());
    }

    @Test
    public void selectingReportableAttemptClearsSiblingSelection() {
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate()));
        MicroAstRun original = reviewedRun("run-1");
        original.setReportable(false);
        MicroAstRun repeat = reviewedRun("run-2");
        repeat.setReportable(true);
        when(runDAO.get("run-1")).thenReturn(Optional.of(original));
        when(runDAO.getByIsolateId("iso-1")).thenReturn(List.of(original, repeat));
        when(runDAO.update(any(MicroAstRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MicroAstRun selected = service.selectReportableRun("run-1", "7");

        assertFalse(repeat.isReportable());
        assertTrue(selected.isReportable());
        verify(runDAO).update(original);
        verify(runDAO).update(repeat);
    }

    @Test
    public void firstReadingSnapshotsRunMethodAndMixedMethodsAreRejected() {
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("iso-1");
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        when(runDAO.update(run)).thenReturn(run);
        MicroIsolate isolate = isolate();
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setId("clsi-std");
        when(breakpointService.getActiveStandard("CLSI", "2026")).thenReturn(standard);
        when(interpretationService.interpret(any(), any(), any())).thenReturn(MicroAstInterpretation.SUSCEPTIBLE);

        service.recordReading("run-1", "abx-1", MicroAstMethod.MIC, new BigDecimal("4"), "7");

        assertEquals(MicroAstMethod.MIC.name(), run.getMethod());
        verify(runDAO).update(run);
        try {
            service.recordReading("run-1", "abx-2", MicroAstMethod.ZONE, new BigDecimal("20"), "7");
            fail("Expected one method per AST attempt");
        } catch (IllegalStateException expected) {
            assertEquals("AST_RUN_METHOD_MISMATCH", expected.getMessage());
        }
    }

    private MicroIsolate isolate() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        return isolate;
    }

    private MicroIsolate identifiedIsolate() {
        MicroIsolate isolate = isolate();
        isolate.setOrganismId("org-1");
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        return isolate;
    }

    private MicroAstRun reviewedRun(String id) {
        MicroAstRun run = new MicroAstRun();
        run.setId(id);
        run.setIsolateId("iso-1");
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        return run;
    }

    private MicroAstPanel panel(String id, String name, int version) {
        MicroAstPanel panel = new MicroAstPanel();
        panel.setId(id);
        panel.setName(name);
        panel.setVersionNumber(version);
        return panel;
    }

    private MicroBreakpointStandard standard(String id, String authority, String version) {
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setId(id);
        standard.setAuthority(authority);
        standard.setVersion(version);
        standard.setIsActive("Y");
        standard.setLifecycleStatus("ACTIVE");
        return standard;
    }

    private MicroCase mutableCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        return microCase;
    }
}
