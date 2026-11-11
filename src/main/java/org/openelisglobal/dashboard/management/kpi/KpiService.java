package org.openelisglobal.dashboard.management.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.openelisglobal.dashboard.management.ScopeRange;

public interface KpiService {

    ManagementKpis getKpis(ScopeRange scope);

    /**
     * Per test section id: the TAT report's LAB_UNIT median and 90th percentile,
     * and on-time % (null when the QI TAT indicator is disabled).
     */
    Map<String, SectionTat> tatBySection(ScopeRange scope);

    /** The QC pass tile's rate between two dates, or null when no QC ran. */
    BigDecimal qcPassRate(LocalDate from, LocalDate to);

    record SectionTat(BigDecimal medianHours, BigDecimal p90Hours, BigDecimal onTimePercent) {
    }
}
