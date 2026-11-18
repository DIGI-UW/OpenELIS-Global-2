package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MoneyTest {

    @Test
    public void wholeRupeesBecomePaise() {
        assertEquals(Long.valueOf(150000L), Money.parseRupeesToPaise("1500"));
    }

    @Test
    public void fractionalRupeesBecomePaise() {
        assertEquals(Long.valueOf(123450L), Money.parseRupeesToPaise("1234.50"));
        assertEquals(Long.valueOf(5L), Money.parseRupeesToPaise("0.05"));
    }

    @Test
    public void blankClearsThePrice() {
        assertNull(Money.parseRupeesToPaise(""));
        assertNull(Money.parseRupeesToPaise("   "));
        assertNull(Money.parseRupeesToPaise(null));
    }

    @Test
    public void paiseFormatsAsRupeesWithTwoDecimals() {
        assertEquals("1234.50", Money.formatPaiseAsRupees(123450L));
        assertEquals("0.05", Money.formatPaiseAsRupees(5L));
        assertNull(Money.formatPaiseAsRupees(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativePriceIsRejected() {
        Money.parseRupeesToPaise("-10");
    }

    @Test(expected = IllegalArgumentException.class)
    public void morethanTwoDecimalsIsRejected() {
        Money.parseRupeesToPaise("10.555");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonNumericIsRejected() {
        Money.parseRupeesToPaise("ten");
    }

    @Test(expected = IllegalArgumentException.class)
    public void scientificNotationIsRejected() {
        // BigDecimal would read this as 100000 rupees.
        Money.parseRupeesToPaise("1e5");
    }

    @Test(expected = IllegalArgumentException.class)
    public void plusSignIsRejected() {
        Money.parseRupeesToPaise("+5");
    }

    @Test(expected = IllegalArgumentException.class)
    public void groupingCommasAreRejected() {
        Money.parseRupeesToPaise("1,000");
    }

    @Test
    public void trailingZeroDecimalsAndSurroundingSpacesAreAccepted() {
        assertEquals(Long.valueOf(1050L), Money.parseRupeesToPaise(" 10.500 "));
        assertEquals(Long.valueOf(1000L), Money.parseRupeesToPaise("10."));
    }

    @Test
    public void theMaximumPriceIsAccepted() {
        assertEquals(Long.valueOf(Money.MAX_PRICE_PAISE), Money.parseRupeesToPaise("1000000"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aPaiseOverTheMaximumIsRejected() {
        Money.parseRupeesToPaise("1000000.01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void aNumberTooBigForALongIsRejectedNotACrash() {
        // Must be IllegalArgumentException (a 422), never ArithmeticException (a 500).
        Money.parseRupeesToPaise("99999999999999999999999");
    }
}
