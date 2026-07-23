package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.dto.AnalyteDetail;
import org.openelisglobal.qc.dto.InstrumentQCStatus;
import org.openelisglobal.qc.dto.QCDashboardSummary;
import org.openelisglobal.qc.dto.TriggeredRuleDetail;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QC Dashboard (T120).
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers) Compiles all data within transaction to prevent
 * LazyInitializationException.
 */
@Service
public class QCDashboardServiceImpl implements QCDashboardService {

    private static final String COLOR_GREEN = "GREEN";
    private static final String COLOR_YELLOW = "YELLOW";
    private static final String COLOR_RED = "RED";

    private static final String SEVERITY_REJECTION = "REJECTION";
    private static final String SEVERITY_WARNING = "WARNING";

    private static final String STATUS_UNRESOLVED = "UNRESOLVED";

    private static final int DEFAULT_WINDOW_DAYS = 30;

    @Autowired
    private QCRuleViolationDAO violationDAO;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private QCControlLotDAO controlLotDAO;

    @Autowired
    private TestService testService;

    private Timestamp[] defaultDateRange() {
        Instant now = Instant.now();
        Timestamp endDate = Timestamp.from(now);
        Timestamp startDate = Timestamp.from(now.minus(DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS));
        return new Timestamp[] { startDate, endDate };
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentQCStatus> getAllInstrumentComplianceStatus() {
        Timestamp[] range = defaultDateRange();
        return getAllInstrumentComplianceStatus(range[0], range[1]);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentQCStatus> getAllInstrumentComplianceStatus(Timestamp startDate, Timestamp endDate) {
        // Collect instrument IDs from both QC results and unresolved violations
        Set<String> instrumentIds = new HashSet<>();
        try {
            List<String> resultInstrumentIds = resultDAO.findDistinctInstrumentIds();
            instrumentIds.addAll(resultInstrumentIds);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "getAllInstrumentComplianceStatus",
                    "Could not load instrument IDs from QC results: " + e.getMessage());
        }
        try {
            List<QCRuleViolation> allUnresolved = violationDAO.findUnresolved();
            allUnresolved.stream().map(QCRuleViolation::getInstrumentId).forEach(instrumentIds::add);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "getAllInstrumentComplianceStatus",
                    "Could not load instrument IDs from violations: " + e.getMessage());
        }

        // Pre-fetch analyzers to avoid N+1
        Map<String, Analyzer> analyzerCache = new HashMap<>();
        for (String id : instrumentIds) {
            try {
                Optional<Analyzer> analyzer = analyzerService.getWithType(id);
                analyzer.ifPresent(a -> analyzerCache.put(id, a));
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "getAllInstrumentComplianceStatus",
                        "Could not load analyzer " + id + ": " + e.getMessage());
            }
        }

        List<InstrumentQCStatus> statuses = new ArrayList<>();

        for (String instrumentId : instrumentIds) {
            InstrumentQCStatus status = buildInstrumentStatus(instrumentId, startDate, endDate,
                    analyzerCache.get(instrumentId));
            statuses.add(status);
        }

        // Remove instruments that have no activity in the date range
        statuses.removeIf(s -> s.getAnalyteDetails().isEmpty() && s.getUnresolvedRejections() == 0
                && s.getUnresolvedWarnings() == 0);

        // Sort by compliance color (RED first, then YELLOW, then GREEN)
        statuses.sort((a, b) -> {
            int colorOrder = getColorOrder(a.getComplianceColor()) - getColorOrder(b.getComplianceColor());
            if (colorOrder != 0) {
                return colorOrder;
            }
            return a.getInstrumentId().compareTo(b.getInstrumentId());
        });

        return statuses;
    }

    @Override
    @Transactional(readOnly = true)
    public InstrumentQCStatus getInstrumentComplianceStatus(String instrumentId) {
        Timestamp[] range = defaultDateRange();
        return getInstrumentComplianceStatus(instrumentId, range[0], range[1]);
    }

    @Override
    @Transactional(readOnly = true)
    public InstrumentQCStatus getInstrumentComplianceStatus(String instrumentId, Timestamp startDate,
            Timestamp endDate) {
        Analyzer analyzer = null;
        try {
            Optional<Analyzer> opt = analyzerService.getWithType(String.valueOf(instrumentId));
            analyzer = opt.orElse(null);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "getInstrumentComplianceStatus",
                    "Could not load analyzer " + instrumentId + ": " + e.getMessage());
        }

        return buildInstrumentStatus(instrumentId, startDate, endDate, analyzer);
    }

    @Override
    @Transactional(readOnly = true)
    public QCDashboardSummary getDashboardSummary() {
        Timestamp[] range = defaultDateRange();
        return getDashboardSummary(range[0], range[1]);
    }

    @Override
    @Transactional(readOnly = true)
    public QCDashboardSummary getDashboardSummary(Timestamp startDate, Timestamp endDate) {
        List<InstrumentQCStatus> allStatuses = getAllInstrumentComplianceStatus(startDate, endDate);

        QCDashboardSummary summary = new QCDashboardSummary();
        summary.setTotalInstruments(allStatuses.size());

        int compliant = 0;
        int warning = 0;
        int nonCompliant = 0;
        int totalRejections = 0;
        int totalWarnings = 0;

        for (InstrumentQCStatus status : allStatuses) {
            switch (status.getComplianceColor()) {
            case COLOR_GREEN:
                compliant++;
                break;
            case COLOR_YELLOW:
                warning++;
                break;
            case COLOR_RED:
                nonCompliant++;
                break;
            }
            totalRejections += status.getUnresolvedRejections();
            totalWarnings += status.getUnresolvedWarnings();
        }

        summary.setCompliantInstruments(compliant);
        summary.setWarningInstruments(warning);
        summary.setNonCompliantInstruments(nonCompliant);
        summary.setTotalRejections(totalRejections);
        summary.setTotalWarnings(totalWarnings);
        summary.setTotalUnresolvedViolations(totalRejections + totalWarnings);
        summary.setLastUpdateTime(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

        return summary;
    }

    /**
     * Build the compliance status for a specific instrument within a date range.
     * Loads violations and results for the instrument within the window.
     */
    private InstrumentQCStatus buildInstrumentStatus(String instrumentId, Timestamp startDate, Timestamp endDate,
            Analyzer analyzer) {

        // Load ALL unresolved violations (not date-scoped) — compliance state is
        // always the full picture so summary tiles reflect true unresolved state
        List<QCRuleViolation> instrumentViolations = List.of();
        try {
            instrumentViolations = violationDAO.findUnresolvedByInstrument(instrumentId);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "buildInstrumentStatus",
                    "Could not load violations for instrument " + instrumentId + ": " + e.getMessage());
        }

        // Load QC results in date range
        List<QCResult> resultsInRange = List.of();
        try {
            resultsInRange = resultDAO.findByInstrumentAndDateRange(instrumentId, startDate, endDate);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "buildInstrumentStatus",
                    "Could not load QC results for instrument " + instrumentId + ": " + e.getMessage());
        }

        InstrumentQCStatus status = new InstrumentQCStatus();
        status.setInstrumentId(instrumentId);

        // Populate instrument metadata from Analyzer
        if (analyzer != null) {
            status.setInstrumentName(analyzer.getName());
            status.setInstrumentLocation(analyzer.getLocation());
            if (analyzer.getAnalyzerType() != null) {
                status.setInstrumentType(analyzer.getAnalyzerType().getName());
            } else {
                status.setInstrumentType(analyzer.getType());
            }
        } else {
            status.setInstrumentName("Instrument " + instrumentId);
        }

        // Count violations by severity and build rule details
        int rejections = 0;
        int warnings = 0;
        Set<String> triggeredRules = new HashSet<>();
        Map<String, String> ruleSeverityMap = new HashMap<>();
        String lastViolationTime = null;

        for (QCRuleViolation violation : instrumentViolations) {
            if (SEVERITY_REJECTION.equals(violation.getSeverity())) {
                rejections++;
            } else if (SEVERITY_WARNING.equals(violation.getSeverity())) {
                warnings++;
            }
            triggeredRules.add(violation.getRuleCode());

            String existing = ruleSeverityMap.get(violation.getRuleCode());
            if (existing == null || SEVERITY_REJECTION.equals(violation.getSeverity())) {
                ruleSeverityMap.put(violation.getRuleCode(), violation.getSeverity());
            }

            if (violation.getViolationDateTime() != null) {
                String violationTime = violation.getViolationDateTime().toInstant().toString();
                if (lastViolationTime == null || violationTime.compareTo(lastViolationTime) > 0) {
                    lastViolationTime = violationTime;
                }
            }
        }

        status.setUnresolvedRejections(rejections);
        status.setUnresolvedWarnings(warnings);
        status.setTriggeredRules(new ArrayList<>(triggeredRules));
        status.setLastViolationTime(lastViolationTime);

        List<TriggeredRuleDetail> ruleDetails = new ArrayList<>();
        for (Map.Entry<String, String> entry : ruleSeverityMap.entrySet()) {
            ruleDetails.add(new TriggeredRuleDetail(entry.getKey(), entry.getValue()));
        }
        status.setTriggeredRuleDetails(ruleDetails);

        // Derive test IDs from QC results (not violations) — fixes analyteDetails for
        // instruments with results but no violations
        Set<String> testIds = resultsInRange.stream().map(QCResult::getTestId).collect(Collectors.toSet());

        // Group results by test ID to find latest per test
        Map<String, QCResult> latestByTest = new HashMap<>();
        for (QCResult result : resultsInRange) {
            QCResult current = latestByTest.get(result.getTestId());
            if (current == null || (result.getRunDateTime() != null && current.getRunDateTime() != null
                    && result.getRunDateTime().after(current.getRunDateTime()))) {
                latestByTest.put(result.getTestId(), result);
            }
        }

        List<AnalyteDetail> analyteDetails = new ArrayList<>();
        String lastResultTime = null;
        for (String testId : testIds) {
            AnalyteDetail detail = buildAnalyteDetail(testId, latestByTest.get(testId));
            if (detail != null) {
                analyteDetails.add(detail);
                if (detail.getLastRunTime() != null) {
                    if (lastResultTime == null || detail.getLastRunTime().compareTo(lastResultTime) > 0) {
                        lastResultTime = detail.getLastRunTime();
                    }
                }
            }
        }
        status.setAnalyteDetails(analyteDetails);
        status.setLastResultTime(lastResultTime);

        // Count active control lots for this instrument
        try {
            long activeCount = controlLotDAO.countActiveByInstrument(instrumentId);
            status.setActiveControlLots((int) activeCount);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "buildInstrumentStatus",
                    "Could not count active control lots for instrument " + instrumentId + ": " + e.getMessage());
        }

        status.setComplianceColor(calculateComplianceColor(rejections, warnings));

        return status;
    }

    /**
     * Build analyte detail from a test ID and the latest QC result for that test.
     */
    private AnalyteDetail buildAnalyteDetail(String testId, QCResult latestResult) {
        AnalyteDetail detail = new AnalyteDetail();
        detail.setTestId(testId);

        try {
            Test test = testService.getTestById(String.valueOf(testId));
            if (test != null) {
                detail.setTestName(test.getDescription() != null ? test.getDescription() : "Test " + testId);
            } else {
                detail.setTestName("Test " + testId);
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getName(), "buildAnalyteDetail",
                    "Could not load test " + testId + ": " + e.getMessage());
            detail.setTestName("Test " + testId);
        }

        if (latestResult != null) {
            detail.setLatestZScore(latestResult.getZScore());
            if (latestResult.getRunDateTime() != null) {
                detail.setLastRunTime(latestResult.getRunDateTime().toInstant().toString());
            }
        }

        return detail;
    }

    /**
     * Calculate compliance color based on violation counts. RED: Any unresolved
     * REJECTION violations YELLOW: Only WARNING violations (no rejections) GREEN:
     * No unresolved violations
     */
    private String calculateComplianceColor(int rejections, int warnings) {
        if (rejections > 0) {
            return COLOR_RED;
        } else if (warnings > 0) {
            return COLOR_YELLOW;
        } else {
            return COLOR_GREEN;
        }
    }

    /**
     * Get sort order for compliance colors (lower = higher priority).
     */
    private int getColorOrder(String color) {
        switch (color) {
        case COLOR_RED:
            return 0;
        case COLOR_YELLOW:
            return 1;
        case COLOR_GREEN:
            return 2;
        default:
            return 3;
        }
    }
}