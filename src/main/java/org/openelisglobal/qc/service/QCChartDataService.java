package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<QCResult> getResultsByControlLotAndDateRange(String controlLotId, Timestamp startDate, Timestamp endDate);

    /**
     * Get all rule violations associated with the given result IDs.
     *
     * @param resultIds List of QC result IDs
     * @return List of matching violations
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<QCRuleViolation> getViolationsForResults(List<String> resultIds);

    /**
     * Get the latest statistics for a control lot (mean, SD for reference lines).
     *
     * @param controlLotId The control lot ID
     * @return Latest statistics, or null if none exist
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    QCStatistics getLatestStatistics(String controlLotId);
}
