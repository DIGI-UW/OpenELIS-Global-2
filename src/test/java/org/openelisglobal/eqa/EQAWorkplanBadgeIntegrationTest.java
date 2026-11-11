package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.workplan.controller.rest.WorkplanRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The EQA badge on the workplan. Every workplan variant builds its rows by hand
 * and none of them set the flag the badge reads, so the bench saw no marker on
 * any of them; they now all route through one method on the shared base
 * controller, which is what this exercises against real rows.
 *
 * <p>
 * The workplan package is outside the test component scan (its controllers pull
 * in the whole result-entry stack), so the base controller is built by hand
 * with the one collaborator this path uses — the same pattern the blinding test
 * uses for the scheduler.
 */
public class EQAWorkplanBadgeIntegrationTest extends EQASpineTestBase {

    private static final long EQA_SAMPLE_ID = 98132L;
    private static final long PLAIN_SAMPLE_ID = 98133L;
    private static final long WITHDRAWN_SAMPLE_ID = 98134L;

    @Autowired
    private SampleEQAService sampleEQAService;

    private WorkplanRestController workplan;

    @Before
    public void seedOrdersAndController() {
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE sample_id IN (?, ?, ?)", EQA_SAMPLE_ID, PLAIN_SAMPLE_ID,
                WITHDRAWN_SAMPLE_ID);
        jdbc.update("DELETE FROM clinlims.sample WHERE id IN (?, ?, ?)", EQA_SAMPLE_ID, PLAIN_SAMPLE_ID,
                WITHDRAWN_SAMPLE_ID);

        insertSample(EQA_SAMPLE_ID, "EQAWP98132");
        insertSample(PLAIN_SAMPLE_ID, "EQAWP98133");
        insertSample(WITHDRAWN_SAMPLE_ID, "EQAWP98134");
        jdbc.update("INSERT INTO clinlims.sample_eqa (id, sample_id, is_eqa_sample, eqa_priority, sys_user_id,"
                + " last_updated) VALUES (?, ?, true, 'URGENT', ?, NOW())", 98135L, EQA_SAMPLE_ID, USER);
        // A row that exists but says this is not an EQA sample: the flag is the
        // fact, not the presence of the row.
        jdbc.update(
                "INSERT INTO clinlims.sample_eqa (id, sample_id, is_eqa_sample, eqa_priority, sys_user_id,"
                        + " last_updated) VALUES (?, ?, false, 'STANDARD', ?, NOW())",
                98136L, WITHDRAWN_SAMPLE_ID, USER);

        workplan = new WorkplanRestController();
        ReflectionTestUtils.setField(workplan, "sampleEQAService", sampleEQAService);
    }

    @Test
    public void anEqaOrderIsFlaggedWithItsPriority() {
        TestResultItem row = new TestResultItem();

        ReflectionTestUtils.invokeMethod(workplan, "markEqaSample", row, sample(EQA_SAMPLE_ID));

        assertTrue("the badge reads this flag", row.isEqaSample());
        assertEquals("URGENT", row.getEqaPriority());
    }

    @Test
    public void anOrdinaryOrderIsLeftAlone() {
        TestResultItem row = new TestResultItem();

        ReflectionTestUtils.invokeMethod(workplan, "markEqaSample", row, sample(PLAIN_SAMPLE_ID));

        assertFalse(row.isEqaSample());
        assertNull(row.getEqaPriority());
    }

    @Test
    public void anOrderWhoseEqaFlagIsOffIsLeftAlone() {
        TestResultItem row = new TestResultItem();

        ReflectionTestUtils.invokeMethod(workplan, "markEqaSample", row, sample(WITHDRAWN_SAMPLE_ID));

        assertFalse(row.isEqaSample());
        assertNull(row.getEqaPriority());
    }

    @Test
    public void aRowWithNoSampleBehindItIsNotAnError() {
        TestResultItem row = new TestResultItem();

        ReflectionTestUtils.invokeMethod(workplan, "markEqaSample", row, (Sample) null);

        assertFalse(row.isEqaSample());
    }

    private void insertSample(long id, String accession) {
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date,"
                + " is_confirmation, lastupdated) VALUES (?, ?, NOW(), NOW(), false, NOW())", id, accession);
    }

    private Sample sample(long id) {
        Sample sample = new Sample();
        sample.setId(String.valueOf(id));
        return sample;
    }
}
