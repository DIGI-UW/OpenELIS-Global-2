package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstOverrideEventDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroAstOverrideEventForm;
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
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
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroAstServiceImpl implements MicroAstService {

    private final MicroAstRunDAO runDAO;
    private final MicroAstReadingDAO readingDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroBreakpointService breakpointService;
    private final MicroAstInterpretationService interpretationService;
    private final MicroAstPanelDAO panelDAO;
    private final MicroOrganismDAO organismDAO;
    private final MicroAstOverrideEventDAO overrideEventDAO;
    private final SystemUserService systemUserService;
    private final MicroAstPanelAntibioticDAO panelAntibioticDAO;
    private final MicroAstRunAntibioticDAO runAntibioticDAO;
    private final MicroAntibioticDAO antibioticDAO;

    public MicroAstServiceImpl(MicroAstRunDAO runDAO, MicroAstReadingDAO readingDAO, MicroIsolateDAO isolateDAO,
            MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, MicroBreakpointService breakpointService,
            MicroAstInterpretationService interpretationService, MicroAstPanelDAO panelDAO,
            MicroOrganismDAO organismDAO, MicroAstOverrideEventDAO overrideEventDAO,
            SystemUserService systemUserService, MicroAstPanelAntibioticDAO panelAntibioticDAO,
            MicroAstRunAntibioticDAO runAntibioticDAO, MicroAntibioticDAO antibioticDAO) {
        this.runDAO = runDAO;
        this.readingDAO = readingDAO;
        this.isolateDAO = isolateDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.breakpointService = breakpointService;
        this.interpretationService = interpretationService;
        this.panelDAO = panelDAO;
        this.organismDAO = organismDAO;
        this.overrideEventDAO = overrideEventDAO;
        this.systemUserService = systemUserService;
        this.panelAntibioticDAO = panelAntibioticDAO;
        this.runAntibioticDAO = runAntibioticDAO;
        this.antibioticDAO = antibioticDAO;
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String performedBy) {
        return startRun(isolateId, panelId, null, null, MicroAstTechnique.LEGACY_UNSPECIFIED_MIC, null, performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String performedBy) {
        return startRun(isolateId, panelId, breakpointStandardId, null, MicroAstTechnique.LEGACY_UNSPECIFIED_MIC, null,
                performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            String panelAdjustmentReason, String performedBy) {
        return startRun(isolateId, panelId, breakpointStandardId, panelAdjustmentReason,
                MicroAstTechnique.LEGACY_UNSPECIFIED_MIC, null, performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            String panelAdjustmentReason, MicroAstTechnique technique, List<String> orderedAntibioticIds,
            String performedBy) {
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        if (technique == null) {
            throw new IllegalArgumentException("AST_TECHNIQUE_REQUIRED");
        }
        MicroIsolate isolate = isolateDAO.get(isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        if (!MicroIsolateIdentificationStatus.CONFIRMED.name().equals(isolate.getIdentificationStatus())
                || isBlank(isolate.getOrganismId())) {
            throw new IllegalStateException("AST_ISOLATE_IDENTIFICATION_REQUIRED");
        }
        requireMutableCase(isolate.getCaseId());
        MicroOrganism organism = organismDAO.get(isolate.getOrganismId())
                .orElseThrow(() -> new IllegalArgumentException("AST_ORGANISM_NOT_FOUND"));
        PanelSelection panelSelection = resolvePanel(organism, panelId);
        OrderedSelection orderedSelection = resolveOrderedAntibiotics(panelSelection.panel().getId(),
                orderedAntibioticIds);
        boolean adjusted = panelSelection.adjusted() || orderedSelection.adjusted();
        if (adjusted && isBlank(panelAdjustmentReason)) {
            throw new IllegalArgumentException("AST_PANEL_ADJUSTMENT_REASON_REQUIRED");
        }
        MicroBreakpointStandard standard = resolveBreakpointStandard(breakpointStandardId);

        MicroAstRun run = new MicroAstRun();
        run.setIsolateId(isolateId);
        run.setPanelId(panelSelection.panel().getId());
        run.setPanelVersion(panelSelection.panel().getVersionNumber());
        run.setPanelProvenance(adjusted ? "ADJUSTED" : "ORGANISM_DEFAULT");
        run.setPanelAdjustmentReason(adjusted ? panelAdjustmentReason.trim() : null);
        run.setBreakpointStandardId(standard.getId());
        run.setBreakpointVersion(standard.getVersion());
        run.setTechnique(technique.name());
        run.setMethod(technique.measurementType().name());
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        run.setStartedAt(MicroCaseServiceImpl.now());
        run.setStartedBy(performedBy);
        runDAO.insert(run);
        snapshotOrderedAntibiotics(run.getId(), orderedSelection.antibiotics());
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_RUN_CREATED, performedBy, "AST run created",
                "{\"astRunId\":\"" + run.getId() + "\"}");
        return run;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroAstSetupForm getSetup(String isolateId) {
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        MicroIsolate isolate = isolateDAO.get(isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        if (!MicroIsolateIdentificationStatus.CONFIRMED.name().equals(isolate.getIdentificationStatus())
                || isBlank(isolate.getOrganismId())) {
            throw new IllegalStateException("AST_ISOLATE_IDENTIFICATION_REQUIRED");
        }
        MicroOrganism organism = organismDAO.get(isolate.getOrganismId())
                .orElseThrow(() -> new IllegalArgumentException("AST_ORGANISM_NOT_FOUND"));
        MicroAstSetupForm form = new MicroAstSetupForm();
        form.isolateId = isolateId;
        form.panelProvenance = "UNASSIGNED";
        if (!isBlank(organism.getDefaultAstPanelId())) {
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
    @Transactional(readOnly = true)
    public List<MicroAstPanelAntibiotic> getPanelAntibiotics(String panelId) {
        MicroCaseServiceImpl.requireText(panelId, "panelId");
        MicroAstPanel panel = panelDAO.get(panelId)
                .orElseThrow(() -> new IllegalArgumentException("AST_PANEL_NOT_FOUND"));
        if (!"Y".equalsIgnoreCase(panel.getIsActive())) {
            throw new IllegalArgumentException("AST_PANEL_NOT_AVAILABLE");
        }
        return panelAntibioticDAO.getByPanelId(panelId);
    }

    @Override
    @Transactional
    public MicroAstReading recordReading(String runId, String antibioticId, MicroAstMethod method, BigDecimal rawValue,
            String performedBy) {
        MicroAstRun run = requireRun(runId);
        MicroAstMethod expected = measurementTypeFor(run);
        if (method == null || !expected.equals(method)) {
            throw new IllegalStateException("AST_RUN_MEASUREMENT_TYPE_MISMATCH");
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
        MicroCaseServiceImpl.requireText(antibioticId, "antibioticId");
        requireEditableRun(run);
        if (runAntibioticDAO.getByRunIdAndAntibioticId(run.getId(), antibioticId).isEmpty()) {
            throw new IllegalStateException("AST_ANTIBIOTIC_NOT_ORDERED");
        }
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableCase(isolate.getCaseId());
        MicroBreakpointRule rule = findRule(run, isolate, antibioticId, method);
        MicroAstInterpretation interpretation = interpretationService.interpret(rule, method, rawValue);

        MicroAstReading reading = new MicroAstReading();
        reading.setAstRunId(run.getId());
        reading.setAntibioticId(antibioticId);
        reading.setMethod(method.name());
        reading.setRawValue(rawValue);
        reading.setRawText(rawValue == null ? null : rawValue.toPlainString());
        reading.setInterpretation(interpretation.name());
        reading.setBreakpointRuleId(rule == null ? null : rule.getId());
        reading.setSource("MANUAL_ENTRY");
        reading.setMatchedBy(matchedBy(rule));
        reading.setUnits(defaultUnits(method));
        reading.setCreatedAt(MicroCaseServiceImpl.now());
        reading.setCreatedBy(performedBy);
        readingDAO.insert(reading);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_READING_RECORDED, performedBy,
                "AST reading recorded",
                "{\"astRunId\":\"" + run.getId() + "\",\"readingId\":\"" + reading.getId() + "\"}");
        return reading;
    }

    @Override
    @Transactional
    public MicroAstReading overrideReading(String readingId, MicroAstInterpretation overrideInterpretation,
            String overrideReason, String performedBy) {
        MicroCaseServiceImpl.requireText(readingId, "readingId");
        if (overrideInterpretation == null) {
            throw new IllegalArgumentException("overrideInterpretation is required");
        }
        interpretationService.validateOverride(overrideInterpretation, overrideReason);
        MicroAstReading reading = readingDAO.get(readingId)
                .orElseThrow(() -> new IllegalArgumentException("AST reading not found"));
        MicroAstRun run = requireRun(reading.getAstRunId());
        requireEditableRun(run);
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableCase(isolate.getCaseId());
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
        if (isBlank(reading.getOverrideInterpretation())) {
            throw new IllegalStateException("AST_OVERRIDE_NOT_ACTIVE");
        }
        MicroAstRun run = requireRun(reading.getAstRunId());
        requireEditableRun(run);
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableCase(isolate.getCaseId());
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
        List<MicroAstOverrideEventForm> history = new ArrayList<>();
        for (MicroAstOverrideEvent event : overrideEventDAO.getByRunId(runId)) {
            MicroAstOverrideEventForm form = new MicroAstOverrideEventForm();
            form.id = event.getId();
            form.readingId = event.getReadingId();
            form.action = event.getAction();
            form.fromInterpretation = event.getFromInterpretation();
            form.toInterpretation = event.getToInterpretation();
            form.reason = event.getReason();
            form.performedAt = event.getPerformedAt();
            form.performedBy = event.getPerformedBy();
            form.performedByDisplay = MicrobiologyUserDisplayResolver.resolve(systemUserService, event.getPerformedBy(),
                    userDisplayById);
            history.add(form);
        }
        return history;
    }

    @Override
    @Transactional
    public MicroAstRun reviewRun(String runId, String performedBy) {
        MicroAstRun run = requireRun(runId);
        requireEditableRun(run);
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableCase(isolate.getCaseId());
        requireCompleteOrderedResults(runId);
        run.setStatus(MicroAstRunStatus.REVIEWED.name());
        run.setReviewedAt(MicroCaseServiceImpl.now());
        run.setReviewedBy(performedBy);
        MicroAstRun updated = runDAO.update(run);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_REVIEWED, performedBy, "AST reviewed",
                "{\"astRunId\":\"" + runId + "\"}");
        return updated;
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

    private void requireCompleteOrderedResults(String runId) {
        List<MicroAstRunAntibiotic> ordered = runAntibioticDAO.getByRunId(runId);
        if (ordered.isEmpty()) {
            throw new IllegalStateException("AST_ORDERED_ANTIBIOTICS_REQUIRED");
        }
        List<MicroAstReading> readings = readingDAO.getByRunId(runId);
        Set<String> covered = new LinkedHashSet<>();
        Set<String> unresolved = new LinkedHashSet<>();
        for (MicroAstReading reading : readings) {
            covered.add(reading.getAntibioticId());
            if (MicroAstInterpretation.NO_BREAKPOINT.name().equals(reading.getInterpretation())
                    && isBlank(reading.getOverrideInterpretation())) {
                unresolved.add(reading.getAntibioticId());
            } else {
                unresolved.remove(reading.getAntibioticId());
            }
        }
        for (MicroAstRunAntibiotic antibiotic : ordered) {
            if (!covered.contains(antibiotic.getAntibioticId())) {
                throw new IllegalStateException("AST_ORDERED_RESULTS_INCOMPLETE");
            }
            if (unresolved.contains(antibiotic.getAntibioticId())) {
                throw new IllegalStateException("AST_NO_BREAKPOINT_UNRESOLVED");
            }
        }
    }

    private MicroBreakpointRule findRule(MicroAstRun run, MicroIsolate isolate, String antibioticId,
            MicroAstMethod method) {
        if (isBlank(run.getBreakpointStandardId())) {
            throw new IllegalStateException("AST_BREAKPOINT_STANDARD_REQUIRED");
        }
        String organismGroup = organismDAO.get(isolate.getOrganismId()).map(MicroOrganism::getOrganismGroup)
                .orElse(null);
        return breakpointService.findBreakpointRule(run.getBreakpointStandardId(), isolate.getOrganismId(),
                organismGroup, antibioticId, method.name(), null, method.name());
    }

    private String matchedBy(MicroBreakpointRule rule) {
        if (rule == null) {
            return "NONE";
        }
        if (!isBlank(rule.getSpecimenTypeId())) {
            return "SPECIMEN";
        }
        if (!isBlank(rule.getOrganismId())) {
            return "ORGANISM";
        }
        if (!isBlank(rule.getOrganismGroup())) {
            return "GROUP";
        }
        return "STANDARD";
    }

    private String defaultUnits(MicroAstMethod method) {
        return MicroAstMethod.ZONE.equals(method) ? "mm" : "ug/mL";
    }

    private MicroAstRun requireRun(String runId) {
        MicroCaseServiceImpl.requireText(runId, "runId");
        return runDAO.get(runId).orElseThrow(() -> new IllegalArgumentException("AST run not found"));
    }

    private MicroAstMethod measurementTypeFor(MicroAstRun run) {
        if (!isBlank(run.getTechnique())) {
            MicroAstMethod derived = MicroAstTechnique.valueOf(run.getTechnique()).measurementType();
            if (!isBlank(run.getMethod()) && !derived.name().equals(run.getMethod())) {
                throw new IllegalStateException("AST_RUN_TECHNIQUE_MEASUREMENT_MISMATCH");
            }
            return derived;
        }
        if (isBlank(run.getMethod())) {
            throw new IllegalStateException("AST_RUN_MEASUREMENT_TYPE_REQUIRED");
        }
        return MicroAstMethod.valueOf(run.getMethod());
    }

    private PanelSelection resolvePanel(MicroOrganism organism, String requestedPanelId) {
        String orderedPanelId = organism.getDefaultAstPanelId();
        String selectedPanelId = isBlank(requestedPanelId) ? orderedPanelId : requestedPanelId.trim();
        if (isBlank(selectedPanelId)) {
            throw new IllegalStateException("AST_ORDERED_PANEL_REQUIRED");
        }
        MicroAstPanel panel = panelDAO.get(selectedPanelId)
                .orElseThrow(() -> new IllegalArgumentException("AST_PANEL_NOT_FOUND"));
        if (!"Y".equalsIgnoreCase(panel.getIsActive())) {
            throw new IllegalArgumentException("AST_PANEL_NOT_AVAILABLE");
        }
        return new PanelSelection(panel, isBlank(orderedPanelId) || !orderedPanelId.equals(selectedPanelId));
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
        List<OrderedAntibiotic> selected = new ArrayList<>();
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
            selected.add(new OrderedAntibiotic(antibioticId, displayOrder++, panelRow == null ? 1 : panelRow.getTier(),
                    panelRow == null ? "ALWAYS" : panelRow.getReportBehavior()));
        }
        return new OrderedSelection(selected, !requestedIds.equals(baselineIds));
    }

    private List<String> normalizeOrderedAntibioticIds(List<String> requestedAntibioticIds) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String antibioticId : requestedAntibioticIds) {
            if (isBlank(antibioticId) || !normalized.add(antibioticId.trim())) {
                throw new IllegalArgumentException("AST_ORDERED_ANTIBIOTICS_INVALID");
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("AST_ORDERED_ANTIBIOTICS_REQUIRED");
        }
        return List.copyOf(normalized);
    }

    private void snapshotOrderedAntibiotics(String runId, List<OrderedAntibiotic> antibiotics) {
        for (OrderedAntibiotic source : antibiotics) {
            MicroAstRunAntibiotic ordered = new MicroAstRunAntibiotic();
            ordered.setAstRunId(runId);
            ordered.setAntibioticId(source.antibioticId());
            ordered.setDisplayOrder(source.displayOrder());
            ordered.setTier(source.tier());
            ordered.setReportBehavior(source.reportBehavior());
            runAntibioticDAO.insert(ordered);
        }
    }

    private MicroBreakpointStandard resolveBreakpointStandard(String requestedStandardId) {
        if (!isBlank(requestedStandardId)) {
            MicroBreakpointStandard selected = breakpointService.getStandard(requestedStandardId.trim());
            if (selected == null || !"Y".equalsIgnoreCase(selected.getIsActive())) {
                throw new IllegalArgumentException("AST_BREAKPOINT_STANDARD_NOT_AVAILABLE");
            }
            return selected;
        }
        List<MicroBreakpointStandard> activeStandards = breakpointService.getActiveStandards();
        if (activeStandards == null || activeStandards.size() != 1) {
            throw new IllegalArgumentException("AST_BREAKPOINT_STANDARD_REQUIRED");
        }
        return activeStandards.get(0);
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

    private String effectiveInterpretation(MicroAstReading reading) {
        return isBlank(reading.getOverrideInterpretation()) ? reading.getInterpretation()
                : reading.getOverrideInterpretation();
    }

    private void requireMutableCase(String caseId) {
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);
    }

    private void requireEditableRun(MicroAstRun run) {
        if (MicroAstRunStatus.REVIEWED.name().equals(run.getStatus())) {
            throw new IllegalStateException("AST_RUN_REVIEWED");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private record PanelSelection(MicroAstPanel panel, boolean adjusted) {
    }

    private record OrderedSelection(List<OrderedAntibiotic> antibiotics, boolean adjusted) {
    }

    private record OrderedAntibiotic(String antibioticId, Integer displayOrder, Integer tier, String reportBehavior) {
    }
}
