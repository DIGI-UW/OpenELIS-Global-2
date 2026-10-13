package org.openelisglobal.dashboard.management.kpi;

import java.math.BigDecimal;
import java.util.List;

public record ManagementKpis(Kpi samples, Kpi medianTat, Kpi onTime, Kpi rejection, Kpi qcPass) {

    /** A null spark point is a period with nothing to measure. */
    public record Kpi(BigDecimal value, BigDecimal prior, List<BigDecimal> spark, Thresholds thresholds) {
    }

    /** The shape the QI tiles' rateTone reads. */
    public record Thresholds(boolean enabled, BigDecimal target, BigDecimal action, String direction) {
    }
}
