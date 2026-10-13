package org.openelisglobal.dashboard.management.kpi;

import java.time.LocalDate;
import java.util.Map;

public interface KpiDAO {

    /** interval: DAILY, WEEKLY or MONTHLY. */
    Map<String, Long> countSamplesReceivedByPeriod(LocalDate from, LocalDate to, String interval);

    /**
     * QC runs per period key as {total, failed}. A run fails on a FAIL or INVALID
     * outcome or a Westgard REJECTION violation.
     */
    Map<String, long[]> countQcRunsByPeriod(LocalDate from, LocalDate to, String interval);
}
