package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.openelisglobal.microbiology.form.MicroAstOverrideEventForm;
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
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
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroAstServiceImpl implements MicroAstService {

    private static final String DEFAULT_BREAKPOINT_AUTHORITY = "CLSI";
    private static final String DEFAULT_BREAKPOINT_VERSION = "2026";

    private final MicroAstRunDAO runDAO;
    private final MicroAstReadingDAO readingDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroBreakpointService breakpointService;
    private final MicroAstInterpretationService interpretationService;
    private final MicroCaseAmendmentDAO amendmentDAO;
    private final MicroReagentLotService reagentLotService;
    private final MicroOrganismDAO organismDAO;
    private final MicroAstPanelDAO panelDAO;
    private final MicroAstOverrideEventDAO overrideEventDAO;
    private final SystemUserService systemUserService;
    private final MicroAstPanelAntibioticDAO panelAntibioticDAO;
    private final MicroAstRunAntibioticDAO runAntibioticDAO;
    private final MicroAntibioticDAO antibioticDAO;

    public MicroAstServiceImpl(MicroAstRunDAO runDAO, MicroAstReadingDAO readingDAO, MicroIsolateDAO isolateDAO,
            MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, MicroBreakpointService breakpointService,
            MicroAstInterpretationService interpretationService, MicroCaseAmendmentDAO amendmentDAO,
            MicroReagentLotService reagentLotService, MicroOrganismDAO organismDAO, MicroAstPanelDAO panelDAO,
            MicroAstOverrideEventDAO overrideEventDAO, SystemUserService systemUserService,
            MicroAstPanelAntibioticDAO panelAntibioticDAO, MicroAstRunAntibioticDAO runAntibioticDAO,
            MicroAntibioticDAO antibioticDAO) {
        this.runDAO = runDAO;
        this.readingDAO = readingDAO;
        this.isolateDAO = isolateDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.breakpointService = breakpointService;
        this.interpretationService = interpretationService;
        this.amendmentDAO = amendmentDAO;
        this.reagentLotService = reagentLotService;
        this.organismDAO = organismDAO;
        this.panelDAO = panelDAO;
        this.overrideEventDAO = overrideEventDAO;
        this.systemUserService = systemUserService;
        this.panelAntibioticDAO = panelAntibioticDAO;
        this.runAntibioticDAO = runAntibioticDAO;
        this.antibioticDAO = antibioticDAO;
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String performedBy) {
        return startRun(isolateId, panelId, null, performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String performedBy) {
        return startRun(isolateId, panelId, breakpointStandardId, List.of(), performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            List<MicroLotSelection> lotSelections, String performedBy) {
        return startRun(isolateId, panelId, breakpointStandardId, null, lotSelections, performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            String panelAdjustmentReason, List<MicroLotSelection> lotSelections, String performedBy) {
        return startRun(isolateId, panelId, breakpointStandardId, panelAdjustmentReason,
                MicroAstTechnique.LEGACY_UNSPECIFIED_MIC, lotSelections, performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            String panelAdjustmentReason, MicroAstTechnique technique, List<MicroLotSelection> lotSelections,
            String performedBy) {
        return startRun(isolateId, panelId, breakpointStandardId, panelAdjustmentReason, technique, lotSelections, null,
                performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            String panelAdjustmentReason, MicroAstTechnique technique, List<MicroLotSelection> lotSelections,
            List<String> orderedAntibioticIds, String performedBy) {
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        if (technique == null) {
            throw new IllegalArgumentException("AST_TECHNIQUE_REQUIRED");
        }
        MicroIsolate isolate = isolateDAO.get(isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        if (!MicroIsolateIdentificationStatus.CONFIRMED.name().equals(isolate.getIdentificationStatus())
                || isolate.getOrganismId() == null || isolate.getOrganismId().trim().isEmpty()) {
            throw new IllegalStateException("AST_ISOLATE_IDENTIFICATION_REQUIRED");
        }
        MicroCase microCase = requireMutableCase(isolate.getCaseId());
        MicroOrganism organism = organismDAO.get(isolate.getOrganismId())
                .orElseThrow(() -> new IllegalArgumentException("AST_ORGANISM_NOT_FOUND"));
        PanelSelection panelSelection = resolvePanel(organism, panelId);
        OrderedSelection orderedSelection = resolveOrderedAntibiotics(panelSelection.panel.getId(),
                orderedAntibioticIds);
        boolean adjusted = panelSelection.adjusted || orderedSelection.adjusted;
        if (adjusted && (panelAdjustmentReason == null || panelAdjustmentReason.trim().isEmpty())) {
            throw new IllegalArgumentException("AST_PANEL_ADJUSTMENT_REASON_REQUIRED");
        }
        MicroBreakpointStandard standard = resolveStandard(breakpointStandardId);
        MicroAstRun run = new MicroAstRun();
        run.setIsolateId(isolateId);
        run.setPanelId(panelSelection.panel.getId());
        run.setPanelVersion(panelSelection.panel.getVersionNumber());
        run.setPanelProvenance(adjusted ? "ADJUSTED" : "ORGANISM_DEFAULT");
        run.setPanelAdjustmentReason(adjusted ? panelAdjustmentReason.trim() : null);
        run.setBreakpointStandardId(standard.getId());
        run.setBreakpointVersion(standard.getVersion());
        run.setAttemptType(MicroAstAttemptType.ORIGINAL.name());
        run.setTechnique(technique.name());
        run.setMethod(technique.measurementType().name());
        run.setReportable(false);
        if (isAmendmentInProgress(microCase)) {
            run.setAmendmentId(requireOpenAmendment(microCase.getId()).getId());
        }
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        run.setStartedAt(MicroCaseServiceImpl.now());
        run.setStartedBy(performedBy);
        runDAO.insert(run);
        snapshotOrderedAntibiotics(run.getId(), orderedSelection.antibiotics);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_RUN_CREATED, performedBy, "AST run created",
                "{\"astRunId\":\"" + run.getId() + "\"}");
        reagentLotService.recordSelections(isolate.getCaseId(), MicroInventoryUsageContext.AST_SETUP, run.getId(),
                lotSelections, performedBy);
        return run;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroAstSetupForm getSetup(String isolateId) {
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        MicroIsolate isolate = isolateDAO.get(isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        if (!MicroIsolateIdentificationStatus.CONFIRMED.name().equals(isolate.getIdentificationStatus())
                || isolate.getOrganismId() == null || isolate.getOrganismId().isBlank()) {
            throw new IllegalStateException("AST_ISOLATE_IDENTIFICATION_REQUIRED");
        }
        MicroOrganism organism = organismDAO.get(isolate.getOrganismId())
                .orElseThrow(() -> new IllegalArgumentException("AST_ORGANISM_NOT_FOUND"));
        MicroAstSetupForm form = new MicroAstSetupForm();
        form.isolateId = isolateId;
        form.panelProvenance = "UNASSIGNED";
        if (organism.getDefaultAstPanelId() != null && !organism.getDefaultAstPanelId().isBlank()) {
            MicroAstPanel panel = panelDAO.get(organism.getDefaultAstPanelId())
                    .orElseThrow(() -> new IllegalStateException("AST_ORDERED_PANEL_NOT_FOUND"));
            form.orderedPanelId = panel.getId();
            form.orderedPanelLabel = panel.getName();
            form.orderedPanelVersion = panel.getVersionNumber();
            form.panelProvenance = "ORGANISM_DEFAULT";
        }
        return form;
    }

    @Override
    @Transactional
    public MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstMethod method, String performedBy) {
        return startRepeatRun(sourceRunId, attemptType, reason, MicroAstTechnique.legacyFor(method), List.of(),
                performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstMethod method, List<MicroLotSelection> lotSelections, String performedBy) {
        return startRepeatRun(sourceRunId, attemptType, reason, MicroAstTechnique.legacyFor(method), lotSelections,
                performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstTechnique technique, String performedBy) {
        return startRepeatRun(sourceRunId, attemptType, reason, technique, List.of(), performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, String performedBy) {
        MicroCaseServiceImpl.requireText(sourceRunId, "sourceRunId");
        if (attemptType == null || MicroAstAttemptType.ORIGINAL.equals(attemptType)) {
            throw new IllegalArgumentException("AST_REPEAT_OR_RETEST_REQUIRED");
        }
        requireAttemptReason(reason);
        if (technique == null) {
            throw new IllegalArgumentException("AST_TECHNIQUE_REQUIRED");
        }
        MicroAstRun source = runDAO.get(sourceRunId)
                .orElseThrow(() -> new IllegalArgumentException("AST source run not found"));
        if (!MicroAstRunStatus.REVIEWED.name().equals(source.getStatus())) {
            throw new MicroAstConflictException("AST_SOURCE_RUN_REVIEW_REQUIRED");
        }
        MicroIsolate isolate = isolateDAO.get(source.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        MicroCase microCase = requireMutableCase(isolate.getCaseId());
        if (isAmendmentInProgress(microCase) && !source.isReportable()) {
            throw new MicroAstConflictException("AST_AMENDMENT_SOURCE_MUST_BE_REPORTABLE");
        }

        MicroAstRun run = new MicroAstRun();
        run.setIsolateId(source.getIsolateId());
        run.setPanelId(source.getPanelId());
        run.setPanelVersion(source.getPanelVersion());
        run.setPanelProvenance(source.getPanelProvenance());
        run.setPanelAdjustmentReason(source.getPanelAdjustmentReason());
        run.setBreakpointStandardId(source.getBreakpointStandardId());
        run.setBreakpointVersion(source.getBreakpointVersion());
        run.setAttemptType(attemptType.name());
        run.setSourceRunId(source.getId());
        run.setAttemptReason(reason.trim());
        run.setTechnique(technique.name());
        run.setMethod(technique.measurementType().name());
        run.setReportable(false);
        if (isAmendmentInProgress(microCase)) {
            run.setAmendmentId(requireOpenAmendment(microCase.getId()).getId());
        }
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        run.setStartedAt(MicroCaseServiceImpl.now());
        run.setStartedBy(performedBy);
        runDAO.insert(run);
        copyOrderedAntibiotics(source, run);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_RUN_CREATED, performedBy,
                attemptType.name() + " AST run created",
                "{\"astRunId\":\"" + run.getId() + "\",\"sourceRunId\":\"" + source.getId() + "\"}");
        reagentLotService.recordSelections(isolate.getCaseId(), MicroInventoryUsageContext.AST_SETUP, run.getId(),
                lotSelections, performedBy);
        return run;
    }

    @Override
    @Transactional
    public MicroAstReading recordReading(String runId, String antibioticId, MicroAstMethod method, BigDecimal rawValue,
            String performedBy) {
        MicroAstRun run = requireRun(runId);
        MicroAstMethod expected = measurementTypeFor(run);
        if (!expected.equals(method)) {
            throw new MicroAstConflictException("AST_RUN_MEASUREMENT_TYPE_MISMATCH");
        }
        return recordReading(run, antibioticId, expected, rawValue, performedBy);
    }

    @Override
    @Transactional
    public MicroAstReading recordReading(String runId, String antibioticId, BigDecimal rawValue, String performedBy) {
        MicroAstRun run = requireRun(runId);
        return recordReading(run, antibioticId, measurementTypeFor(run), rawValue, performedBy);
    }

    private MicroAstReading recordReading(MicroAstRun run, String antibioticId, MicroAstMethod method,
            BigDecimal rawValue, String performedBy) {
        String runId = run.getId();
        MicroCaseServiceImpl.requireText(runId, "runId");
        MicroCaseServiceImpl.requireText(antibioticId, "antibioticId");
        if (runAntibioticDAO.getByRunIdAndAntibioticId(runId, antibioticId).isEmpty()) {
            throw new MicroAstConflictException("AST_ANTIBIOTIC_NOT_ORDERED");
        }
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableRun(run, isolate.getCaseId());
        MicroOrganism organism = organismDAO.get(isolate.getOrganismId()).orElse(null);
        MicroBreakpointRule rule = findRule(run, isolate, organism, antibioticId, method);
        MicroAstInterpretation interpretation = interpretationService.interpret(rule, method, rawValue);

        MicroAstReading reading = new MicroAstReading();
        reading.setAstRunId(runId);
        reading.setAntibioticId(antibioticId);
        reading.setMethod(method.name());
        reading.setRawValue(rawValue);
        reading.setRawText(rawValue == null ? null : rawValue.toPlainString());
        reading.setInterpretation(interpretation.name());
        reading.setBreakpointRuleId(rule == null ? null : rule.getId());
        reading.setSource("MANUAL_ENTRY");
        reading.setMatchedBy(matchedBy(rule));
        reading.setUnits(rule != null && rule.getUnits() != null && !rule.getUnits().isBlank() ? rule.getUnits()
                : defaultUnits(method));
        reading.setCreatedAt(MicroCaseServiceImpl.now());
        reading.setCreatedBy(performedBy);
        readingDAO.insert(reading);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_READING_RECORDED, performedBy,
                "AST reading recorded", "{\"astRunId\":\"" + runId + "\",\"readingId\":\"" + reading.getId() + "\"}");
        return reading;
    }

    @Override
    @Transactional
    public MicroAstReading overrideReading(String readingId, MicroAstInterpretation overrideInterpretation,
            String overrideReason, String performedBy) {
        MicroCaseServiceImpl.requireText(readingId, "readingId");
        interpretationService.validateOverride(overrideInterpretation, overrideReason);
        MicroAstReading reading = readingDAO.get(readingId)
                .orElseThrow(() -> new IllegalArgumentException("AST reading not found"));
        MicroAstRun run = runDAO.get(reading.getAstRunId())
                .orElseThrow(() -> new IllegalArgumentException("AST run not found"));
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableRun(run, isolate.getCaseId());
        String fromInterpretation = effectiveInterpretation(reading);
        reading.setOverrideInterpretation(overrideInterpretation.name());
        reading.setOverrideReason(overrideReason.trim());
        recordOverrideEvent(readingId, MicroAstOverrideAction.OVERRIDE, fromInterpretation,
                overrideInterpretation.name(), overrideReason, performedBy);
        MicroAstReading updated = readingDAO.update(reading);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_READING_OVERRIDDEN, performedBy,
                "AST interpretation overridden", "{\"readingId\":\"" + readingId + "\"}");
        return updated;
    }

    @Override
    @Transactional
    public MicroAstReading revertOverride(String readingId, String reason, String performedBy) {
        MicroCaseServiceImpl.requireText(readingId, "readingId");
        MicroCaseServiceImpl.requireText(reason, "reason");
        MicroAstReading reading = readingDAO.get(readingId)
                .orElseThrow(() -> new IllegalArgumentException("AST reading not found"));
        if (reading.getOverrideInterpretation() == null || reading.getOverrideInterpretation().isBlank()) {
            throw new MicroAstConflictException("AST_OVERRIDE_NOT_ACTIVE");
        }
        MicroAstRun run = runDAO.get(reading.getAstRunId())
                .orElseThrow(() -> new IllegalArgumentException("AST run not found"));
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableRun(run, isolate.getCaseId());
        recordOverrideEvent(readingId, MicroAstOverrideAction.REVERT, reading.getOverrideInterpretation(),
                reading.getInterpretation(), reason, performedBy);
        reading.setOverrideInterpretation(null);
        reading.setOverrideReason(null);
        MicroAstReading updated = readingDAO.update(reading);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_READING_OVERRIDDEN, performedBy,
                "AST interpretation override reverted", "{\"readingId\":\"" + readingId + "\"}");
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstOverrideEventForm> getOverrideHistoryForRun(String runId) {
        MicroCaseServiceImpl.requireText(runId, "runId");
        Map<String, String> userDisplayById = new HashMap<>();
        return overrideEventDAO.getByRunId(runId).stream().map(event -> {
            MicroAstOverrideEventForm form = new MicroAstOverrideEventForm();
            form.id = event.getId();
            form.readingId = event.getReadingId();
            form.action = event.getAction();
            form.fromInterpretation = event.getFromInterpretation();
            form.toInterpretation = event.getToInterpretation();
            form.reason = event.getReason();
            form.performedAt = event.getPerformedAt();
            form.performedBy = event.getPerformedBy();
            form.performedByDisplay = resolveUserDisplay(event.getPerformedBy(), userDisplayById);
            return form;
        }).toList();
    }

    @Override
    @Transactional
    public MicroAstRun reviewRun(String runId, String performedBy) {
        MicroCaseServiceImpl.requireText(runId, "runId");
        MicroAstRun run = runDAO.get(runId).orElseThrow(() -> new IllegalArgumentException("AST run not found"));
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableRun(run, isolate.getCaseId());
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        run.setReviewedAt(MicroCaseServiceImpl.now());
        run.setReviewedBy(performedBy);
        List<MicroAstRun> reviewedSiblings = runDAO.getByIsolateId(run.getIsolateId()).stream()
                .filter(candidate -> !run.getId().equals(candidate.getId()))
                .filter(candidate -> MicroAstRunStatus.REVIEWED.name().equals(candidate.getStatus())).toList();
        if (reviewedSiblings.isEmpty()) {
            run.setReportable(true);
        } else {
            run.setReportable(false);
            for (MicroAstRun sibling : reviewedSiblings) {
                if (sibling.isReportable()) {
                    sibling.setReportable(false);
                    runDAO.update(sibling);
                }
            }
        }
        MicroAstRun updated = runDAO.update(run);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_REVIEWED, performedBy, "AST reviewed",
                "{\"astRunId\":\"" + runId + "\"}");
        return updated;
    }

    @Override
    @Transactional
    public MicroAstRun selectReportableRun(String runId, String performedBy) {
        MicroCaseServiceImpl.requireText(runId, "runId");
        MicroAstRun selected = runDAO.get(runId).orElseThrow(() -> new IllegalArgumentException("AST run not found"));
        if (!MicroAstRunStatus.REVIEWED.name().equals(selected.getStatus())) {
            throw new MicroAstConflictException("REPORTABLE_AST_RUN_MUST_BE_REVIEWED");
        }
        MicroIsolate isolate = isolateDAO.get(selected.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableCase(isolate.getCaseId());
        for (MicroAstRun run : runDAO.getByIsolateId(selected.getIsolateId())) {
            if (run.isReportable()) {
                run.setReportable(false);
                runDAO.update(run);
            }
        }
        selected.setReportable(true);
        runDAO.update(selected);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_REPORTABLE_SELECTED, performedBy,
                "Reportable AST attempt selected", "{\"astRunId\":\"" + selected.getId() + "\"}");
        return selected;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRun> getRunsForIsolate(String isolateId) {
        return runDAO.getByIsolateId(isolateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstReading> getReadingsForRun(String runId) {
        return readingDAO.getByRunId(runId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRunAntibiotic> getOrderedAntibioticsForRun(String runId) {
        return runAntibioticDAO.getByRunId(runId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstPanelAntibiotic> getPanelAntibiotics(String panelId) {
        MicroCaseServiceImpl.requireText(panelId, "panelId");
        return panelAntibioticDAO.getByPanelId(panelId);
    }

    private void snapshotPanelAntibiotics(String runId, String panelId) {
        snapshotOrderedAntibiotics(runId, resolveOrderedAntibiotics(panelId, null).antibiotics);
    }

    private void snapshotOrderedAntibiotics(String runId, List<OrderedAntibiotic> antibiotics) {
        for (OrderedAntibiotic source : antibiotics) {
            MicroAstRunAntibiotic ordered = new MicroAstRunAntibiotic();
            ordered.setAstRunId(runId);
            ordered.setAntibioticId(source.antibioticId);
            ordered.setDisplayOrder(source.displayOrder);
            ordered.setTier(source.tier);
            ordered.setReportBehavior(source.reportBehavior);
            runAntibioticDAO.insert(ordered);
        }
    }

    private void copyOrderedAntibiotics(MicroAstRun source, MicroAstRun target) {
        List<MicroAstRunAntibiotic> sourceRows = runAntibioticDAO.getByRunId(source.getId());
        if (sourceRows.isEmpty()) {
            snapshotPanelAntibiotics(target.getId(), source.getPanelId());
            return;
        }
        for (MicroAstRunAntibiotic sourceRow : sourceRows) {
            MicroAstRunAntibiotic ordered = new MicroAstRunAntibiotic();
            ordered.setAstRunId(target.getId());
            ordered.setAntibioticId(sourceRow.getAntibioticId());
            ordered.setDisplayOrder(sourceRow.getDisplayOrder());
            ordered.setTier(sourceRow.getTier());
            ordered.setReportBehavior(sourceRow.getReportBehavior());
            runAntibioticDAO.insert(ordered);
        }
    }

    /**
     * Resolves the breakpoint standard to interpret against: the run's snapshotted
     * choice (M-05: selected at setup) when present, otherwise the configured
     * default so runs started before this field existed, or without an explicit
     * choice, keep working.
     */
    private MicroBreakpointRule findRule(MicroAstRun run, MicroIsolate isolate, MicroOrganism organism,
            String antibioticId, MicroAstMethod method) {
        String standardId = run.getBreakpointStandardId();
        if (standardId == null || standardId.trim().isEmpty()) {
            MicroBreakpointStandard standard = breakpointService.getActiveStandard(DEFAULT_BREAKPOINT_AUTHORITY,
                    DEFAULT_BREAKPOINT_VERSION);
            if (standard == null) {
                return null;
            }
            standardId = standard.getId();
        }
        String organismGroup = organism == null ? null : organism.getOrganismGroup();
        String technique = run.getTechnique();
        if (technique != null && !technique.isBlank() && !MicroAstTechnique.valueOf(technique).isLegacyUnspecified()) {
            MicroBreakpointRule techniqueRule = breakpointService.findBreakpointRule(standardId,
                    isolate.getOrganismId(), organismGroup, antibioticId, technique, null, method.name());
            if (techniqueRule != null) {
                return techniqueRule;
            }
        }
        return breakpointService.findBreakpointRule(standardId, isolate.getOrganismId(), organismGroup, antibioticId,
                method.name(), null, method.name());
    }

    private String matchedBy(MicroBreakpointRule rule) {
        if (rule == null) {
            return "NONE";
        }
        if (rule.getSpecimenTypeId() != null && !rule.getSpecimenTypeId().isBlank()) {
            return "SPECIMEN";
        }
        if (rule.getOrganismId() != null && !rule.getOrganismId().isBlank()) {
            return "ORGANISM";
        }
        if (rule.getOrganismGroup() != null && !rule.getOrganismGroup().isBlank()) {
            return "GROUP";
        }
        return "NONE";
    }

    private String defaultUnits(MicroAstMethod method) {
        return MicroAstMethod.ZONE.equals(method) ? "mm" : "ug/mL";
    }

    private String effectiveInterpretation(MicroAstReading reading) {
        return reading.getOverrideInterpretation() == null || reading.getOverrideInterpretation().isBlank()
                ? reading.getInterpretation()
                : reading.getOverrideInterpretation();
    }

    private void recordOverrideEvent(String readingId, MicroAstOverrideAction action, String fromInterpretation,
            String toInterpretation, String reason, String performedBy) {
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        MicroAstOverrideEvent event = new MicroAstOverrideEvent();
        event.setReadingId(readingId);
        event.setAction(action.name());
        event.setFromInterpretation(fromInterpretation);
        event.setToInterpretation(toInterpretation);
        event.setReason(reason.trim());
        event.setPerformedAt(MicroCaseServiceImpl.now());
        event.setPerformedBy(performedBy);
        overrideEventDAO.insert(event);
    }

    private String resolveUserDisplay(String userId, Map<String, String> userDisplayById) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        if (userDisplayById.containsKey(userId)) {
            return userDisplayById.get(userId);
        }
        SystemUser user = systemUserService.getUserById(userId);
        String display = userId;
        if (user != null) {
            String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
            String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
            String fullName = (firstName + " " + lastName).trim();
            display = fullName.isEmpty() ? userId : fullName;
        }
        userDisplayById.put(userId, display);
        return display;
    }

    private MicroAstRun requireRun(String runId) {
        MicroCaseServiceImpl.requireText(runId, "runId");
        return runDAO.get(runId).orElseThrow(() -> new IllegalArgumentException("AST run not found"));
    }

    private MicroAstMethod measurementTypeFor(MicroAstRun run) {
        if (run.getTechnique() != null && !run.getTechnique().isBlank()) {
            MicroAstMethod derived = MicroAstTechnique.valueOf(run.getTechnique()).measurementType();
            if (run.getMethod() != null && !run.getMethod().isBlank() && !derived.name().equals(run.getMethod())) {
                throw new MicroAstConflictException("AST_RUN_TECHNIQUE_MEASUREMENT_MISMATCH");
            }
            if (run.getMethod() == null || run.getMethod().isBlank()) {
                run.setMethod(derived.name());
                runDAO.update(run);
            }
            return derived;
        }
        if (run.getMethod() == null || run.getMethod().isBlank()) {
            throw new MicroAstConflictException("AST_RUN_MEASUREMENT_TYPE_REQUIRED");
        }
        return MicroAstMethod.valueOf(run.getMethod());
    }

    private void requireAttemptReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("AST_ATTEMPT_REASON_REQUIRED");
        }
    }

    private PanelSelection resolvePanel(MicroOrganism organism, String requestedPanelId) {
        String orderedPanelId = organism.getDefaultAstPanelId();
        String selectedPanelId = requestedPanelId == null || requestedPanelId.isBlank() ? orderedPanelId
                : requestedPanelId;
        if (selectedPanelId == null || selectedPanelId.isBlank()) {
            throw new IllegalStateException("AST_ORDERED_PANEL_REQUIRED");
        }
        MicroAstPanel panel = panelDAO.get(selectedPanelId)
                .orElseThrow(() -> new IllegalArgumentException("AST_PANEL_NOT_FOUND"));
        boolean adjusted = orderedPanelId == null || !orderedPanelId.equals(selectedPanelId);
        return new PanelSelection(panel, adjusted);
    }

    private OrderedSelection resolveOrderedAntibiotics(String panelId, List<String> requestedAntibioticIds) {
        List<MicroAstPanelAntibiotic> panelRows = panelAntibioticDAO.getByPanelId(panelId);
        if (panelRows.isEmpty()) {
            throw new IllegalStateException("AST_PANEL_HAS_NO_ANTIBIOTICS");
        }
        List<String> baselineIds = panelRows.stream().map(MicroAstPanelAntibiotic::getAntibioticId).toList();
        List<String> requestedIds = requestedAntibioticIds == null ? baselineIds
                : normalizeOrderedAntibioticIds(requestedAntibioticIds);
        Map<String, MicroAstPanelAntibiotic> panelByAntibiotic = new HashMap<>();
        for (MicroAstPanelAntibiotic row : panelRows) {
            panelByAntibiotic.put(row.getAntibioticId(), row);
        }
        List<OrderedAntibiotic> ordered = new ArrayList<>();
        int displayOrder = 1;
        for (String antibioticId : requestedIds) {
            MicroAstPanelAntibiotic panelRow = panelByAntibiotic.get(antibioticId);
            if (panelRow == null) {
                MicroAntibiotic antibiotic = antibioticDAO.get(antibioticId)
                        .orElseThrow(() -> new IllegalArgumentException("AST_ANTIBIOTIC_NOT_FOUND"));
                if (!"Y".equalsIgnoreCase(antibiotic.getIsActive())) {
                    throw new IllegalArgumentException("AST_ANTIBIOTIC_NOT_ACTIVE");
                }
            }
            ordered.add(new OrderedAntibiotic(antibioticId, displayOrder++, panelRow == null ? 1 : panelRow.getTier(),
                    panelRow == null ? "ALWAYS" : panelRow.getReportBehavior()));
        }
        return new OrderedSelection(ordered, !requestedIds.equals(baselineIds));
    }

    private List<String> normalizeOrderedAntibioticIds(List<String> requestedAntibioticIds) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String antibioticId : requestedAntibioticIds) {
            if (antibioticId == null || antibioticId.isBlank() || !normalized.add(antibioticId.trim())) {
                throw new IllegalArgumentException("AST_ORDERED_ANTIBIOTICS_INVALID");
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("AST_ORDERED_ANTIBIOTICS_REQUIRED");
        }
        return List.copyOf(normalized);
    }

    private MicroBreakpointStandard resolveStandard(String requestedStandardId) {
        if (requestedStandardId != null && !requestedStandardId.isBlank()) {
            MicroBreakpointStandard selected = breakpointService.getStandard(requestedStandardId);
            if (selected == null || "N".equals(selected.getIsActive())
                    || "ARCHIVED".equals(selected.getLifecycleStatus())) {
                throw new IllegalArgumentException("AST_BREAKPOINT_STANDARD_NOT_AVAILABLE");
            }
            return selected;
        }
        List<MicroBreakpointStandard> active = breakpointService.getActiveStandards().stream()
                .filter(standard -> "ACTIVE".equals(standard.getLifecycleStatus())).toList();
        if (active.size() != 1) {
            throw new IllegalArgumentException("AST_BREAKPOINT_STANDARD_REQUIRED");
        }
        return active.get(0);
    }

    private record PanelSelection(MicroAstPanel panel, boolean adjusted) {
    }

    private record OrderedSelection(List<OrderedAntibiotic> antibiotics, boolean adjusted) {
    }

    private record OrderedAntibiotic(String antibioticId, Integer displayOrder, Integer tier, String reportBehavior) {
    }

    private MicroCase requireMutableCase(String caseId) {
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);
        return microCase;
    }

    private void requireMutableRun(MicroAstRun run, String caseId) {
        MicroCase microCase = requireMutableCase(caseId);
        if (!isAmendmentInProgress(microCase)) {
            return;
        }
        MicroCaseAmendment amendment = requireOpenAmendment(caseId);
        if (!amendment.getId().equals(run.getAmendmentId())) {
            throw new MicroAmendmentConflictException("AMENDMENT_NEW_AST_RUN_REQUIRED");
        }
    }

    private MicroCaseAmendment requireOpenAmendment(String caseId) {
        MicroCaseAmendment amendment = amendmentDAO.getOpenByCaseId(caseId);
        if (amendment == null) {
            throw new MicroAmendmentConflictException("AMENDMENT_NOT_OPEN");
        }
        return amendment;
    }

    private boolean isAmendmentInProgress(MicroCase microCase) {
        return MicroCaseStage.AMENDED.name().equals(microCase.getStage())
                && MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name().equals(microCase.getFinalReleaseState());
    }

    private void recordActivity(String caseId, MicroCaseActivityType activityType, String performedBy, String note,
            String structuredData) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(activityType.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote(note);
        activity.setStructuredData(structuredData);
        activityDAO.insert(activity);
    }
}
