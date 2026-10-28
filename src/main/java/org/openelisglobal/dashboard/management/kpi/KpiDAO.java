package org.openelisglobal.dashboard.management.kpi;

import java.math.BigDecimal;
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

    /** Key of the whole range in {@link #tatByPeriod}. */
    String ALL_PERIODS = "";

    /**
     * Receipt-to-validation TAT of the analyses received in the range, per period
     * key and under {@link #ALL_PERIODS}.
     */
    Map<String, TatFigures> tatByPeriod(LocalDate from, LocalDate to, String interval, BigDecimal targetHours);

    /**
     * {validated, on time} per test section id (null when the analysis has none);
     * sections with nothing validated are left out.
     */
    Map<String, long[]> countOnTimeBySection(LocalDate from, LocalDate to, BigDecimal targetHours);

    record TatFigures(BigDecimal medianHours, long validated, long onTime) {
    }
}
