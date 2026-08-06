package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
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
import org.openelisglobal.systemuser.valueholder.SystemUser;

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

    @Mock
    private MicroAstPanelAntibioticDAO panelAntibioticDAO;

    @Mock
    private MicroAstRunAntibioticDAO runAntibioticDAO;

    @Mock
    private MicroAstOverrideEventDAO overrideEventDAO;

    @Mock
    private SystemUserService systemUserService;

    private MicroAstService service;

    @Before
    public void setUp() {
        service = new MicroAstServiceImpl(runDAO, readingDAO, isolateDAO, caseDAO, activityDAO, breakpointService,
                interpretationService, amendmentDAO, reagentLotService, organismDAO, panelDAO, overrideEventDAO,
                systemUserService, panelAntibioticDAO, runAntibioticDAO);
        when(caseDAO.get("case-1")).thenReturn(Optional.of(mutableCase()));
        MicroOrganism organism = new MicroOrganism();
        organism.setId("org-1");
        organism.setDefaultAstPanelId("panel-1");
        when(organismDAO.get("org-1")).thenReturn(Optional.of(organism));
        MicroAstPanel panel = panel("panel-1", "GN-STD", 3);
        when(panelDAO.get("panel-1")).thenReturn(Optional.of(panel));
        when(panelAntibioticDAO.getByPanelId("panel-1")).thenReturn(List.of(panelAntibiotic("panel-1", "abx-1", 1)));
        when(runAntibioticDAO.getByRunIdAndAntibioticId(any(String.class), any(String.class))).thenAnswer(
                invocation -> Optional.of(orderedAntibiotic(invocation.getArgument(0), invocation.getArgument(1), 1)));
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
    public void startRunSnapshotsTechniqueAndDerivedMeasurementType() {
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));

        MicroAstRun vitekRun = service.startRun("iso-1", "panel-1", "eucast-std", null,
                MicroAstTechnique.VITEK_2, List.of(), "1");
        MicroAstRun diskRun = service.startRun("iso-1", "panel-1", "eucast-std", null,
                MicroAstTechnique.DISK_DIFFUSION, List.of(), "1");

        assertEquals(MicroAstTechnique.VITEK_2.name(), vitekRun.getTechnique());
        assertEquals(MicroAstMethod.MIC.name(), vitekRun.getMethod());
        assertEquals(MicroAstTechnique.DISK_DIFFUSION.name(), diskRun.getTechnique());
        assertEquals(MicroAstMethod.ZONE.name(), diskRun.getMethod());
    }

    @Test
    public void startRunSnapshotsTheExactOrderedAntibioticSet() {
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));

        MicroAstRun run = service.startRun("iso-1", "panel-1", "eucast-std", null,
                MicroAstTechnique.VITEK_2, List.of(), "1");

        ArgumentCaptor<MicroAstRunAntibiotic> ordered = ArgumentCaptor.forClass(MicroAstRunAntibiotic.class);
        verify(runAntibioticDAO).insert(ordered.capture());
        assertEquals(run.getId(), ordered.getValue().getAstRunId());
        assertEquals("abx-1", ordered.getValue().getAntibioticId());
        assertEquals(Integer.valueOf(1), ordered.getValue().getDisplayOrder());
    }

    @Test
    public void recordReadingRejectsAnAntibioticOutsideTheOrderedSet() {
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("iso-1");
        run.setTechnique(MicroAstTechnique.VITEK_2.name());
        run.setMethod(MicroAstMethod.MIC.name());
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        when(runAntibioticDAO.getByRunIdAndAntibioticId("run-1", "abx-outside")).thenReturn(Optional.empty());

        try {
            service.recordReading("run-1", "abx-outside", new BigDecimal("2"), "1");
            fail("Expected an ordered-panel conflict");
        } catch (MicroAstConflictException expected) {
            assertEquals("AST_ANTIBIOTIC_NOT_ORDERED", expected.getMessage());
        }

        verify(readingDAO, never()).insert(any(MicroAstReading.class));
    }

    @Test
    public void repeatRunCopiesTheSourceOrderedSetInsteadOfTheMutablePanel() {
        MicroAstRun source = reviewedRun("run-1");
        source.setPanelId("panel-1");
        source.setPanelVersion(3);
        source.setTechnique(MicroAstTechnique.VITEK_2.name());
        source.setMethod(MicroAstMethod.MIC.name());
        when(runDAO.get("run-1")).thenReturn(Optional.of(source));
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));
        MicroAstRunAntibiotic sourceOrder = orderedAntibiotic("run-1", "abx-original", 1);
        when(runAntibioticDAO.getByRunId("run-1")).thenReturn(List.of(sourceOrder));

        MicroAstRun repeat = service.startRepeatRun("run-1", MicroAstAttemptType.REPEAT, "Confirm result",
                MicroAstTechnique.VITEK_2, "1");

        ArgumentCaptor<MicroAstRunAntibiotic> copied = ArgumentCaptor.forClass(MicroAstRunAntibiotic.class);
        verify(runAntibioticDAO).insert(copied.capture());
        assertEquals(repeat.getId(), copied.getValue().getAstRunId());
        assertEquals("abx-original", copied.getValue().getAntibioticId());
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
        when(panelAntibioticDAO.getByPanelId("panel-2"))
                .thenReturn(List.of(panelAntibiotic("panel-2", "abx-2", 1)));

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
        rule.setOrganismId("org-1");
        rule.setUnits("ug/mL");
        when(breakpointService.findBreakpointRule("eucast-std", "org-1", null, "abx-1", "MIC", null, "MIC"))
                .thenReturn(rule);
        when(interpretationService.interpret(rule, MicroAstMethod.MIC, new BigDecimal("4")))
                .thenReturn(MicroAstInterpretation.SUSCEPTIBLE);

        MicroAstReading reading = service.recordReading("run-1", "abx-1", MicroAstMethod.MIC, new BigDecimal("4"), "1");

        verify(breakpointService).findBreakpointRule("eucast-std", "org-1", null, "abx-1", "MIC", null, "MIC");
        verify(breakpointService, org.mockito.Mockito.never()).getActiveStandard(any(String.class), any(String.class));
        assertEquals("MANUAL_ENTRY", reading.getSource());
        assertEquals("ORGANISM", reading.getMatchedBy());
        assertEquals("ug/mL", reading.getUnits());
    }

    @Test
    public void readingUsesRunTechniqueAndFallsBackToLegacyMeasurementRule() {
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("iso-1");
        run.setBreakpointStandardId("eucast-std");
        run.setTechnique(MicroAstTechnique.DISK_DIFFUSION.name());
        run.setMethod(MicroAstMethod.ZONE.name());
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));
        MicroBreakpointRule legacyRule = new MicroBreakpointRule();
        legacyRule.setId("legacy-zone-rule");
        legacyRule.setUnits("mm");
        when(breakpointService.findBreakpointRule("eucast-std", "org-1", null, "abx-1", "DISK_DIFFUSION", null, "ZONE"))
                .thenReturn(null);
        when(breakpointService.findBreakpointRule("eucast-std", "org-1", null, "abx-1", "ZONE", null, "ZONE"))
                .thenReturn(legacyRule);
        when(interpretationService.interpret(legacyRule, MicroAstMethod.ZONE, new BigDecimal("18")))
                .thenReturn(MicroAstInterpretation.SUSCEPTIBLE);

        MicroAstReading reading = service.recordReading("run-1", "abx-1", new BigDecimal("18"), "1");

        verify(breakpointService).findBreakpointRule("eucast-std", "org-1", null, "abx-1", "DISK_DIFFUSION", null,
                "ZONE");
        verify(breakpointService).findBreakpointRule("eucast-std", "org-1", null, "abx-1", "ZONE", null, "ZONE");
        assertEquals(MicroAstMethod.ZONE.name(), reading.getMethod());
        assertEquals("mm", reading.getUnits());
    }

    @Test
    public void noBreakpointReadingRetainsNoneProvenanceAndMethodUnits() {
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("iso-1");
        run.setBreakpointStandardId("eucast-std");
        run.setTechnique(MicroAstTechnique.DISK_DIFFUSION.name());
        run.setMethod(MicroAstMethod.ZONE.name());
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));
        when(interpretationService.interpret(null, MicroAstMethod.ZONE, new BigDecimal("18")))
                .thenReturn(MicroAstInterpretation.NO_BREAKPOINT);

        MicroAstReading reading = service.recordReading("run-1", "abx-1", MicroAstMethod.ZONE, new BigDecimal("18"),
                "1");

        assertEquals("NONE", reading.getMatchedBy());
        assertEquals("mm", reading.getUnits());
        assertEquals(MicroAstInterpretation.NO_BREAKPOINT.name(), reading.getInterpretation());
    }

    @Test
    public void overrideAndRevertPreserveImmutableActorLinkedHistory() {
        MicroAstReading reading = reading("reading-1", "run-1", MicroAstInterpretation.SUSCEPTIBLE);
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        run.setIsolateId("iso-1");
        when(readingDAO.get("reading-1")).thenReturn(Optional.of(reading));
        when(runDAO.get("run-1")).thenReturn(Optional.of(run));
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(identifiedIsolate()));
        when(readingDAO.update(reading)).thenReturn(reading);

        service.overrideReading("reading-1", MicroAstInterpretation.RESISTANT, "Clinical exception", "42");
        service.revertOverride("reading-1", "Repeat confirmed the original", "84");

        ArgumentCaptor<MicroAstOverrideEvent> eventCaptor = ArgumentCaptor.forClass(MicroAstOverrideEvent.class);
        verify(overrideEventDAO, org.mockito.Mockito.times(2)).insert(eventCaptor.capture());
        List<MicroAstOverrideEvent> events = eventCaptor.getAllValues();
        assertEquals(MicroAstOverrideAction.OVERRIDE.name(), events.get(0).getAction());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE.name(), events.get(0).getFromInterpretation());
        assertEquals(MicroAstInterpretation.RESISTANT.name(), events.get(0).getToInterpretation());
        assertEquals("Clinical exception", events.get(0).getReason());
        assertEquals("42", events.get(0).getPerformedBy());
        assertNotNull(events.get(0).getPerformedAt());
        assertEquals(MicroAstOverrideAction.REVERT.name(), events.get(1).getAction());
        assertEquals(MicroAstInterpretation.RESISTANT.name(), events.get(1).getFromInterpretation());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE.name(), events.get(1).getToInterpretation());
        assertEquals("Repeat confirmed the original", events.get(1).getReason());
        assertEquals("84", events.get(1).getPerformedBy());
        assertNotNull(events.get(1).getPerformedAt());
        assertEquals(null, reading.getOverrideInterpretation());
        assertEquals(null, reading.getOverrideReason());
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE.name(), reading.getInterpretation());
    }

    @Test
    public void overrideHistoryResolvesTheActorForDisplay() {
        MicroAstOverrideEvent event = new MicroAstOverrideEvent();
        event.setId("event-1");
        event.setReadingId("reading-1");
        event.setAction(MicroAstOverrideAction.OVERRIDE.name());
        event.setFromInterpretation(MicroAstInterpretation.SUSCEPTIBLE.name());
        event.setToInterpretation(MicroAstInterpretation.RESISTANT.name());
        event.setReason("Clinical exception");
        event.setPerformedAt(MicroCaseServiceImpl.now());
        event.setPerformedBy("42");
        when(overrideEventDAO.getByRunId("run-1")).thenReturn(List.of(event));
        SystemUser user = new SystemUser();
        user.setId("42");
        user.setFirstName("Olivia");
        user.setLastName("Mendez");
        when(systemUserService.getUserById("42")).thenReturn(user);

        org.openelisglobal.microbiology.form.MicroAstOverrideEventForm form = service.getOverrideHistoryForRun("run-1")
                .get(0);

        assertEquals("Olivia Mendez", form.performedByDisplay);
        assertEquals("42", form.performedBy);
    }

    @Test
    public void revertRejectsReadingWithoutAnActiveOverride() {
        MicroAstReading reading = reading("reading-1", "run-1", MicroAstInterpretation.SUSCEPTIBLE);
        when(readingDAO.get("reading-1")).thenReturn(Optional.of(reading));

        try {
            service.revertOverride("reading-1", "Not applicable", "84");
            fail("Expected revert without an override to fail");
        } catch (MicroAstConflictException expected) {
            assertEquals("AST_OVERRIDE_NOT_ACTIVE", expected.getMessage());
        }

        verify(overrideEventDAO, never()).insert(any(MicroAstOverrideEvent.class));
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
    public void repeatRunSnapshotsItsSelectedTechniqueAndMeasurement() {
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate()));
        MicroAstRun source = reviewedRun("run-1");
        source.setPanelId("panel-1");
        source.setBreakpointStandardId("standard-1");
        when(runDAO.get("run-1")).thenReturn(Optional.of(source));

        MicroAstRun repeat = service.startRepeatRun("run-1", MicroAstAttemptType.RETEST, "Method comparison",
                MicroAstTechnique.ETEST, "7");

        assertEquals(MicroAstTechnique.ETEST.name(), repeat.getTechnique());
        assertEquals(MicroAstMethod.MIC.name(), repeat.getMethod());
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
    public void legacyReadingUsesSnapshottedMeasurementAndRejectsMismatch() {
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
            assertEquals("AST_RUN_MEASUREMENT_TYPE_MISMATCH", expected.getMessage());
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
        run.setPanelId("panel-1");
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        return run;
    }

    private MicroAstReading reading(String id, String runId, MicroAstInterpretation interpretation) {
        MicroAstReading reading = new MicroAstReading();
        reading.setId(id);
        reading.setAstRunId(runId);
        reading.setInterpretation(interpretation.name());
        return reading;
    }

    private MicroAstPanel panel(String id, String name, int version) {
        MicroAstPanel panel = new MicroAstPanel();
        panel.setId(id);
        panel.setName(name);
        panel.setVersionNumber(version);
        return panel;
    }

    private MicroAstPanelAntibiotic panelAntibiotic(String panelId, String antibioticId, int displayOrder) {
        MicroAstPanelAntibiotic row = new MicroAstPanelAntibiotic();
        row.setPanelId(panelId);
        row.setAntibioticId(antibioticId);
        row.setDisplayOrder(displayOrder);
        return row;
    }

    private MicroAstRunAntibiotic orderedAntibiotic(String runId, String antibioticId, int displayOrder) {
        MicroAstRunAntibiotic row = new MicroAstRunAntibiotic();
        row.setAstRunId(runId);
        row.setAntibioticId(antibioticId);
        row.setDisplayOrder(displayOrder);
        return row;
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
