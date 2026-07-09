package org.openelisglobal.qa.service;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.qa.dto.QaOverviewSummary;
import org.openelisglobal.qa.dto.QaOverviewSummary.ActivityItem;
import org.openelisglobal.qc.dto.QCDashboardSummary;
import org.openelisglobal.qc.service.QCDashboardService;
import org.openelisglobal.qc.service.QCRuleViolationService;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compiles the QA Overview aggregates (OGC-694 WS-F) from existing QC, EQA,
 * audit-trail, and e-signature services within one read-only transaction
 * (Constitution IV.5).
 */
@Service
public class QaOverviewServiceImpl implements QaOverviewService {

    private static final int EQA_DUE_SOON_DAYS = 14;
    private static final int ACTIVITY_CAP = 10;

    @Autowired
    private QCDashboardService qcDashboardService;

    @Autowired
    private QCRuleViolationService qcRuleViolationService;

    @Autowired
    private SampleEQAService sampleEQAService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ElectronicSignatureService electronicSignatureService;

    @Autowired
    private AnalyzerService analyzerService;

    @Override
    @Transactional(readOnly = true)
    public QaOverviewSummary getSummary() {
        Instant now = Instant.now();
        Instant dayAgo = now.minus(24, ChronoUnit.HOURS);
        LocalDate weekStartDate = LocalDate.now().with(DayOfWeek.MONDAY);
        Instant weekStart = weekStartDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        Timestamp nowTs = Timestamp.from(now);
        Timestamp dayAgoTs = Timestamp.from(dayAgo);
        Timestamp weekStartTs = Timestamp.from(weekStart);

        QaOverviewSummary summary = new QaOverviewSummary();
        summary.week.weekStart = weekStartDate.toString();
        summary.week.weekStartInstant = weekStart.toString();

        // One scan covers both windows: the 24h range can reach before the week
        // start early in the week, and the week range always contains "recent".
        Timestamp from = weekStartTs.before(dayAgoTs) ? weekStartTs : dayAgoTs;
        List<QCRuleViolation> violations = loadViolations(from, nowTs);

        compileQc(summary, violations, weekStartTs, dayAgoTs);
        compileEqa(summary, nowTs);
        compileWeekCounters(summary, weekStartTs, nowTs);
        compileActivity(summary, violations, dayAgoTs, nowTs);

        return summary;
    }

    private List<QCRuleViolation> loadViolations(Timestamp from, Timestamp to) {
        try {
            return qcRuleViolationService.findByDateRange(from, to);
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getName(), "loadViolations", "QC violations unavailable: " + e.getMessage());
            return List.of();
        }
    }

    private void compileQc(QaOverviewSummary summary, List<QCRuleViolation> violations, Timestamp weekStart,
            Timestamp dayAgo) {
        try {
            QCDashboardSummary qcSummary = qcDashboardService.getDashboardSummary();
            summary.qc.compliantInstruments = qcSummary.getCompliantInstruments();
            summary.qc.warningInstruments = qcSummary.getWarningInstruments();
            summary.qc.nonCompliantInstruments = qcSummary.getNonCompliantInstruments();
            summary.qc.totalInstruments = qcSummary.getTotalInstruments();
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getName(), "compileQc", "QC dashboard summary unavailable: " + e.getMessage());
        }
        summary.qc.violations24h = violations.stream()
                .filter(v -> v.getViolationDateTime() != null && !v.getViolationDateTime().before(dayAgo)).count();
        List<QCRuleViolation> weekViolations = violations.stream()
                .filter(v -> v.getViolationDateTime() != null && !v.getViolationDateTime().before(weekStart))
                .collect(Collectors.toList());
        summary.qc.violationsThisWeek = weekViolations.size();
        weekViolations.stream().collect(Collectors.groupingBy(QCRuleViolation::getRuleCode, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> summary.qc.weekRuleBreakdown.put(e.getKey(), e.getValue()));
    }

    private void compileEqa(QaOverviewSummary summary, Timestamp now) {
        try {
            List<SampleEQA> samples = sampleEQAService.findEqaSamples();
            Timestamp soonCutoff = Timestamp.from(now.toInstant().plus(EQA_DUE_SOON_DAYS, ChronoUnit.DAYS));
            summary.eqa.open = samples.size();
            summary.eqa.overdue = samples.stream()
                    .filter(s -> s.getEqaDeadline() != null && s.getEqaDeadline().before(now)).count();
            summary.eqa.dueSoon14d = samples.stream().filter(s -> s.getEqaDeadline() != null
                    && !s.getEqaDeadline().before(now) && !s.getEqaDeadline().after(soonCutoff)).count();
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getName(), "compileEqa", "EQA orders unavailable: " + e.getMessage());
        }
    }

    private void compileWeekCounters(QaOverviewSummary summary, Timestamp weekStart, Timestamp now) {
        try {
            summary.week.auditEntries = historyService.getSystemEventHistoryCount(weekStart, now, null, null, null,
                    null, null);
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getName(), "compileWeekCounters",
                    "Audit history count unavailable: " + e.getMessage());
        }
        try {
            summary.week.signatureEvents = electronicSignatureService.countSignaturesInDateRange(weekStart, now);
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getName(), "compileWeekCounters",
                    "Signature count unavailable: " + e.getMessage());
        }
    }

    private void compileActivity(QaOverviewSummary summary, List<QCRuleViolation> violations, Timestamp dayAgo,
            Timestamp now) {
        List<ActivityItem> items = new ArrayList<>();
        try {
            electronicSignatureService.getSignaturesInDateRange(dayAgo, now).stream().limit(ACTIVITY_CAP)
                    .forEach(sig -> items.add(toActivityItem(sig)));
        } catch (RuntimeException e) {
            LogEvent.logWarn(getClass().getName(), "compileActivity", "Signatures unavailable: " + e.getMessage());
        }
        Map<String, String> instrumentNames = new HashMap<>();
        violations.stream().filter(v -> v.getViolationDateTime() != null && !v.getViolationDateTime().before(dayAgo))
                .limit(ACTIVITY_CAP).forEach(v -> items.add(toActivityItem(v, instrumentNames)));
        // Instant.toString() emits variable fractional precision, so lexicographic
        // order is not chronological — parse for the newest-first cut.
        items.sort(Comparator
                .comparing((ActivityItem item) -> item.timestamp == null ? Instant.MIN : Instant.parse(item.timestamp))
                .reversed());
        summary.activity = items.stream().limit(ACTIVITY_CAP).collect(Collectors.toList());
    }

    private ActivityItem toActivityItem(ElectronicSignature sig) {
        ActivityItem item = new ActivityItem();
        item.type = ActivityItem.TYPE_ESIG;
        item.timestamp = sig.getSignedAt() != null ? sig.getSignedAt().toInstant().toString() : null;
        item.actor = sig.getSignerNamePrinted();
        item.meaning = sig.getSignatureMeaning() != null ? sig.getSignatureMeaning().name() : null;
        item.recordType = sig.getRecordType();
        item.recordId = sig.getRecordId();
        return item;
    }

    private ActivityItem toActivityItem(QCRuleViolation violation, Map<String, String> instrumentNames) {
        ActivityItem item = new ActivityItem();
        item.type = ActivityItem.TYPE_QC_VIOLATION;
        item.timestamp = violation.getViolationDateTime() != null
                ? violation.getViolationDateTime().toInstant().toString()
                : null;
        item.ruleCode = violation.getRuleCode();
        item.severity = violation.getSeverity();
        item.instrumentName = instrumentNames.computeIfAbsent(violation.getInstrumentId(), this::lookupInstrumentName);
        return item;
    }

    private String lookupInstrumentName(String instrumentId) {
        try {
            return analyzerService.getWithType(instrumentId).map(Analyzer::getName)
                    .orElse("Instrument " + instrumentId);
        } catch (RuntimeException e) {
            return "Instrument " + instrumentId;
        }
    }
}
