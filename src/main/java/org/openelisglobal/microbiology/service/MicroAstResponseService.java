package org.openelisglobal.microbiology.service;

import java.util.*;
import java.util.stream.Collectors;
import org.openelisglobal.microbiology.dao.MicroReportingRuleDAO;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroAstResponseService {
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.microbiology.dao.MicroAntibioticDAO antibiotics;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroReportingRuleDAO reportingRules;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.microbiology.dao.MicroIsolateDAO isolates;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseDAO cases;
    private final MicroAstService astService;

    public MicroAstResponseService(MicroAstService astService) {
        this.astService = astService;
    }

    public MicroAstReadingForm toReadingFormWithHistory(MicroAstReading reading) {
        return toReadingForm(reading, astService.getOverrideHistoryForRun(reading.getAstRunId()));
    }

    public List<MicroAstRunForm> runs(String isolateId) {
        return astService.getRunsForIsolate(isolateId).stream().map(this::toRunFormWithReadings).toList();
    }

    public List<MicroAstRunAntibioticForm> panelAntibiotics(String panelId) {
        return astService.getPanelAntibiotics(panelId).stream().map(ordered -> {
            var f = new MicroAstRunAntibioticForm();
            f.antibioticId = ordered.getAntibioticId();
            f.displayOrder = ordered.getDisplayOrder();
            f.tier = ordered.getTier();
            f.reportBehavior = ordered.getReportBehavior();
            return f;
        }).toList();
    }

    public MicroAstRunForm toRunFormWithReadings(MicroAstRun run) {
        MicroAstRunForm form = toRunForm(run);
        List<MicroAstOverrideEventForm> overrideHistory = astService.getOverrideHistoryForRun(run.getId());
        for (MicroAstReading reading : astService.getReadingsForRun(run.getId())) {
            form.readings.add(toReadingForm(reading, overrideHistory));
        }
        return form;
    }

    public MicroAstRunForm toRunForm(MicroAstRun run) {
        MicroAstRunForm form = new MicroAstRunForm();
        form.id = run.getId();
        form.isolateId = run.getIsolateId();
        form.panelId = run.getPanelId();
        form.panelVersion = run.getPanelVersion();
        form.panelProvenance = run.getPanelProvenance();
        form.panelAdjustmentReason = run.getPanelAdjustmentReason();
        form.breakpointStandardId = run.getBreakpointStandardId();
        form.breakpointVersion = run.getBreakpointVersion();
        form.attemptType = run.getAttemptType();
        form.sourceRunId = run.getSourceRunId();
        form.attemptReason = run.getAttemptReason();
        form.method = run.getMethod();
        form.technique = run.getTechnique();
        form.clinicalContext = run.getClinicalContext();
        form.measurementType = run.getMethod();
        form.reportable = run.isReportable();
        form.status = run.getStatus();
        form.startedAt = run.getStartedAt();
        form.startedBy = run.getStartedBy();
        form.reviewedAt = run.getReviewedAt();
        form.reviewedBy = run.getReviewedBy();
        form.analyzerInstrumentId = run.getAnalyzerInstrumentId();
        form.analyzerCardId = run.getAnalyzerCardId();
        form.analyzerSoftwareVersion = run.getAnalyzerSoftwareVersion();
        form.analyzerOrganismId = run.getAnalyzerOrganismId();
        form.analyzerOrganismName = run.getAnalyzerOrganismName();
        form.analyzerOrganismConfidence = run.getAnalyzerOrganismConfidence();
        form.analyzerExpertFlags = run.getAnalyzerExpertFlags();
        form.instrumentQcReference = run.getInstrumentQcReference();
        form.qcState = run.getQcState();
        form.qcOverrideReason = run.getQcOverrideReason();
        form.qcOverriddenAt = run.getQcOverriddenAt();
        form.qcOverriddenBy = run.getQcOverriddenBy();
        form.analyzerFlagsAcknowledgedAt = run.getAnalyzerFlagsAcknowledgedAt();
        form.analyzerFlagsAcknowledgedBy = run.getAnalyzerFlagsAcknowledgedBy();
        form.analyzerFlagsAcknowledgementReason = run.getAnalyzerFlagsAcknowledgementReason();
        form.analyzerLoadedAt = run.getAnalyzerLoadedAt();
        form.analyzerCompletedAt = run.getAnalyzerCompletedAt();
        form.analyzerMessageCodes = run.getAnalyzerMessageCodes();
        form.sourceEventId = run.getSourceEventId();
        var readings = astService.getReadingsForRun(run.getId());
        form.intrinsicContradiction = intrinsicContradiction(readings);
        var orderedAntibiotics = astService.getOrderedAntibioticsForRun(run.getId());
        boolean released = orderedAntibiotics.stream().anyMatch(ordered -> ordered.getReportRuleId() != null)
                && finalReleased(run);
        Map<String, MicroAstReading> lastReadings = readings.stream().collect(
                Collectors.toMap(MicroAstReading::getAntibioticId, reading -> reading, (first, second) -> second));
        for (MicroAstRunAntibiotic ordered : orderedAntibiotics) {
            MicroAstRunAntibioticForm orderedForm = new MicroAstRunAntibioticForm();
            orderedForm.antibioticId = ordered.getAntibioticId();
            orderedForm.antibioticName = antibiotics.get(ordered.getAntibioticId()).orElseThrow().getDisplayName();
            orderedForm.displayOrder = ordered.getDisplayOrder();
            orderedForm.tier = ordered.getTier();
            orderedForm.reportBehavior = ordered.getReportBehavior();
            orderedForm.reportRuleId = ordered.getReportRuleId();
            if (orderedForm.reportRuleId != null && !released) {
                var last = lastReadings.get(ordered.getAntibioticId());
                var interpretation = last == null ? null : MicroAstReportChoiceService.interpretation(last);
                var rule = reportingRules.get(orderedForm.reportRuleId).orElse(null);
                orderedForm.ruleNote = rule == null ? null : rule.noteFor(interpretation);
            }
            orderedForm.reported = ordered.isReported();
            orderedForm.reportManual = ordered.isReportManual();
            form.orderedAntibiotics.add(orderedForm);
        }
        return form;
    }

    public MicroAstReadingForm toReadingForm(MicroAstReading reading) {
        return toReadingForm(reading, List.of());
    }

    public MicroAstReadingForm toReadingForm(MicroAstReading reading, List<MicroAstOverrideEventForm> overrideHistory) {
        MicroAstReadingForm form = new MicroAstReadingForm();
        form.id = reading.getId();
        form.astRunId = reading.getAstRunId();
        form.antibioticId = reading.getAntibioticId();
        form.measurementType = reading.getMethod();
        form.method = reading.getMethod();
        form.rawValue = reading.getRawValue();
        form.rawText = reading.getRawText();
        form.interpretation = reading.getInterpretation();
        form.breakpointRuleId = reading.getBreakpointRuleId();
        form.source = reading.getSource();
        form.matchedBy = reading.getMatchedBy();
        form.units = reading.getUnits();
        form.instrumentInterpretation = reading.getInstrumentInterpretation();
        form.analyzerResultReference = reading.getAnalyzerResultReference();
        form.overrideInterpretation = reading.getOverrideInterpretation();
        form.overrideReason = reading.getOverrideReason();
        form.overrideRuleId = reading.getOverrideRuleId();
        form.createdAt = reading.getCreatedAt();
        form.createdBy = reading.getCreatedBy();
        form.overrideHistory = overrideHistory.stream().filter(event -> reading.getId().equals(event.readingId))
                .toList();
        return form;
    }

    private boolean intrinsicContradiction(List<MicroAstReading> readings) {
        Map<String, MicroAstReading> latest = new HashMap<>();
        for (MicroAstReading reading : readings) {
            latest.merge(reading.getAntibioticId(), reading, (current,
                    candidate) -> candidate.getEntrySequence() >= current.getEntrySequence() ? candidate : current);
        }
        return latest.values().stream().anyMatch(MicroAstReading::contradictsExpertRule);
    }

    private boolean finalReleased(MicroAstRun run) {
        return isolates.get(run.getIsolateId()).flatMap(isolate -> cases.get(isolate.getCaseId()))
                .map(c -> "FINAL_RELEASED".equals(c.getFinalReleaseState())).orElse(false);
    }
}
