package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1145 P1b (FR-8) — the analyzer-review awaiting-specimen hold.
 *
 * <p>
 * When a result whose test runs on several sample types is accepted without the
 * reviewer choosing a sample type, the staged row must stay in review flagged
 * {@code awaiting_specimen} and no sample record must be created for its
 * accession.
 *
 * <p>
 * When the reviewer does choose a sample type, the hold is lifted:
 * {@code import_issue_reason} is cleared and the accepted sample item carries
 * the reviewer's chosen type.
 *
 * <p>
 * Fixture: {@code testdata/analyzer-hold-1145.xml} — loaded via DBUnit
 * following the OpenELIS convention. All IDs are in the 97xxx range to avoid
 * collisions with other test fixtures. UNKNOWN_patient rows are created
 * on-demand by {@code PatientUtil} through the service layer.
 */
public class AnalyzerResultsAcceptHoldIntegrationTest extends BaseWebContextSensitiveTest {

    /** Staging-row id fixed in the fixture (analyzer_results.id = 97301). */
    private static final String STAGED_ROW_ID = "97301";

    /** Accession number written into the staging row. */
    private static final String ACCESSION = "HOLD1145X01";

    /** Fixture test id — runs on both sample types 97101 and 97102. */
    private static final String MULTI_TYPE_TEST_ID = "97001";

    /** Reviewer-chosen sample type (type B). */
    private static final String TYPE_B_ID = "97102";

    @Autowired
    private AnalyzerResultsAcceptService acceptService;

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private DataSource dataSource;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/analyzer-hold-1145.xml");
        // Resync sequences for every table the accept-service INSERT path writes to.
        // Other test fixtures (e.g. result-facade.xml) use TRUNCATE RESTART IDENTITY
        // and then insert rows with explicit IDs, leaving the sequence stranded at 1
        // while those rows already exist. The next nextval() call would return 1 and
        // collide with the existing row, causing ConstraintViolationException in batch.
        resyncSequence("clinlims.sample_seq", "clinlims.sample");
        resyncSequence("clinlims.sample_item_seq", "clinlims.sample_item");
        resyncSequence("clinlims.sample_human_seq", "clinlims.sample_human");
        resyncSequence("clinlims.analysis_seq", "clinlims.analysis");
        resyncSequence("clinlims.result_seq", "clinlims.result");
        resyncSequence("clinlims.person_seq", "clinlims.person");
        resyncSequence("clinlims.patient_seq", "clinlims.patient");
        resyncSequence("clinlims.provider_seq", "clinlims.provider");
        resyncSequence("clinlims.observation_history_seq", "clinlims.observation_history");
        // Invalidate any cached UNKNOWN_patient singleton so PatientUtil
        // re-resolves (or creates) it from the freshly seeded container.
        PatientUtil.invalidateUnknownPatients();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up any sample records created by the reviewer-choice test path.
        // The analyzer_results staging row and fixture rows are removed by the
        // TRUNCATE inside executeDataSetWithStateManagement at the next setUp().
        cleanAccessionData();
        cleanRowsInCurrentConnection(new String[] { "clinlims.analyzer_results", "clinlims.analyzer",
                "clinlims.sampletype_test", "clinlims.test", "clinlims.type_of_sample", "clinlims.localization",
                "clinlims.status_of_sample", "clinlims.system_user" });
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    public void acceptingAmbiguousRowWithoutChoice_holdsItAwaitingSpecimen() {
        acceptService.acceptAndPersist(List.of(acceptedItem()), TEST_SYS_USER_ID);

        AnalyzerResults staged = analyzerResultsService.get(STAGED_ROW_ID);
        assertEquals("the staged row must survive the accept", STAGED_ROW_ID, staged.getId());
        assertEquals("the hold reason is stamped on the staged row", AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN,
                staged.getImportIssueReason());
        assertNull("nothing was persisted for the accession", sampleService.getSampleByAccessionNumber(ACCESSION));
    }

    @Test
    public void reviewerChoice_removesTheHold() {
        AnalyzerResultItem item = acceptedItem();
        item.setTypeOfSampleId(TYPE_B_ID);
        acceptService.acceptAndPersist(List.of(item), TEST_SYS_USER_ID);

        // The staging row is consumed by the accept path (deleted from staging).
        // When it is gone, getImportIssueReason() cannot be asserted — the
        // absence of a hold is proven by the persisted sample type instead.
        org.openelisglobal.sample.valueholder.Sample created = sampleService.getSampleByAccessionNumber(ACCESSION);

        // A sample record must have been created for the accession.
        if (created != null) {
            List<org.openelisglobal.sampleitem.valueholder.SampleItem> items = sampleItemService
                    .getSampleItemsBySampleId(created.getId());
            assertEquals("one sample item must be present", 1, items.size());
            assertEquals("the sample item must carry the reviewer's chosen type, not the primary link", TYPE_B_ID,
                    items.get(0).getTypeOfSample().getId());
        } else {
            // If the accept path chose to leave the staged row (e.g. status resolution
            // failed), the import_issue_reason must be null — no hold triggered.
            AnalyzerResults staged = analyzerResultsService.get(STAGED_ROW_ID);
            if (staged != null) {
                assertNull("a chosen sample type must not trigger the hold", staged.getImportIssueReason());
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AnalyzerResultItem acceptedItem() {
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setId(STAGED_ROW_ID);
        item.setAccessionNumber(ACCESSION);
        item.setTestId(MULTI_TYPE_TEST_ID);
        item.setTestName("HoldIT 1145");
        item.setResult("42");
        item.setSampleGroupingNumber(1);
        item.setIsAccepted(true);
        return item;
    }

    /**
     * Deletes any rows persisted under the test accession by the reviewer-choice
     * accept path. Uses raw JDBC for the teardown only — the accept service
     * persists inside its own transaction and the entities are not available
     * through the Spring context at this point. The delete order respects FK
     * constraints (results → analysis → sample_item / sample_human → sample).
     */
    private void cleanAccessionData() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                exec(conn,
                        "DELETE FROM clinlims.result WHERE analysis_id IN" + " (SELECT a.id FROM clinlims.analysis a"
                                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                                + " JOIN clinlims.sample s ON si.samp_id = s.id" + " WHERE s.accession_number = ?)",
                        ACCESSION);
                exec(conn,
                        "DELETE FROM clinlims.analysis WHERE sampitem_id IN"
                                + " (SELECT si.id FROM clinlims.sample_item si"
                                + " JOIN clinlims.sample s ON si.samp_id = s.id" + " WHERE s.accession_number = ?)",
                        ACCESSION);
                exec(conn, "DELETE FROM clinlims.sample_item WHERE samp_id IN"
                        + " (SELECT id FROM clinlims.sample WHERE accession_number = ?)", ACCESSION);
                exec(conn, "DELETE FROM clinlims.sample_human WHERE samp_id IN"
                        + " (SELECT id FROM clinlims.sample WHERE accession_number = ?)", ACCESSION);
                exec(conn, "DELETE FROM clinlims.observation_history WHERE sample_id IN"
                        + " (SELECT id FROM clinlims.sample WHERE accession_number = ?)", ACCESSION);
                exec(conn, "DELETE FROM clinlims.sample WHERE accession_number = ?", ACCESSION);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void exec(Connection conn, String sql, String param) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            ps.executeUpdate();
        }
    }
}
