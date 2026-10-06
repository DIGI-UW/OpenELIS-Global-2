package org.openelisglobal.orderentry.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.orderentry.valueholder.ArrivalCondition;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Who received a sample and the condition it arrived in, as a step save sends
 * them (FRS clinical order entry v4, FR-B23 and FR-C9a). Pure, so the rules are
 * tested without the application context.
 */
public final class SampleReceiptAndArrival {

    static final BigDecimal MIN_ARRIVAL_TEMPERATURE = new BigDecimal("-100");
    static final BigDecimal MAX_ARRIVAL_TEMPERATURE = new BigDecimal("60");

    private SampleReceiptAndArrival() {
    }

    /**
     * Sets the receiver, the arrival condition and the measured temperature on
     * {@code item}. The receiver defaults to the user saving a sample that has a
     * receipt date. An unknown condition or an implausible temperature is not
     * stored. A recorded arrival is stamped with the saving user and {@code now}.
     */
    public static void apply(SampleItem item, String receivedById, String arrivalCondition, String arrivalTemperature,
            String currentUserId, Timestamp now) {
        String savingUser = StringUtils.trimToEmpty(currentUserId);
        if (GenericValidator.isInt(StringUtils.trimToEmpty(receivedById))) {
            item.setReceivedById(receivedById.trim());
        } else if (item.getReceivedDate() != null && GenericValidator.isInt(savingUser)) {
            item.setReceivedById(savingUser);
        }
        ArrivalCondition condition = ArrivalCondition.fromValue(arrivalCondition);
        item.setArrivalCondition(condition == null ? null : condition.name());
        item.setArrivalTemperature(plausibleTemperature(arrivalTemperature));
        if (item.getArrivalCondition() != null || item.getArrivalTemperature() != null) {
            item.setArrivalRecordedById(GenericValidator.isInt(savingUser) ? savingUser : null);
            item.setArrivalRecordedAt(now);
        }
    }

    /**
     * A temperature in degrees C rounded to one decimal, accepting a decimal comma,
     * or null when blank, not a number or outside -100 to 60.
     */
    public static BigDecimal plausibleTemperature(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            BigDecimal degrees = new BigDecimal(value.trim().replace(',', '.')).setScale(1, RoundingMode.HALF_UP);
            return degrees.compareTo(MIN_ARRIVAL_TEMPERATURE) < 0 || degrees.compareTo(MAX_ARRIVAL_TEMPERATURE) > 0
                    ? null
                    : degrees;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
