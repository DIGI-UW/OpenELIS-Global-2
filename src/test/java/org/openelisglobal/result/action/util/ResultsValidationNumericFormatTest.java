package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;

/**
 * A numeric result may be written plainly or in scientific notation, in any of
 * the forms a technician types (1.5e5, 1.5E+05, 1.5 x 10^5, 1.5×10⁵, 10⁻³).
 * Anything that is not a number is refused here: the entry field is free text,
 * so this is the check that keeps "abc" and a bare "3²" out of a numeric
 * result.
 */
public class ResultsValidationNumericFormatTest extends BaseWebContextSensitiveTest {

    private static final String NUMBER_FORMAT_ERROR = "errors.number.format";

    @Autowired
    private ResultsValidation resultsValidation;

    @Test
    public void validateItem_acceptsPlainNumbersWithOrWithoutAComparator() {
        for (String value : new String[] { "12.5", "0", "-3", "<5", ">1500" }) {
            assertFalse(value, hasNumberFormatError(resultsValidation.validateItem(numericItem(value))));
        }
    }

    @Test
    public void validateItem_acceptsScientificNotationInEveryWrittenForm() {
        for (String value : new String[] { "1.5e5", "1.5E+05", "1.5 x 10^5", "1.5×10⁵", "2×10⁻³", "10⁻³",
                "<1.5e-7" }) {
            assertFalse(value, hasNumberFormatError(resultsValidation.validateItem(numericItem(value))));
        }
    }

    @Test
    public void validateItem_rejectsTextAndBareSuperscripts() {
        for (String value : new String[] { "abc", "3²", "3.5⁻³", "1.5e", "NaN", "1e400" }) {
            assertTrue(value, hasNumberFormatError(resultsValidation.validateItem(numericItem(value))));
        }
    }

    @Test
    public void validateItem_leavesNonNumericResultTypesAlone() {
        TestResultItem item = numericItem("abc");
        item.setResultType("A");

        assertFalse(hasNumberFormatError(resultsValidation.validateItem(item)));
    }

    private TestResultItem numericItem(String value) {
        TestResultItem item = new TestResultItem();
        item.setResultType("N");
        item.setResultValue(value);
        return item;
    }

    private boolean hasNumberFormatError(Errors errors) {
        return errors.getAllErrors().stream().anyMatch(error -> NUMBER_FORMAT_ERROR.equals(error.getCode()));
    }
}
