package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;

/**
 * The date a reference laboratory put on its own report, typed alongside a
 * result coming back from it. The save records it against the referral, and an
 * unparseable value used to be logged and dropped there: the save reported
 * success and the External Referrals report kept printing a blank report-date
 * column, with nothing to tell the user their date had not been taken.
 */
public class ResultsValidationReferralReportDateTest extends BaseWebContextSensitiveTest {

    private static final String REPORT_DATE_ERROR = "errors.referral.reportDate";

    @Autowired
    private ResultsValidation resultsValidation;

    @Test
    public void validateItem_raisesNoReportDateError_whenTheRowCarriesNoReferral() {
        TestResultItem item = new TestResultItem();

        assertFalse(hasReportDateError(resultsValidation.validateItem(item)));
    }

    @Test
    public void validateItem_raisesNoReportDateError_whenTheReferralCarriesNoReportDate() {
        assertFalse(hasReportDateError(resultsValidation.validateItem(itemWithReportDate(null))));
        assertFalse(hasReportDateError(resultsValidation.validateItem(itemWithReportDate(""))));
        assertFalse(hasReportDateError(resultsValidation.validateItem(itemWithReportDate("   "))));
    }

    @Test
    public void validateItem_acceptsADateInTheConfiguredFormat() {
        String today = DateUtil.formatDateAsText(DateUtil.getNowAsTimestamp());

        assertFalse(hasReportDateError(resultsValidation.validateItem(itemWithReportDate(today))));
    }

    @Test
    public void validateItem_rejectsADateThatDoesNotExist() {
        // The 31st of February, whichever way round the configured format reads
        // the first two fields. Lenient parsing rolls this forward into March
        // instead of failing, which is how a mistyped date reached the referral
        // as a date nobody entered.
        Errors errors = resultsValidation.validateItem(itemWithReportDate("31/02/2026"));

        assertTrue(hasReportDateError(errors));
    }

    @Test
    public void validateItem_rejectsAValueThatIsNotADateAtAll() {
        assertTrue(hasReportDateError(resultsValidation.validateItem(itemWithReportDate("last Tuesday"))));
    }

    @Test
    public void validateItem_reportsTheReportDateSeparatelyFromTheTestDate() {
        // Both dates are wrong, and the row has to say so about both: the two
        // fields sit next to each other in the panel.
        TestResultItem item = itemWithReportDate("31/02/2026");
        item.setTestDate("also not a date");

        Errors errors = resultsValidation.validateItem(item);

        assertTrue(hasReportDateError(errors));
        assertTrue(hasError(errors, "errors.date"));
    }

    private TestResultItem itemWithReportDate(String reportDate) {
        ReferralItem referralItem = new ReferralItem();
        referralItem.setReferredReportDate(reportDate);
        TestResultItem item = new TestResultItem();
        item.setReferralItem(referralItem);
        return item;
    }

    private boolean hasReportDateError(Errors errors) {
        return hasError(errors, REPORT_DATE_ERROR);
    }

    private boolean hasError(Errors errors, String code) {
        return errors.getAllErrors().stream().anyMatch(error -> code.equals(error.getCode()));
    }
}
