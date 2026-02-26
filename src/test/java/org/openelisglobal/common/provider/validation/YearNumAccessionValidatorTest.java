package org.openelisglobal.common.provider.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class YearNumAccessionValidatorTest extends BaseWebContextSensitiveTest {

    // -------------------------------------------------------------------------
    // createFirstAccessionNumber tests
    // -------------------------------------------------------------------------

    @Test
    public void createFirstAccessionNumber_shouldReturnYearWithIncrement_whenNoSeparator() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        String result = validator.createFirstAccessionNumber(null);
        // should be current 2-digit year + 000001
        assertEquals(6 + 2, result.length());
    }

    @Test
    public void createFirstAccessionNumber_shouldIncludesSeparator_whenSeparatorProvided() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, '-');
        String result = validator.createFirstAccessionNumber(null);
        // should be YY-000001 (9 chars)
        assertEquals(9, result.length());
    }

    @Test
    public void createFirstAccessionNumber_shouldEndWithAllOnesInLastDigit() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        String result = validator.createFirstAccessionNumber(null);
        assertEquals("000001", result.substring(2));
    }

    // -------------------------------------------------------------------------
    // incrementAccessionNumber tests
    // -------------------------------------------------------------------------

    @Test
    public void incrementAccessionNumber_shouldIncrementByOne_whenNoSeparator() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        String current = validator.createFirstAccessionNumber(null); // YY000001
        String next = validator.incrementAccessionNumber(current);
        assertEquals(current.substring(0, 2) + "000002", next);
    }

    @Test
    public void incrementAccessionNumber_shouldIncrementByOne_whenSeparatorProvided() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, '-');
        String current = validator.createFirstAccessionNumber(null); // YY-000001
        String next = validator.incrementAccessionNumber(current);
        assertEquals(current.substring(0, 2) + "-000002", next);
    }

    @Test(expected = IllegalArgumentException.class)
    public void incrementAccessionNumber_shouldThrowException_whenAtMaxValue() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        String year = current2DigitYear();
        validator.incrementAccessionNumber(year + "999999");
    }

    // -------------------------------------------------------------------------
    // getMaxAccessionLength / getMinAccessionLength / needProgramCode tests
    // -------------------------------------------------------------------------

    @Test
    public void getMaxAccessionLength_shouldReturn8_whenLengthIs6AndNoSeparator() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        assertEquals(8, validator.getMaxAccessionLength());
    }

    @Test
    public void getMaxAccessionLength_shouldReturn9_whenLengthIs6AndSeparatorProvided() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, '-');
        assertEquals(9, validator.getMaxAccessionLength());
    }

    @Test
    public void getMinAccessionLength_shouldEqualMaxAccessionLength() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        assertEquals(validator.getMaxAccessionLength(), validator.getMinAccessionLength());
    }

    @Test
    public void needProgramCode_shouldReturnFalse() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        assertEquals(false, validator.needProgramCode());
    }

    @Test
    public void getPrefix_shouldReturnNull() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        assertEquals(null, validator.getPrefix());
    }

    @Test
    public void getInvarientLength_shouldReturn0() {
        YearNumAccessionValidator validator = new YearNumAccessionValidator(6, null);
        assertEquals(0, validator.getInvarientLength());
    }

    // -------------------------------------------------------------------------
    // helper
    // -------------------------------------------------------------------------

    private String current2DigitYear() {
        int year = new java.util.GregorianCalendar().get(java.util.Calendar.YEAR) - 2000;
        return String.format("%02d", year);
    }
}
