package org.openelisglobal.dashboard.management.quality;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record WeightsView(BigDecimal qc, BigDecimal eqa, BigDecimal nceInverse, BigDecimal amendmentInverse) {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    // Rounded as NUMERIC(3,2) stores them, so the sum check sees the stored values.
    public WeightsView {
        qc = scaled(qc);
        eqa = scaled(eqa);
        nceInverse = scaled(nceInverse);
        amendmentInverse = scaled(amendmentInverse);
    }

    private static BigDecimal scaled(BigDecimal weight) {
        return weight == null ? null : weight.setScale(2, RoundingMode.HALF_UP);
    }

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
