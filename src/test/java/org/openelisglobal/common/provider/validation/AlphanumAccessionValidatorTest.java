package org.openelisglobal.common.provider.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class AlphanumAccessionValidatorTest {

    // -------------------------------------------------------------------------
    // normalizeAccessionString tests
    // -------------------------------------------------------------------------

    @Test
    public void normalizeAccessionString_shouldRemoveDashes_whenDashesPresent() {
        String result = AlphanumAccessionValidator.normalizeAccessionString("26-ABC-123-456");
        assertEquals("26ABC123456", result);
    }

    @Test
    public void normalizeAccessionString_shouldReturnSameString_whenNoDashesPresent() {
        String result = AlphanumAccessionValidator.normalizeAccessionString("26ABC123456");
        assertEquals("26ABC123456", result);
    }

    @Test
    public void normalizeAccessionString_shouldReturnNull_whenInputIsNull() {
        String result = AlphanumAccessionValidator.normalizeAccessionString(null);
        assertNull(result);
    }

    @Test
    public void normalizeAccessionString_shouldReturnEmptyString_whenInputIsEmpty() {
        String result = AlphanumAccessionValidator.normalizeAccessionString("");
        assertEquals("", result);
    }

    // -------------------------------------------------------------------------
    // convertAlphaNumLabNumForDisplay tests
    // -------------------------------------------------------------------------

    @Test
    public void convertAlphaNumLabNumForDisplay_shouldReturnNull_whenInputIsNull() {
        String result = AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay(null);
        assertNull(result);
    }

    @Test
    public void convertAlphaNumLabNumForDisplay_shouldReturnEmpty_whenInputIsEmpty() {
        String result = AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay("");
        assertEquals("", result);
    }

    @Test
    public void convertAlphaNumLabNumForDisplay_shouldFormatWithDashes_whenNoPrefix() {
        String result = AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay("26123456");
        assertEquals("26-123-456", result);
    }

    @Test
    public void convertAlphaNumLabNumForDisplay_shouldFormatWithPrefixAndDashes_whenPrefixPresent() {
        String result = AlphanumAccessionValidator.convertAlphaNumLabNumForDisplay("26AB123456");
        assertEquals("26-AB-123-456", result);
    }
}
