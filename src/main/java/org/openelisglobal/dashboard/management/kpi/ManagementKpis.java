package org.openelisglobal.dashboard.management.kpi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.openelisglobal.dashboard.management.ScopeRange;

/**
 * Each spark is aligned to {@code periods}; a null point is a period with
 * nothing to measure.
 */
public record ManagementKpis(List<String> periods, Kpi samples, Kpi medianTat, Kpi onTime, Kpi rejection, Kpi qcPass,
        ScopeRange scope, String generatedAt) {

    public ManagementKpis(List<String> periods, Kpi samples, Kpi medianTat, Kpi onTime, Kpi rejection, Kpi qcPass,
            ScopeRange scope) {
        this(periods, samples, medianTat, onTime, rejection, qcPass, scope, Instant.now().toString());
    }

    public record Kpi(BigDecimal value, BigDecimal prior, List<BigDecimal> spark, Thresholds thresholds) {
    }

    /** The shape the QI tiles' rateTone reads. */
    public record Thresholds(boolean enabled, BigDecimal target, BigDecimal action, String direction) {
    }
}
