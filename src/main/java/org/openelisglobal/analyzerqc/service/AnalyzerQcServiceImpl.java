package org.openelisglobal.analyzerqc.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.QcFrequencyType;
import org.openelisglobal.analyzer.valueholder.QcStatus;
import org.openelisglobal.analyzerqc.dao.AnalyzerQcRunDAO;
import org.openelisglobal.analyzerqc.form.QcRunForm;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcRun;
import org.openelisglobal.analyzerqc.valueholder.AnalyzerQcStatus;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core QC status evaluation and run recording.
 *
 * QC status rules (BR-AQC-001 / BR-AQC-002):
 *   qcRequired = false  → always PASS (informational, BR-AQC-004)
 *   No runs on record   → NOT_RUN
 *   DAILY               → last PASS must be from today (same calendar day)
 *   PER_SHIFT           → last PASS within qcFrequencyHours (default 8 h)
 *   CUSTOM_HOURS        → last PASS within qcFrequencyHours
 */
@Service
@Transactional
public class AnalyzerQcServiceImpl implements AnalyzerQcService {

    // Use AnalyzerService (not AnalyzerDAO) — project convention:
    // services inject other services, same as AnalyzerQueryServiceImpl.
    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerQcRunDAO analyzerQcRunDAO;

    // ── getQcStatus ──────────────────────────────────────────────────────────

    @Override
    public AnalyzerQcStatus getQcStatus(String analyzerId) {
        Analyzer analyzer = requireAnalyzer(analyzerId);

        Optional<AnalyzerQcRun> lastPass = analyzerQcRunDAO.getLastPassForAnalyzer(analyzerId);
        Optional<AnalyzerQcRun> lastRun  = analyzerQcRunDAO.getLastRunForAnalyzer(analyzerId);

        AnalyzerQcStatus status = new AnalyzerQcStatus();
        status.setAnalyzerId(analyzerId);
        status.setQcRequired(analyzer.isQcRequired());
        status.setStatus(evaluateStatus(analyzer, lastPass));
        status.setNextQcDue(calculateNextDue(analyzer, lastPass));

        lastRun.ifPresent(r -> {
            status.setLastRunDate(r.getRunDate());
            status.setLastRunResult(r.getResult());
            status.setLastRunValue(r.getValue());
            status.setLastRunSource(r.getSource());
        });

        return status;
    }

    // ── recordQcRun ──────────────────────────────────────────────────────────

    @Override
    public void recordQcRun(String analyzerId, QcRunForm form, String currentUserId) {
        Analyzer analyzer = requireAnalyzer(analyzerId);

        // Resolve run date — default to now if not supplied
        Timestamp runDate = (form.getRunDate() != null)
                ? form.getRunDate()
                : new Timestamp(System.currentTimeMillis());

        // BR-AQC-006: run date cannot be in the future
        if (runDate.after(new Timestamp(System.currentTimeMillis()))) {
            throw new IllegalArgumentException("error.analyzerQc.qcDateFuture");
        }

        AnalyzerQcRun run = new AnalyzerQcRun();
        run.setAnalyzer(analyzer);
        run.setResult(form.getResult());
        run.setValue(form.getValue());
        run.setRunDate(runDate);
        run.setSource(form.getSource());

        // Use caller-specified user or fall back to the authenticated session user
        String userId = (form.getPerformedByUserId() != null
                && !form.getPerformedByUserId().isBlank())
                ? form.getPerformedByUserId()
                : currentUserId;
        run.setSysUserId(userId);
        analyzerQcRunDAO.insert(run);

        LogEvent.logInfo(getClass().getName(), "recordQcRun",
                "QC run recorded for analyzer=" + analyzerId
                        + " result=" + form.getResult()
                        + " source=" + form.getSource());
    }

    // ── getQcHistory ─────────────────────────────────────────────────────────

    @Override
    public List<AnalyzerQcRun> getQcHistory(String analyzerId) {
        requireAnalyzer(analyzerId);
        return analyzerQcRunDAO.getAllRunsForAnalyzer(analyzerId);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Analyzer requireAnalyzer(String analyzerId) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
        }
        return analyzer;
    }

    /**
     * Derives current QC validity status from the analyzer's frequency rule
     * and the timestamp of the most recent PASS run.
     */
    private QcStatus evaluateStatus(Analyzer analyzer, Optional<AnalyzerQcRun> lastPass) {
        // BR-AQC-004: informational mode — QC is tracked but never blocks
        if (!analyzer.isQcRequired()) {
            return QcStatus.PASS;
        }
        if (lastPass.isEmpty()) {
            return QcStatus.NOT_RUN;
        }
        if (analyzer.getQcFrequencyType() == null) {
            // Frequency rule not yet configured — treat as not run
            return QcStatus.NOT_RUN;
        }

        Instant lastPassTime = lastPass.get().getRunDate().toInstant();

        return switch (analyzer.getQcFrequencyType()) {
            case DAILY -> {
                LocalDate lastPassDay = lastPassTime
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                yield lastPassDay.equals(LocalDate.now(ZoneId.systemDefault()))
                        ? QcStatus.PASS : QcStatus.OVERDUE;
            }
            case PER_SHIFT -> {
                // Default shift = 8 h; overrideable via qcFrequencyHours
                int shiftHours = (analyzer.getQcFrequencyHours() != null)
                        ? analyzer.getQcFrequencyHours() : 8;

                Duration elapsed = Duration.between(lastPassTime, Instant.now());
                yield elapsed.compareTo(Duration.ofHours(shiftHours)) <= 0
                        ? QcStatus.PASS : QcStatus.OVERDUE;
            }
            case CUSTOM_HOURS -> {
                if (analyzer.getQcFrequencyHours() == null) {
                    yield QcStatus.NOT_RUN;
                }

                Duration elapsed = Duration.between(lastPassTime, Instant.now());
                yield elapsed.compareTo(Duration.ofHours(analyzer.getQcFrequencyHours())) <= 0
                        ? QcStatus.PASS : QcStatus.OVERDUE;
            }
        };
    }

    /**
     * Calculates when the next QC run is due based on the frequency rule.
     * Returns null if frequency is not configured or no PASS run on record.
     */
    private Timestamp calculateNextDue(Analyzer analyzer, Optional<AnalyzerQcRun> lastPass) {
        if (lastPass.isEmpty() || analyzer.getQcFrequencyType() == null) {
            return null;
        }
        Instant base = lastPass.get().getRunDate().toInstant();
        Instant next = switch (analyzer.getQcFrequencyType()) {
            case DAILY -> base.atZone(ZoneId.systemDefault())
                    .toLocalDate().plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant();
            case PER_SHIFT -> {
                int hours = (analyzer.getQcFrequencyHours() != null)
                        ? analyzer.getQcFrequencyHours() : 8;
                yield base.plus(hours, ChronoUnit.HOURS);
            }
            case CUSTOM_HOURS -> base.plus(
                    analyzer.getQcFrequencyHours(), ChronoUnit.HOURS);
        };
        return Timestamp.from(next);
    }
}