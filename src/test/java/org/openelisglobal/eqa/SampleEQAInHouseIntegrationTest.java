package org.openelisglobal.eqa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Whether an order belongs to an in-house scheme. Result entry asks this per
 * row so it can keep the EQA badge off blinded in-house orders: an analyst
 * running them must not be able to tell them from patient samples.
 */
public class SampleEQAInHouseIntegrationTest extends EQASpineTestBase {

    private static final long IN_HOUSE_SAMPLE_ID = 98141L;
    private static final long EXTERNAL_SAMPLE_ID = 98142L;
    private static final long FLAG_OFF_SAMPLE_ID = 98143L;
    private static final long PLAIN_SAMPLE_ID = 98144L;

    @Autowired
    private SampleEQAService sampleEQAService;

    @Before
    public void seedOrders() {
        deleteSeededOrders();

        Long inHouseCycle = insertCycle(insertScheme("In-house CD4", EQASchemeType.IN_HOUSE, null), 1);
        Long externalCycle = insertCycle(insertScheme("WHO AFRO HIV VL", EQASchemeType.INTERNATIONAL_PT, "WHO"), 1);

        insertSample(IN_HOUSE_SAMPLE_ID, "EQAIH98141");
        insertSample(EXTERNAL_SAMPLE_ID, "EQAIH98142");
        insertSample(FLAG_OFF_SAMPLE_ID, "EQAIH98143");
        insertSample(PLAIN_SAMPLE_ID, "EQAIH98144");

        insertSampleEqa(98145L, IN_HOUSE_SAMPLE_ID, true, inHouseCycle);
        insertSampleEqa(98146L, EXTERNAL_SAMPLE_ID, true, externalCycle);
        // The row exists but says this is not an EQA sample: the flag is the fact.
        insertSampleEqa(98147L, FLAG_OFF_SAMPLE_ID, false, inHouseCycle);
    }

    @Override
    protected void cleanEqaTables() {
        // sample_eqa points at eqa_cycle, so it goes before the base clean-up.
        deleteSeededOrders();
        super.cleanEqaTables();
    }

    @Test
    public void anOrderFromAnInHouseSchemeIsInHouse() {
        assertTrue(sampleEQAService.isInHouse(IN_HOUSE_SAMPLE_ID));
    }

    @Test
    public void anOrderFromAnExternalSchemeIsNot() {
        assertFalse(sampleEQAService.isInHouse(EXTERNAL_SAMPLE_ID));
    }

    @Test
    public void anOrderWhoseEqaFlagIsOffIsNot() {
        assertFalse(sampleEQAService.isInHouse(FLAG_OFF_SAMPLE_ID));
    }

    @Test
    public void anOrdinaryOrderIsNot() {
        assertFalse(sampleEQAService.isInHouse(PLAIN_SAMPLE_ID));
    }

    private void deleteSeededOrders() {
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE sample_id BETWEEN ? AND ?", IN_HOUSE_SAMPLE_ID,
                PLAIN_SAMPLE_ID);
        jdbc.update("DELETE FROM clinlims.sample WHERE id BETWEEN ? AND ?", IN_HOUSE_SAMPLE_ID, PLAIN_SAMPLE_ID);
    }

    private void insertSample(long id, String accession) {
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date,"
                + " is_confirmation, lastupdated) VALUES (?, ?, NOW(), NOW(), false, NOW())", id, accession);
    }

    private void insertSampleEqa(long id, long sampleId, boolean isEqa, Long cycleId) {
        jdbc.update(
                "INSERT INTO clinlims.sample_eqa (id, sample_id, is_eqa_sample, eqa_priority, cycle_id,"
                        + " sys_user_id, last_updated) VALUES (?, ?, ?, 'STANDARD', ?, ?, NOW())",
                id, sampleId, isEqa, cycleId, USER);
    }
}
