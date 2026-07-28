package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
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

    public MicroAstServiceImpl(MicroAstRunDAO runDAO, MicroAstReadingDAO readingDAO, MicroIsolateDAO isolateDAO,
            MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO, MicroBreakpointService breakpointService,
            MicroAstInterpretationService interpretationService) {
        this.runDAO = runDAO;
        this.readingDAO = readingDAO;
        this.isolateDAO = isolateDAO;
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.breakpointService = breakpointService;
        this.interpretationService = interpretationService;
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String performedBy) {
        return startRun(isolateId, panelId, null, performedBy);
    }

    @Override
    @Transactional
    public MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String performedBy) {
        MicroCaseServiceImpl.requireText(isolateId, "isolateId");
        MicroIsolate isolate = isolateDAO.get(isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableCase(isolate.getCaseId());
        MicroAstRun run = new MicroAstRun();
        run.setIsolateId(isolateId);
        run.setPanelId(panelId);
        run.setBreakpointStandardId(breakpointStandardId);
        run.setStatus(MicroAstRunStatus.IN_PROGRESS.name());
        run.setStartedAt(MicroCaseServiceImpl.now());
        run.setStartedBy(performedBy);
        runDAO.insert(run);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_RUN_CREATED, performedBy, "AST run created",
                "{\"astRunId\":\"" + run.getId() + "\"}");
        return run;
    }

    @Override
    @Transactional
    public MicroAstReading recordReading(String runId, String antibioticId, MicroAstMethod method, BigDecimal rawValue,
            String performedBy) {
        MicroCaseServiceImpl.requireText(runId, "runId");
        MicroCaseServiceImpl.requireText(antibioticId, "antibioticId");
        if (method == null) {
            throw new IllegalArgumentException("method is required");
        }
        MicroAstRun run = runDAO.get(runId).orElseThrow(() -> new IllegalArgumentException("AST run not found"));
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableCase(isolate.getCaseId());
        MicroBreakpointRule rule = findRule(run, isolate, antibioticId, method);
        MicroAstInterpretation interpretation = interpretationService.interpret(rule, method, rawValue);

        MicroAstReading reading = new MicroAstReading();
        reading.setAstRunId(runId);
        reading.setAntibioticId(antibioticId);
        reading.setMethod(method.name());
        reading.setRawValue(rawValue);
        reading.setRawText(rawValue == null ? null : rawValue.toPlainString());
        reading.setInterpretation(interpretation.name());
        reading.setBreakpointRuleId(rule == null ? null : rule.getId());
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
        requireMutableCase(isolate.getCaseId());
        reading.setOverrideInterpretation(overrideInterpretation.name());
        reading.setOverrideReason(overrideReason);
        MicroAstReading updated = readingDAO.update(reading);
        recordActivity(isolate.getCaseId(), MicroCaseActivityType.AST_READING_OVERRIDDEN, performedBy,
                "AST interpretation overridden", "{\"readingId\":\"" + readingId + "\"}");
        return updated;
    }

    @Override
    @Transactional
    public MicroAstRun reviewRun(String runId, String performedBy) {
        MicroCaseServiceImpl.requireText(runId, "runId");
        MicroAstRun run = runDAO.get(runId).orElseThrow(() -> new IllegalArgumentException("AST run not found"));
        MicroIsolate isolate = isolateDAO.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        requireMutableCase(isolate.getCaseId());
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

    /**
     * Resolves the breakpoint standard to interpret against: the run's snapshotted
     * choice (M-05: selected at setup) when present, otherwise the configured
     * default so runs started before this field existed, or without an explicit
     * choice, keep working.
     */
    private MicroBreakpointRule findRule(MicroAstRun run, MicroIsolate isolate, String antibioticId,
            MicroAstMethod method) {
        String standardId = run.getBreakpointStandardId();
        if (standardId == null || standardId.trim().isEmpty()) {
            MicroBreakpointStandard standard = breakpointService.getActiveStandard(DEFAULT_BREAKPOINT_AUTHORITY,
                    DEFAULT_BREAKPOINT_VERSION);
            if (standard == null) {
                return null;
            }
            standardId = standard.getId();
        }
        return breakpointService.findBreakpointRule(standardId, isolate.getOrganismId(), null, antibioticId,
                method.name(), null, method.name());
    }

    private void requireMutableCase(String caseId) {
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseMutationGuard.requireMutable(microCase);
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
