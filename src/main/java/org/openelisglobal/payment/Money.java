package org.openelisglobal.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Converts between user-entered rupees and stored paise (1/100 rupee, integer).
 */
public final class Money {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * Highest price a test or panel may have: ₹10,00,000 (10 lakh). Well above any
     * real lab test, so it only catches typos such as extra zeros. The frontend
     * check (priceValidation.js) uses the same limit.
     */
    public static final long MAX_PRICE_PAISE = 100_000_000L;

    /**
     * Plain digits with an optional decimal part. Rules out what BigDecimal would
     * otherwise accept: signs, exponents ("1e5") and grouping commas.
     */
    private static final Pattern PLAIN_AMOUNT = Pattern.compile("\\d+(\\.\\d*)?");

    private Money() {
    }

    /**
     * Parses a rupee amount such as "1234.50" into paise. Blank or null means no
     * price. Anything else that is not a plain amount of at most two decimals, up
     * to {@link #MAX_PRICE_PAISE}, throws IllegalArgumentException.
     */
    public static Long parseRupeesToPaise(String rupees) {
        if (rupees == null || rupees.trim().isEmpty()) {
            return null;
        }
        String trimmed = rupees.trim();
        if (!PLAIN_AMOUNT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Price must be a plain amount such as 1234.50: " + rupees);
        }
        BigDecimal amount = new BigDecimal(trimmed);
        if (amount.scale() > 2 && amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Price allows at most two decimal places: " + rupees);
        }
        BigDecimal paise = amount.multiply(HUNDRED).setScale(0, RoundingMode.UNNECESSARY);
        // Compare before converting, so a huge number is refused, not overflowed.
        if (paise.compareTo(BigDecimal.valueOf(MAX_PRICE_PAISE)) > 0) {
            throw new IllegalArgumentException("Price is above the maximum of 1000000 rupees: " + rupees);
        }
        return paise.longValueExact();
    }

    /** Formats paise as rupees with two decimals, e.g. 123450 -> "1234.50". */
    public static String formatPaiseAsRupees(Long paise) {
        if (paise == null) {
            return null;
        }
        return BigDecimal.valueOf(paise).divide(HUNDRED, 2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
