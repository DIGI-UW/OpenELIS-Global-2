package org.openelisglobal.dashboard.management.kpi;

import java.time.LocalDate;
import java.util.Map;

public interface KpiDAO {

    /**
     * Samples received per period key of the given interval (DAILY, WEEKLY,
     * MONTHLY).
     */
    Map<String, Long> countSamplesReceivedByPeriod(LocalDate from, LocalDate to, String interval);

    /**
     * QC runs per period key as {total, failed}. A run fails on a FAIL or INVALID
     * outcome or a Westgard REJECTION violation.
     */
    Map<String, long[]> countQcRunsByPeriod(LocalDate from, LocalDate to, String interval);
}
