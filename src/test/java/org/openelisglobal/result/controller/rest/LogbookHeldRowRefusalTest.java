package org.openelisglobal.result.controller.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.resultrun.form.RunHoldCause;
import org.openelisglobal.resultrun.form.WaitingRun;
import org.openelisglobal.test.beanItems.TestResultItem;

/**
 * OGC-1200: a Generic Sample results save is refused whole when one of its rows
 * is held, so the refusal names the held row's lab number and test, and the
 * user knows which value to clear.
 */
public class LogbookHeldRowRefusalTest extends BaseWebContextSensitiveTest {

    @Test
    public void theRefusalNamesTheHeldRowsLabNumberAndTestBeforeTheRun() {
        TestResultItem item = new TestResultItem();
        item.setAccessionNumber("DEV01260000000000385");
        item.setTestName("Amylase(Serum)");
        WaitingRun held = new WaitingRun(327L, "RUN-327", "RESULT_ENTRY", null, false, null)
                .held(RunHoldCause.QC_MISSING);

        assertEquals(
                "DEV01260000000000385, Amylase(Serum): RUN-327 has no control for this test, and the test"
                        + " requires one on every run, so its results there are held: they cannot be accepted, entered,"
                        + " edited or released until a control is recorded on the run or the test is decided.",
                LogbookResultsRestController.heldRowRefusal(item, held));
    }
}
