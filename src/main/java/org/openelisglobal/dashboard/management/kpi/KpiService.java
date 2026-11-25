package org.openelisglobal.dashboard.management.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.openelisglobal.dashboard.management.ScopeRange;

public interface KpiService {

    ManagementKpis getKpis(ScopeRange scope);

    /**
     * On-time % by the KPI strip's rule, keyed like the TAT report's LAB_UNIT
     * breakdown. Empty when the QI TAT indicator is disabled.
     */
    Map<String, BigDecimal> onTimeByLabUnit(ScopeRange scope);

    /** The QC pass tile's rate between two dates, or null when no QC ran. */
    BigDecimal qcPassRate(LocalDate from, LocalDate to);
}
