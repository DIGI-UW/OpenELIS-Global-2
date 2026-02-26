package org.openelisglobal.common.provider.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class ProgramAccessionValidatorTest extends BaseWebContextSensitiveTest {

    // -------------------------------------------------------------------------
    // createFirstAccessionNumber tests
    // -------------------------------------------------------------------------

    @Test
    public void createFirstAccessionNumber_shouldReturnProgramCodeWithStartingIncrement() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        String result = validator.createFirstAccessionNumber("ABCD");
        assertEquals("ABCD00001", result);
    }

    @Test
    public void createFirstAccessionNumber_shouldWorkWithDifferentProgramCodes() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        assertEquals("TEST00001", validator.createFirstAccessionNumber("TEST"));
        assertEquals("VCT000001", validator.createFirstAccessionNumber("VCT0"));
    }

    // -------------------------------------------------------------------------
    // incrementAccessionNumber tests
    // -------------------------------------------------------------------------

    @Test
    public void incrementAccessionNumber_shouldIncrementByOne() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        String result = validator.incrementAccessionNumber("ABCD00001");
        assertEquals("ABCD00002", result);
    }

    @Test
    public void incrementAccessionNumber_shouldPadWithLeadingZeros() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        String result = validator.incrementAccessionNumber("ABCD00009");
        assertEquals("ABCD00010", result);
    }

    @Test
    public void incrementAccessionNumber_shouldHandleLargeIncrement() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        String result = validator.incrementAccessionNumber("ABCD09999");
        assertEquals("ABCD10000", result);
    }

    @Test
    public void incrementAccessionNumber_shouldUppercaseProgramCode() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        String result = validator.incrementAccessionNumber("abcd00001");
        assertEquals("ABCD00002", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void incrementAccessionNumber_shouldThrowException_whenAtMaxValue() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        validator.incrementAccessionNumber("ABCD99999");
    }

    // -------------------------------------------------------------------------
    // getMaxAccessionLength / getMinAccessionLength / getChangeableLength tests
    // -------------------------------------------------------------------------

    @Test
    public void getMaxAccessionLength_shouldReturn9() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        assertEquals(9, validator.getMaxAccessionLength());
    }

    @Test
    public void getMinAccessionLength_shouldEqualMaxAccessionLength() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        assertEquals(validator.getMaxAccessionLength(), validator.getMinAccessionLength());
    }

    @Test
    public void getChangeableLength_shouldReturn5() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        assertEquals(5, validator.getChangeableLength());
    }

    @Test
    public void getInvarientLength_shouldReturn4() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        assertEquals(4, validator.getInvarientLength());
    }

    @Test
    public void needProgramCode_shouldReturnTrue() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        assertEquals(true, validator.needProgramCode());
    }

    @Test
    public void getPrefix_shouldReturnNull() {
        ProgramAccessionValidator validator = new ProgramAccessionValidator();
        assertEquals(null, validator.getPrefix());
    }
}
