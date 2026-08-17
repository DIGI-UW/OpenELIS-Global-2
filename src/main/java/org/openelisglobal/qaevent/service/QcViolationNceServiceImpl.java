package org.openelisglobal.qaevent.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QcViolationNceServiceImpl implements QcViolationNceService {

    public static final String TRIGGER_SOURCE_QC_VIOLATION = "QC_VIOLATION";
    /**
     * Bench controls that raise the signal without a statistical violation (D4).
     */
    public static final String TRIGGER_SOURCE_BENCH_CONTROL = "QC_BENCH_CONTROL";
    public static final String CAP_NONE_APPLIED = "none_applied";
    public static final String CAP_TIME_24H = "time_24h";
    public static final String CAP_COUNT_50 = "count_50";

    private static final String NCE_SEVERITY_CRITICAL = "CRITICAL";
    private static final String NCE_STATUS_PENDING = "Pending";
    private static final String NCE_CATEGORY_ANALYSIS = "Analysis";
    private static final String NCE_TYPE_QC_FAILURE = "QC failure";
    private static final long CAP_WINDOW_MILLIS = 24L * 60 * 60 * 1000;
    private static final int CAP_MAX_SAMPLES = 50;

    @Autowired
    private NCEventService ncEventService;
    @Autowired
    private NceNumberGeneratorService nceNumberGeneratorService;
    @Autowired
    private NceCategoryService nceCategoryService;
    @Autowired
    private NceTypeService nceTypeService;
    @Autowired
    private NceSpecimenService nceSpecimenService;
    @Autowired
    private NceHistoryService nceHistoryService;
    @Autowired
    private QCResultService qcResultService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;

    @PersistenceContext
    private EntityManager entityManager;

    // REQUIRES_NEW so a failure here (e.g. an NCE-number collision) can never
    // poison the violation's transaction, and a retry starts clean. Trade-off:
    // if the outer violation transaction later rolls back, the NCE survives as
    // an orphan — rare, visible, and harmless next to losing the violation.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NcEvent createNceForViolation(QCRuleViolation violation) {
        NcEvent existing = ncEventService.findByTriggerSource(TRIGGER_SOURCE_QC_VIOLATION, violation.getId());
        if (existing != null) {
            return existing;
        }

        Integer userId = violation.getSystemUserId() != null ? violation.getSystemUserId() : 1;
        String sysUserId = String.valueOf(userId);
        Timestamp violationTime = violation.getViolationDateTime() != null ? violation.getViolationDateTime()
                : new Timestamp(System.currentTimeMillis());
        String analyzerName = resolveAnalyzerName(violation.getInstrumentId());
        String testName = resolveTestName(violation.getTestId());

        // The triggering control carries two things the violation does not: the lab
        // unit
        // (a bench violation has no analyzer to scope by) and when the control actually
        // ran. Those are different facts from violation_date_time, which records when
        // we
        // DETECTED the failure — for an analyzer that is near-identical, but a
        // technician
        // can record a bench control from earlier in the shift, and anchoring its blast
        // radius to row-creation time would hold the wrong patients, or none at all.
        QCResult triggering = resolveTriggeringResult(violation.getTriggeringResultId());
        String testSectionId = violation.getInstrumentId() == null && triggering != null ? triggering.getTestSectionId()
                : null;
        Timestamp windowAnchor = triggering != null && triggering.getRunDateTime() != null ? triggering.getRunDateTime()
                : violationTime;
        AffectedSamples affected = resolveAffectedSamples(violation.getInstrumentId(), testSectionId,
                violation.getTestId(), windowAnchor);

        NcEvent nce = new NcEvent();
        nce.setSysUserId(sysUserId);
        nce.setNceNumber(nceNumberGeneratorService.generateNceNumber());
        nce.setSeverity(NCE_SEVERITY_CRITICAL);
        nce.setStatus(NCE_STATUS_PENDING);
        nce.setAutoGenerated(Boolean.TRUE);
        nce.setWestgardRule(violation.getRuleCode());
        nce.setTriggerSourceType(TRIGGER_SOURCE_QC_VIOLATION);
        nce.setTriggerSourceId(violation.getId());
        nce.setTitle("QC failure — Westgard " + violation.getRuleCode() + " on " + analyzerName);
        nce.setDescription(buildDescription(violation, analyzerName, testName, violationTime));
        nce.setImmediateAction("Sample run held pending review");
        nce.setNameOfReporter("System (Westgard QC)");
        nce.setReportDate(new Date(System.currentTimeMillis()));
        nce.setDateOfEvent(new Date(violationTime.getTime()));
        nce.setAffectedSamplesCapReason(affected.capReason());
        resolveCategoryAndType(nce);

        nce = ncEventService.save(nce);

        linkAffectedSamples(nce, affected.links(), sysUserId);

        nceHistoryService.logActivity(nce.getId(), "AUTO_CREATED",
                "Auto-created from QC violation " + violation.getId() + " (rule " + violation.getRuleCode() + " on "
                        + analyzerName + "); " + affected.links().size() + " affected sample(s) linked; cap: "
                        + affected.capReason(),
                null, null, userId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createNceForViolation", "Created " + nce.getNceNumber()
                + " for QC violation " + violation.getId() + " (" + violation.getRuleCode() + ")");
        return nce;
    }

    /**
     * The RDT counterpart of {@link #createNceForViolation}. It keeps an Invalid
     * control line out of the statistical record — a missing control line is not a
     * Westgard rule hit, and mixing the two would corrupt violation counts and
     * sigma metrics — but the patient-safety half is identical: the same capped
     * window, the same affected-analysis links, the same NCE for the lab to work.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NcEvent createNceForFailedControl(QCResult qcResult) {
        NcEvent existing = ncEventService.findByTriggerSource(TRIGGER_SOURCE_BENCH_CONTROL, qcResult.getId());
        if (existing != null) {
            return existing;
        }

        Integer userId = qcResult.getSystemUserId() != null ? qcResult.getSystemUserId() : 1;
        String sysUserId = String.valueOf(userId);
        Timestamp failureTime = qcResult.getRunDateTime() != null ? qcResult.getRunDateTime()
                : new Timestamp(System.currentTimeMillis());
        String testName = resolveTestName(qcResult.getTestId());
        String controlName = qcResult.getControlLabel() != null ? qcResult.getControlLabel() : testName;

        AffectedSamples affected = resolveAffectedSamples(null, qcResult.getTestSectionId(), qcResult.getTestId(),
                failureTime);

        NcEvent nce = new NcEvent();
        nce.setSysUserId(sysUserId);
        nce.setNceNumber(nceNumberGeneratorService.generateNceNumber());
        nce.setSeverity(NCE_SEVERITY_CRITICAL);
        nce.setStatus(NCE_STATUS_PENDING);
        nce.setAutoGenerated(Boolean.TRUE);
        // Deliberately no westgardRule: this did not come from a statistical rule.
        nce.setTriggerSourceType(TRIGGER_SOURCE_BENCH_CONTROL);
        nce.setTriggerSourceId(qcResult.getId());
        nce.setTitle("QC failure — " + qcResult.getQualitativeOutcome() + " control on " + testName);
        nce.setDescription("Auto-created from a bench control. " + qcResult.getSource() + " control " + controlName
                + " for test " + testName + " read " + qcResult.getQualitativeOutcome() + " at " + failureTime + ".");
        nce.setImmediateAction("Repeat the test with a new kit; affected results held pending review");
        nce.setNameOfReporter("System (bench QC)");
        nce.setReportDate(new Date(System.currentTimeMillis()));
        nce.setDateOfEvent(new Date(failureTime.getTime()));
        nce.setAffectedSamplesCapReason(affected.capReason());
        resolveCategoryAndType(nce);

        nce = ncEventService.save(nce);

        linkAffectedSamples(nce, affected.links(), sysUserId);

        nceHistoryService.logActivity(nce.getId(), "AUTO_CREATED",
                "Auto-created from " + qcResult.getSource() + " control " + qcResult.getId() + " ("
                        + qcResult.getQualitativeOutcome() + "); " + affected.links().size()
                        + " affected sample(s) linked; cap: " + affected.capReason(),
                null, null, userId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createNceForFailedControl",
                "Created " + nce.getNceNumber() + " for " + qcResult.getSource() + " control " + qcResult.getId());
        return nce;
    }

    /** The control that triggered a violation, or null if unresolvable. */
    private QCResult resolveTriggeringResult(String triggeringResultId) {
        if (triggeringResultId == null) {
            return null;
        }
        try {
            return qcResultService.get(triggeringResultId);
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveTriggeringResult",
                    "Could not resolve triggering QC result " + triggeringResultId);
            return null;
        }
    }

    /**
     * Window = [last in-control QC, violation), floored at 24h and capped at the 50
     * newest DISTINCT patient samples (OGC-728). Result revisions of the same
     * sample collapse to one link. The recorded cap reason reflects what actually
     * trimmed the list: count_50 if >50 distinct samples, time_24h only if the 24h
     * floor genuinely excluded older samples, else none_applied.
     */
    private AffectedSamples resolveAffectedSamples(String instrumentId, String testSectionId, String testId,
            Timestamp failureTime) {
        // An analyzer failure scopes by instrument; a bench control has none, so it
        // scopes by lab unit instead (OGC-1147). Everything after this choice —
        // the 24h floor, the 50-sample cap, the cap-reason logic — is identical, which
        // is the point: one blast-radius rule, not one per QC source.
        boolean byAnalyzer = instrumentId != null;
        if (!byAnalyzer && testSectionId == null) {
            // Nothing to scope by. Recording the control still matters; linking patient
            // samples to it does not become guesswork.
            return new AffectedSamples(List.of(), CAP_NONE_APPLIED);
        }

        Timestamp floor = new Timestamp(failureTime.getTime() - CAP_WINDOW_MILLIS);
        Timestamp windowStart = floor;
        boolean flooredWithoutInControlQc = true;

        List<QCResult> lastAccepted = byAnalyzer
                ? qcResultService.findLatestAcceptedBefore(instrumentId, testId, failureTime)
                : qcResultService.findLatestAcceptedBenchResultBefore(testSectionId, testId, failureTime);
        if (!lastAccepted.isEmpty() && lastAccepted.get(0).getRunDateTime() != null
                && lastAccepted.get(0).getRunDateTime().after(floor)) {
            windowStart = lastAccepted.get(0).getRunDateTime();
            flooredWithoutInControlQc = false;
        }

        List<Object[]> rows = byAnalyzer
                ? analysisService.getAffectedSampleItemIdsByAnalyzerAndTestCompletedInRange(instrumentId, testId,
                        windowStart, failureTime)
                : analysisService.getAffectedSampleItemIdsByTestSectionAndTestCompletedInRange(testSectionId, testId,
                        windowStart, failureTime);
        if (rows == null) {
            rows = List.of();
        }

        // Rows are newest-first, so the first analysis seen per sample is its latest
        // revision; putIfAbsent keeps that one and dedupes the rest.
        Map<Integer, Integer> latestAnalysisBySample = new LinkedHashMap<>();
        for (Object[] row : rows) {
            latestAnalysisBySample.putIfAbsent(Integer.valueOf(String.valueOf(row[0])),
                    Integer.valueOf(String.valueOf(row[1])));
        }

        String capReason;
        if (latestAnalysisBySample.size() > CAP_MAX_SAMPLES) {
            capReason = CAP_COUNT_50;
        } else if (flooredWithoutInControlQc && (byAnalyzer
                ? analysisService.existsAnalysisCompletedBeforeByAnalyzerAndTest(instrumentId, testId, floor)
                : analysisService.existsAnalysisCompletedBeforeByTestSectionAndTest(testSectionId, testId, floor))) {
            capReason = CAP_TIME_24H;
        } else {
            capReason = CAP_NONE_APPLIED;
        }

        List<SampleLink> links = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : latestAnalysisBySample.entrySet()) {
            if (links.size() >= CAP_MAX_SAMPLES) {
                break;
            }
            links.add(new SampleLink(entry.getKey(), entry.getValue()));
        }
        return new AffectedSamples(links, capReason);
    }

    private void linkAffectedSamples(NcEvent nce, List<SampleLink> links, String sysUserId) {
        for (SampleLink link : links) {
            NceSpecimen specimen = new NceSpecimen();
            specimen.setNceId(nce.getId());
            specimen.setSampleItemId(link.sampleItemId());
            specimen.setAnalysisId(link.analysisId());
            specimen.setSysUserId(sysUserId);
            nceSpecimenService.save(specimen);
        }
    }

    private String buildDescription(QCRuleViolation violation, String analyzerName, String testName,
            Timestamp violationTime) {
        String description = "Auto-created from Westgard QC violation. Rule " + violation.getRuleCode()
                + " rejected on analyzer " + analyzerName + ", test " + testName + ", at " + violationTime + ".";
        if (violation.getResolutionNotes() != null && !violation.getResolutionNotes().isEmpty()) {
            description += " " + violation.getResolutionNotes();
        }
        return description;
    }

    /**
     * Category "Analysis" / type "QC failure" are seeded by the nce-categories /
     * nce-types CSV config handlers; ids are sequence-assigned so they must be
     * resolved by name. Both are optional context — a missing seed never blocks NCE
     * creation.
     */
    private void resolveCategoryAndType(NcEvent nce) {
        try {
            NceCategory category = nceCategoryService.getAllNceCategories().stream()
                    .filter(cat -> NCE_CATEGORY_ANALYSIS.equals(cat.getName())).findFirst().orElse(null);
            if (category == null) {
                return;
            }
            nce.setNceCategoryId(category.getId());
            nceTypeService.getNceTypesByCategoryId(category.getId()).stream()
                    .filter(type -> NCE_TYPE_QC_FAILURE.equals(type.getName())).findFirst()
                    .ifPresent(type -> nce.setNceTypeId(type.getId()));
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveCategoryAndType",
                    "Could not resolve NCE category/type: " + e.getMessage());
        }
    }

    /**
     * Read the analyzer name via projection, not a managed-entity load:
     * Analyzer.testUnitIds is a converted collection Hibernate flags dirty on load,
     * bumping the analyzer's @Version. Loading it here would make two NCEs for the
     * same analyzer (one QC run failing several analytes) collide on optimistic
     * locking. A projection touches no managed state.
     */
    private String resolveAnalyzerName(String instrumentId) {
        if (instrumentId != null) {
            try {
                List<String> names = entityManager
                        .createQuery("SELECT a.name FROM Analyzer a WHERE a.id = :id", String.class)
                        .setParameter("id", instrumentId).getResultList();
                if (!names.isEmpty() && names.get(0) != null) {
                    return names.get(0);
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveAnalyzerName",
                        "Could not resolve analyzer name for ID " + instrumentId);
            }
        }
        return "analyzer " + instrumentId;
    }

    private String resolveTestName(String testId) {
        if (testId != null) {
            try {
                Test test = testService.get(testId);
                if (test != null && test.getName() != null) {
                    return test.getName();
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "resolveTestName",
                        "Could not resolve test name for ID " + testId);
            }
        }
        return "test " + testId;
    }

    private record SampleLink(Integer sampleItemId, Integer analysisId) {
    }

    private record AffectedSamples(List<SampleLink> links, String capReason) {
    }
}
