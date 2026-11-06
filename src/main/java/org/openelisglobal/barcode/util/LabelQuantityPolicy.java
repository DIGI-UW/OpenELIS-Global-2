package org.openelisglobal.barcode.util;

import java.util.Map;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.labelpreset.service.LabelQuantityRefusedException;

/**
 * Shared safety bound on a label quantity, independent of business overrides.
 */
public final class LabelQuantityPolicy {
    public static final int DEFAULT_MAXIMUM = 100;
    public static final int DEFAULT_MAXIMUM_TOTAL = 1000;

    private LabelQuantityPolicy() {
    }

    public static int maximum() {
        int configured = BarcodeConfigUtil.parseIntSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_REQUEST_LABEL_QUANTITY),
                DEFAULT_MAXIMUM);
        return configured > 0 ? configured : DEFAULT_MAXIMUM;
    }

    public static void requireTotal(long total) {
        int configured = BarcodeConfigUtil.parseIntSafe(
                ConfigurationProperties.getInstance().getPropertyValue(Property.MAX_REQUEST_LABEL_TOTAL),
                DEFAULT_MAXIMUM_TOTAL);
        int maximum = configured > 0 ? configured : DEFAULT_MAXIMUM_TOTAL;
        if (total > maximum) {
            throw new LabelQuantityRefusedException(LabelQuantityRefusedException.JOB_TOO_LARGE,
                    "Print job exceeds the total limit of " + maximum + " labels; print smaller batches",
                    Map.of("max", maximum));
        }
    }

    public static void require(int quantity, int minimum) {
        if (quantity < minimum) {
            throw new LabelQuantityRefusedException(LabelQuantityRefusedException.BELOW_MINIMUM,
                    "Label quantity is below the minimum of " + minimum);
        }
        int maximum = maximum();
        if (quantity > maximum) {
            throw new LabelQuantityRefusedException(LabelQuantityRefusedException.ABOVE_LIMIT,
                    "Label quantity exceeds the request safety limit of " + maximum, Map.of("max", maximum));
        }
    }
}
