package org.openelisglobal.common.util.validator;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;

public class CustomDateValidatorTest {

    @Test
    public void testValidateDate_Past_TomorrowAllowed() {
        CustomDateValidator validator = CustomDateValidator.getInstance();

        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_MONTH, 1); // Tomorrow
        Date tomorrow = cal.getTime();

        // This effectively simulates a user in a timezone ahead of the server
        // selecting "Today", which the server sees as "Tomorrow"
        String result = validator.validateDate(tomorrow, DateRelation.PAST);

        // This validates that "Tomorrow" (up to 24h) is accepted as PAST
        // to handle timezone discrepancies (User ahead of Server)
        assertEquals(IActionConstants.VALID, result);
    }

    @Test
    public void testValidateDate_Past_DayAfterTomorrowRejected() {
        CustomDateValidator validator = CustomDateValidator.getInstance();

        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_MONTH, 2); // 2 days in future
        Date dayAfterTomorrow = cal.getTime();

        String result = validator.validateDate(dayAfterTomorrow, DateRelation.PAST);

        assertEquals(IActionConstants.INVALID_TO_LARGE, result);
    }
}
