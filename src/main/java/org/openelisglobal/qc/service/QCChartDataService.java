package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Service interface for QC Chart Data retrieval. Supports User Story 2: Monitor
 * QC Data with Control Charts.
 */
public interface QCChartDataService {

    /**
     * Get QC results for a control lot within an optional date range.
     *
     * @param controlLotId The control lot ID
     * @param startDate    Optional start date (inclusive), may be null
     * @param endDate      Optional end date (inclusive), may be null
     * @return List of matching QC results
     */
    List<QCResult> getResultsByControlLotAndDateRange(String controlLotId, Timestamp startDate, Timestamp endDate);

    /**
     * Get all rule violations associated with the given result IDs.
     *
     * @param resultIds List of QC result IDs
     * @return List of matching violations
     */
    List<QCRuleViolation> getViolationsForResults(List<String> resultIds);

    /**
     * Get the latest statistics for a control lot (mean, SD for reference lines).
     *
     * @param controlLotId The control lot ID
     * @return Latest statistics, or null if none exist
     */
    QCStatistics getLatestStatistics(String controlLotId);

    /**
     * Latest statistics for a control lot paired with its Westgard sigma metric
     * (the OGC-704 lot -> TEa -> {@link SigmaMetrics#compute} wiring). Extracted so
     * the chart statistics endpoint and the inspector export (OGC-706) share one
     * sigma code path rather than duplicating it.
     *
     * @param controlLotId The control lot ID
     * @return statistics + derived sigma, or null when the lot has no statistics
     *         yet
     */
    StatsWithSigma getStatisticsWithSigma(String controlLotId);

    /**
     * Assemble the inspector-export model (OGC-706) for one instrument over a date
     * window, optionally narrowed to a single test and/or control level. Runs,
     * per-result violations, latest statistics, sigma and display names are all
     * materialized here inside the read transaction so the controller can render
     * CSV/PDF without touching lazy associations.
     *
     * @param instrumentId Required instrument ID (report scope)
     * @param testId       Optional test ID filter (blank/null = all tests)
     * @param controlLevel Optional control level filter (blank/null = all levels)
     * @param start        Window start (inclusive)
     * @param end          Window end (inclusive)
     * @param maxRows      Cap on total run rows across lots; when exceeded the
     *                     model is marked truncated so the CSV can surface a notice
     * @return The assembled export model (lots with no runs in the window are
     *         omitted)
     */
    QCExportModel getExportModel(String instrumentId, String testId, String controlLevel, Timestamp start,
            Timestamp end, int maxRows);

    /** Latest control statistics paired with the derived sigma metric. */
    record StatsWithSigma(QCStatistics statistics, SigmaMetrics.SigmaResult sigma) {
    }

    /**
     * One control lot's slice of the export: metadata, runs, violations, stats,
     * sigma.
     */
    record LotSection(QCControlLot lot, String testName, List<QCResult> results, List<QCRuleViolation> violations,
            QCStatistics statistics, SigmaMetrics.SigmaResult sigma) {
    }

    /** The full inspector-export model for one instrument over a window. */
    record QCExportModel(String instrumentName, List<LotSection> sections, int totalRuns, int totalViolations,
            boolean truncated) {
    }
}
