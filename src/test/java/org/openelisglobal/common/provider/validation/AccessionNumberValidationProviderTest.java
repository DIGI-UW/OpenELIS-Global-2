package org.openelisglobal.common.provider.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class AccessionNumberValidationProviderTest extends BaseWebContextSensitiveTest {

    private AccessionNumberValidationProvider provider;

    private static final String VALID = "valid";
    private static final String INVALID = "invalid";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        provider = new AccessionNumberValidationProvider();
        executeDataSetWithStateManagement("testdata/accession-number-validation-test.xml");
    }

    // -------------------------------------------------------------------------
    // validate() - null/empty targetId tests
    // -------------------------------------------------------------------------

    @Test
    public void validate_shouldReturnInvalid_whenTargetIdIsNull() throws Exception {
        String result = provider.validate(null, null);
        assertEquals(INVALID, result);
    }

    @Test
    public void validate_shouldReturnInvalid_whenTargetIdIsEmpty() throws Exception {
        String result = provider.validate("", null);
        assertEquals(INVALID, result);
    }

    @Test
    public void validate_shouldReturnInvalid_whenTargetIdIsBlank() throws Exception {
        String result = provider.validate("   ", null);
        assertEquals(INVALID, result);
    }

    // -------------------------------------------------------------------------
    // validate() - sample not found
    // -------------------------------------------------------------------------

    @Test
    public void validate_shouldReturnInvalid_whenSampleNotFound() throws Exception {
        String result = provider.validate("NONEXISTENT999", null);
        assertEquals(INVALID, result);
    }

    // -------------------------------------------------------------------------
    // validate() - sample found, no form specified
    // -------------------------------------------------------------------------

    @Test
    public void validate_shouldReturnValid_whenSampleFoundAndNoFormSpecified() throws Exception {
        String result = provider.validate("ACC-401", null);
        assertEquals(VALID, result);
    }

    // -------------------------------------------------------------------------
    // validate() - sample found, unrecognized form
    // -------------------------------------------------------------------------

    @Test
    public void validate_shouldReturnValid_whenSampleFoundAndUnrecognizedForm() throws Exception {
        String result = provider.validate("ACC-401", "unknownForm");
        assertEquals(VALID, result);
    }

    // -------------------------------------------------------------------------
    // validate() - humanSampleOneForm with null status
    // -------------------------------------------------------------------------

    @Test
    public void validate_shouldReturnInvalid_whenHumanSampleOneFormAndSampleStatusIsNull() throws Exception {
        // sample 402 has no status
        String result = provider.validate("ACC-402", "humanSampleOneForm");
        assertEquals(INVALID, result);
    }

    // -------------------------------------------------------------------------
    // validate() - humanSampleTwoForm with null status
    // -------------------------------------------------------------------------

    @Test
    public void validate_shouldReturnInvalid_whenHumanSampleTwoFormAndSampleStatusIsNull() throws Exception {
        String result = provider.validate("ACC-402", "humanSampleTwoForm");
        assertEquals(INVALID, result);
    }
}
