package org.openelisglobal.dashboard.management.quality;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record QualityRollup(BigDecimal composite, BigDecimal prior, List<Component> components) {

    /** 0 to 100; a null value has no data in the scope and drops out. */
    public record Component(String key, BigDecimal value, BigDecimal weight) {
    }

    static QualityRollup of(List<Component> components, BigDecimal prior) {
        BigDecimal presentWeight = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Component c : components) {
            if (c.value() != null) {
                presentWeight = presentWeight.add(c.weight());
                total = total.add(c.value().multiply(c.weight()));
            }
        }
        BigDecimal composite = presentWeight.signum() == 0 ? null
                : total.divide(presentWeight, 1, RoundingMode.HALF_UP);
        return new QualityRollup(composite, prior, components);
    }
}
