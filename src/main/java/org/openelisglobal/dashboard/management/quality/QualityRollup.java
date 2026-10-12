package org.openelisglobal.dashboard.management.quality;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.dashboard.management.ScopeRange;

public record QualityRollup(BigDecimal composite, BigDecimal prior, List<Component> components, ScopeRange scope,
        String generatedAt) {

    public QualityRollup(BigDecimal composite, BigDecimal prior, List<Component> components, ScopeRange scope) {
        this(composite, prior, components, scope, Instant.now().toString());
    }

    /**
     * One component on a 0 to 100 scale. A null value has no data in the scope and
     * drops out; effectiveWeight is its configured weight renormalised over the
     * components present.
     */
    public record Component(String key, BigDecimal value, BigDecimal weight, BigDecimal effectiveWeight) {
    }

    /** The components with effective weights filled in, and their weighted mean. */
    static QualityRollup of(List<Component> components, BigDecimal prior, ScopeRange scope) {
        BigDecimal presentWeight = BigDecimal.ZERO;
        for (Component c : components) {
            if (c.value() != null) {
                presentWeight = presentWeight.add(c.weight());
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        List<Component> weighted = new ArrayList<>();
        for (Component c : components) {
            BigDecimal effective = c.value() == null || presentWeight.signum() == 0 ? null
                    : c.weight().divide(presentWeight, 4, RoundingMode.HALF_UP);
            weighted.add(new Component(c.key(), c.value(), c.weight(), effective));
            if (effective != null) {
                total = total.add(c.value().multiply(c.weight()));
            }
        }
        BigDecimal composite = presentWeight.signum() == 0 ? null
                : total.divide(presentWeight, 1, RoundingMode.HALF_UP);
        return new QualityRollup(composite, prior, weighted, scope);
    }
}
