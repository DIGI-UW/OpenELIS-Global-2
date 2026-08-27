package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstOverrideEventDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstOverrideAction;
import org.openelisglobal.microbiology.valueholder.MicroAstOverrideEvent;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.systemuser.service.SystemUserService;

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
    private MicroAstPanelDAO panelDAO;
    @Mock
    private MicroOrganismDAO organismDAO;
    @Mock
    private MicroAstOverrideEventDAO overrideEventDAO;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private MicroAstPanelAntibioticDAO panelAntibioticDAO;
    @Mock
    private MicroAstRunAntibioticDAO runAntibioticDAO;
    @Mock
    private MicroAntibioticDAO antibioticDAO;

    @Mock
    private MicroCaseAmendmentDAO amendmentDAO;

    private MicroAstService service;

    @Before
    public void setUp() {
        service = new MicroAstServiceImpl(runDAO, readingDAO, isolateDAO, caseDAO, activityDAO, breakpointService,
                interpretationService, panelDAO, organismDAO, overrideEventDAO, systemUserService, panelAntibioticDAO,
                runAntibioticDAO, antibioticDAO, amendmentDAO);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(mutableCase()));
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism()));
        when(panelDAO.get("panel-1")).thenReturn(Optional.of(panel()));
        when(panelAntibioticDAO.getByPanelId("panel-1")).thenReturn(List.of(panelAntibiotic("abx-1", 1)));
        when(breakpointService.getStandard("std-1")).thenReturn(standard());
    }

    @Test
    public void startRunSnapshotsTechniqueStandardAndOrderedAntibiotics() {
        MicroAstRun run = service.startRun("iso-1", null, "std-1", null, MicroAstTechnique.ETEST, null, "1");

        assertEquals("panel-1", run.getPanelId());
        assertEquals(Integer.valueOf(3), run.getPanelVersion());
        assertEquals("ORGANISM_DEFAULT", run.getPanelProvenance());
        assertEquals("std-1", run.getBreakpointStandardId());
        assertEquals("2026", run.getBreakpointVersion());
        assertEquals(MicroAstTechnique.ETEST.name(), run.getTechnique());
        assertEquals(MicroAstMethod.MIC.name(), run.getMethod());
        ArgumentCaptor<MicroAstRunAntibiotic> ordered = ArgumentCaptor.forClass(MicroAstRunAntibiotic.class);
        verify(runAntibioticDAO).insert(ordered.capture());
        assertEquals(run.getId(), ordered.getValue().getAstRunId());
        assertEquals("abx-1", ordered.getValue().getAntibioticId());
    }

    @Test
    public void adjustedDrugSetRequiresReasonAndPreservesRequestedOrder() {
        MicroAntibiotic added = new MicroAntibiotic();
        added.setId("abx-2");
        added.setIsActive("Y");
        when(antibioticDAO.get("abx-2")).thenReturn(Optional.of(added));

        try {
            service.startRun("iso-1", "panel-1", "std-1", null, MicroAstTechnique.DISK_DIFFUSION,
                    List.of("abx-2", "abx-1"), "1");
            fail("Expected an adjusted ordered set to require a reason");
        } catch (IllegalArgumentException exception) {
            assertEquals("AST_PANEL_ADJUSTMENT_REASON_REQUIRED", exception.getMessage());
        }

        MicroAstRun run = service.startRun("iso-1", "panel-1", "std-1", "additional resistance screen",
                MicroAstTechnique.DISK_DIFFUSION, List.of("abx-2", "abx-1"), "1");

        assertEquals("ADJUSTED", run.getPanelProvenance());
        assertEquals("additional resistance screen", run.getPanelAdjustmentReason());
        assertEquals(MicroAstMethod.ZONE.name(), run.getMethod());
        ArgumentCaptor<MicroAstRunAntibiotic> ordered = ArgumentCaptor.forClass(MicroAstRunAntibiotic.class);
        verify(runAntibioticDAO, times(2)).insert(ordered.capture());
        assertEquals("abx-2", ordered.getAllValues().get(0).getAntibioticId());
        assertEquals("abx-1", ordered.getAllValues().get(1).getAntibioticId());
    }

    @Test
    public void recordReadingDerivesMeasurementAndStoresBreakpointProvenance() {
        MicroAstRun run = run(MicroAstRunStatus.IN_PROGRESS);
        run.setTechnique(MicroAstTechnique.ETEST.name());
        run.setMethod(MicroAstMethod.MIC.name());
        run.setBreakpointStandardId("std-1");
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        when(runAntibioticDAO.getByRunIdAndAntibioticId("run-1", "abx-1"))
                .thenReturn(Optional.of(runAntibiotic("run-1", "abx-1", 1)));
        MicroBreakpointRule rule = rule();
        when(breakpointService.findBreakpointRule("std-1", "org-1", "Enterobacterales", "abx-1", "MIC", null, "MIC"))
                .thenReturn(rule);
        when(interpretationService.interpret(rule, MicroAstMethod.MIC, new BigDecimal("4")))
                .thenReturn(MicroAstInterpretation.SUSCEPTIBLE);

        MicroAstReading reading = service.recordReading("run-1", "abx-1", new BigDecimal("4"), "1");

        assertEquals(MicroAstMethod.MIC.name(), reading.getMethod());
        assertEquals("MANUAL_ENTRY", reading.getSource());
        assertEquals("ORGANISM", reading.getMatchedBy());
        assertEquals("ug/mL", reading.getUnits());
        assertEquals("rule-1", reading.getBreakpointRuleId());
        verify(readingDAO).insert(reading);
    }

    @Test
    public void recordReadingRejectsDrugOutsideTheOrderedSnapshot() {
        MicroAstRun run = run(MicroAstRunStatus.IN_PROGRESS);
        run.setTechnique(MicroAstTechnique.ETEST.name());
        run.setMethod(MicroAstMethod.MIC.name());
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        when(runAntibioticDAO.getByRunIdAndAntibioticId("run-1", "abx-2")).thenReturn(Optional.empty());

        try {
            service.recordReading("run-1", "abx-2", new BigDecimal("4"), "1");
            fail("Expected an unordered antibiotic to be rejected");
        } catch (IllegalStateException exception) {
            assertEquals("AST_ANTIBIOTIC_NOT_ORDERED", exception.getMessage());
        }

        verify(readingDAO, never()).insert(any());
    }

    @Test
    public void reviewRunRequiresAReadingForEveryOrderedAntibiotic() {
        MicroAstRun run = run(MicroAstRunStatus.IN_PROGRESS);
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        when(runAntibioticDAO.getByRunId("run-1"))
                .thenReturn(List.of(runAntibiotic("run-1", "abx-1", 1), runAntibiotic("run-1", "abx-2", 2)));
        MicroAstReading reading = reading("reading-1", "abx-1", MicroAstInterpretation.SUSCEPTIBLE);
        when(readingDAO.getByRunId("run-1")).thenReturn(List.of(reading));

        try {
            service.reviewRun("run-1", "1");
            fail("Expected incomplete ordered results to be rejected");
        } catch (IllegalStateException exception) {
            assertEquals("AST_ORDERED_RESULTS_INCOMPLETE", exception.getMessage());
        }

        verify(runDAO, never()).update(any());
    }

    @Test
    public void overrideAndRevertPreserveDurableAuditHistory() {
        MicroAstRun run = run(MicroAstRunStatus.IN_PROGRESS);
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        MicroAstReading reading = reading("reading-1", "abx-1", MicroAstInterpretation.SUSCEPTIBLE);
        when(readingDAO.get("reading-1")).thenReturn(Optional.of(reading));
        when(readingDAO.update(reading)).thenReturn(reading);

        service.overrideReading("reading-1", MicroAstInterpretation.RESISTANT, "confirmed manually", "1");
        service.revertOverride("reading-1", "override entered in error", "2");

        assertNull(reading.getOverrideInterpretation());
        assertNull(reading.getOverrideReason());
        ArgumentCaptor<MicroAstOverrideEvent> events = ArgumentCaptor.forClass(MicroAstOverrideEvent.class);
        verify(overrideEventDAO, times(2)).insert(events.capture());
        assertEquals(MicroAstOverrideAction.OVERRIDE.name(), events.getAllValues().get(0).getAction());
        assertEquals("SUSCEPTIBLE", events.getAllValues().get(0).getFromInterpretation());
        assertEquals("RESISTANT", events.getAllValues().get(0).getToInterpretation());
        assertEquals(MicroAstOverrideAction.REVERT.name(), events.getAllValues().get(1).getAction());
        assertEquals("RESISTANT", events.getAllValues().get(1).getFromInterpretation());
        assertEquals("SUSCEPTIBLE", events.getAllValues().get(1).getToInterpretation());
    }

    @Test
    public void reviewedRunRejectsNewReadingsAndOverrides() {
        MicroAstRun reviewed = run(MicroAstRunStatus.REVIEWED);
        when(runDAO.get("run-1")).thenReturn(Optional.of(reviewed));

        try {
            service.recordReading("run-1", "abx-1", new BigDecimal("4"), "1");
            fail("Expected a reviewed run to reject new readings");
        } catch (IllegalStateException exception) {
            assertEquals("AST_RUN_REVIEWED", exception.getMessage());
        }

        MicroAstReading reading = reading("reading-1", "abx-1", MicroAstInterpretation.SUSCEPTIBLE);
        when(readingDAO.get("reading-1")).thenReturn(Optional.of(reading));
        try {
            service.overrideReading("reading-1", MicroAstInterpretation.RESISTANT, "confirmed manually", "1");
            fail("Expected a reviewed run to reject overrides");
        } catch (IllegalStateException exception) {
            assertEquals("AST_RUN_REVIEWED", exception.getMessage());
        }

        verify(readingDAO, never()).update(any());
    }

    @Test(expected = IllegalStateException.class)
    public void startRunRejectsFinalReleasedCases() {
        MicroCase finalCase = mutableCase();
        finalCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        finalCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        when(caseDAO.get("case-1")).thenReturn(Optional.of(finalCase));

        service.startRun("iso-1", "panel-1", "std-1", null, MicroAstTechnique.ETEST, null, "1");
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
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
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

    private MicroCase mutableCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.RECEIVED.name());
        return microCase;
    }

    private MicroIsolate identifiedIsolate() {
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setOrganismId("org-1");
        isolate.setIdentificationStatus(MicroIsolateIdentificationStatus.CONFIRMED.name());
        return isolate;
    }

    private MicroOrganism organism() {
        MicroOrganism organism = new MicroOrganism();
        organism.setId("org-1");
        organism.setOrganismGroup("Enterobacterales");
        organism.setDefaultAstPanelId("panel-1");
        return organism;
    }

    private MicroAstPanel panel() {
        MicroAstPanel panel = new MicroAstPanel();
        panel.setId("panel-1");
        panel.setName("Enterobacterales panel");
        panel.setVersionNumber(3);
        panel.setIsActive("Y");
        return panel;
    }

    private MicroAstPanelAntibiotic panelAntibiotic(String antibioticId, int displayOrder) {
        MicroAstPanelAntibiotic ordered = new MicroAstPanelAntibiotic();
        ordered.setPanelId("panel-1");
        ordered.setAntibioticId(antibioticId);
        ordered.setDisplayOrder(displayOrder);
        return ordered;
    }

    private MicroAstRunAntibiotic runAntibiotic(String runId, String antibioticId, int displayOrder) {
        MicroAstRunAntibiotic ordered = new MicroAstRunAntibiotic();
        ordered.setAstRunId(runId);
        ordered.setAntibioticId(antibioticId);
        ordered.setDisplayOrder(displayOrder);
        return ordered;
    }

    private MicroBreakpointStandard standard() {
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        standard.setId("std-1");
        standard.setAuthority("CLSI");
        standard.setVersion("2026");
        standard.setIsActive("Y");
        return standard;
    }

    private MicroBreakpointRule rule() {
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setId("rule-1");
        rule.setOrganismId("org-1");
        return rule;
    }

    private MicroAstRun run(MicroAstRunStatus status) {
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("iso-1");
        run.setStatus(status.name());
        return run;
    }

    private MicroAstReading reading(String id, String antibioticId, MicroAstInterpretation interpretation) {
        MicroAstReading reading = new MicroAstReading();
        reading.setId(id);
        reading.setAstRunId("run-1");
        reading.setAntibioticId(antibioticId);
        reading.setInterpretation(interpretation.name());
        return reading;
    }
}
