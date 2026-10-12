package org.openelisglobal.dashboard.management.quality;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

public record WeightsView(BigDecimal qc, BigDecimal eqa, BigDecimal nceInverse, BigDecimal amendmentInverse) {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    /** Every weight present and between 0 and 1, summing to 1.00 within 0.01. */
    @JsonIgnore
    public boolean isValid() {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal weight : new BigDecimal[] { qc, eqa, nceInverse, amendmentInverse }) {
            if (weight == null || weight.signum() < 0 || weight.compareTo(BigDecimal.ONE) > 0) {
                return false;
            }
            sum = sum.add(weight);
        }
        return sum.subtract(BigDecimal.ONE).abs().compareTo(TOLERANCE) <= 0;
    }
}
