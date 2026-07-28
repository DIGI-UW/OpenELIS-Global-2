package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1145 P1b (FR-8) — the analyzer-review awaiting-specimen hold: accepting a
 * result whose test runs on several sample types, with no reviewer-chosen type
 * and no existing sample item pinning one, must NOT first-match — the staged
 * row stays in review flagged {@code awaiting_specimen} and nothing is
 * persisted for its accession.
 */
public class AnalyzerResultsAcceptHoldIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TYPE_A = 97101L;
    private static final long TYPE_B = 97102L;
    private static final long MULTI_TYPE_TEST = 97001L;
    private static final long ANALYZER_ID = 97201L;
    private static final String ACCESSION = "HOLD1145X01";

    @Autowired
    private AnalyzerResultsAcceptService acceptService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;
    @Autowired
    private org.openelisglobal.common.services.IStatusService statusService;
    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String stagedRowId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        seedSampleType(TYPE_A, "Hold A 1145");
        seedSampleType(TYPE_B, "Hold B 1145");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'HoldIT 1145', 'HoldIT 1145', 'Y', ?, 'CLINICAL', true, NOW())",
                MULTI_TYPE_TEST, UUID.randomUUID().toString());
        insertJunction(TYPE_A, MULTI_TYPE_TEST);
        insertJunction(TYPE_B, MULTI_TYPE_TEST);
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, last_updated)"
                + " VALUES (?, 'HoldAnalyzer1145', true, NOW())", ANALYZER_ID);
        stagedRowId = String.valueOf(jdbc.queryForObject("SELECT nextval('analyzer_results_seq')", Long.class));
        jdbc.update("INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                + " iscontrol, test_id, last_updated) VALUES (?::numeric, ?, ?, 'HoldIT 1145', '42', false,"
                + " ?, NOW())", stagedRowId, ANALYZER_ID, ACCESSION, MULTI_TYPE_TEST);
        seedUnknownPatient();
        ensureCanonicalStatuses();
        typeOfSampleService.clearCache();
    }

    /**
     * Some fixtures (e.g. analyzer-results.xml) TRUNCATE status_of_sample and
     * reseed only their own rows, leaving the shared container — and the
     * StatusService cache — without the canonical statuses this dataset-less test's
     * accept flow needs (Testing Started, SampleEntered, record statuses…). Restore
     * any missing canonical rows and refresh the cache so the test is
     * order-independent. Canonical rows are never deleted.
     */
    private void ensureCanonicalStatuses() {
        String[][] canonical = { { "ORDER", "Test Entered" }, { "ORDER", "Testing Started" },
                { "ORDER", "Testing finished" }, { "ORDER", "NonConforming" }, { "SAMPLE", "SampleEntered" },
                { "SAMPLE", "SampleCanceled" }, { "SAMPLE", "Sample Rejected" }, { "SAMPLE", "SampleDisposed" },
                { "ANALYSIS", "Not Tested" }, { "ANALYSIS", "Test Canceled" }, { "ANALYSIS", "Technical Acceptance" },
                { "ANALYSIS", "Technical Rejected" }, { "ANALYSIS", "Biologist Rejection" },
                { "ANALYSIS", "Finalized" }, { "ANALYSIS", "NonConforming" }, { "ANALYSIS", "Sample Rejected" },
                { "EXTERNAL_ORDER", "Entered" }, { "EXTERNAL_ORDER", "Cancelled" }, { "EXTERNAL_ORDER", "Realized" },
                { "EXTERNAL_ORDER", "NonConforming" }, { "EXTERNAL_ORDER", "AwaitingSpecimen" } };
        for (String[] status : canonical) {
            jdbc.update("INSERT INTO clinlims.status_of_sample (id, description, code, status_type, name, is_active,"
                    + " lastupdated) SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM clinlims.status_of_sample), ?, '1',"
                    + " ?, ?, 'Y', NOW() WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample WHERE status_type"
                    + " = ? AND name = ?)", status[1], status[0], status[1], status[0], status[1]);
        }
        statusService.refreshCache();
    }

    /**
     * The no-sample-entry accept path needs PatientUtil's UNKNOWN_ singletons. When
     * absent, PatientUtil INSERTS them through the entity sequences, which are out
     * of sync with the seeded ids under full-suite ordering — so ensure the
     * canonical rows exist (never deleted; they are singletons, not test data) and
     * reset the static cache in case a rolled-back test populated it.
     */
    private void seedUnknownPatient() {
        Integer people = jdbc.queryForObject("SELECT count(*) FROM clinlims.person WHERE last_name = 'UNKNOWN_'",
                Integer.class);
        if (people == 0) {
            jdbc.update("INSERT INTO clinlims.person (id, last_name, lastupdated) VALUES (97301, 'UNKNOWN_', NOW())");
        }
        Long personId = jdbc.queryForObject(
                "SELECT id FROM clinlims.person WHERE last_name = 'UNKNOWN_' ORDER BY id LIMIT 1", Long.class);
        Integer providers = jdbc.queryForObject("SELECT count(*) FROM clinlims.provider WHERE person_id = ?",
                Integer.class, personId);
        if (providers == 0) {
            jdbc.update("INSERT INTO clinlims.provider (id, person_id, active, lastupdated)"
                    + " VALUES (97302, ?, false, NOW())", personId);
        }
        Integer patients = jdbc.queryForObject("SELECT count(*) FROM clinlims.patient WHERE person_id = ?",
                Integer.class, personId);
        if (patients == 0) {
            jdbc.update("INSERT INTO clinlims.patient (id, person_id, lastupdated) VALUES (97303, ?, NOW())", personId);
        }
        org.openelisglobal.patient.util.PatientUtil.invalidateUnknownPatients();
    }

    private void seedSampleType(long id, String description) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                description);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, 'H', ?, 'true', ?, ?, NOW())",
                id, description, "H" + id % 1000, id, id);
    }

    private void insertJunction(long sampleTypeId, long testId) {
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id, is_panel)"
                + " VALUES (nextval('sample_type_test_seq'), ?, ?, 'false')", sampleTypeId, testId);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // the reviewer-choice case persists real records; unwind them first
        jdbc.update("DELETE FROM clinlims.result WHERE analysis_id IN (SELECT a.id FROM clinlims.analysis a"
                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?)", ACCESSION);
        jdbc.update("DELETE FROM clinlims.analysis WHERE sampitem_id IN (SELECT si.id FROM clinlims.sample_item si"
                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?)", ACCESSION);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE samp_id IN"
                + " (SELECT id FROM clinlims.sample WHERE accession_number = ?)", ACCESSION);
        jdbc.update("DELETE FROM clinlims.sample_human WHERE samp_id IN"
                + " (SELECT id FROM clinlims.sample WHERE accession_number = ?)", ACCESSION);
        jdbc.update("DELETE FROM clinlims.observation_history WHERE sample_id IN"
                + " (SELECT id FROM clinlims.sample WHERE accession_number = ?)", ACCESSION);
        jdbc.update("DELETE FROM clinlims.sample WHERE accession_number = ?", ACCESSION);
        jdbc.update("DELETE FROM clinlims.analyzer_results WHERE accession_number = ?", ACCESSION);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", MULTI_TYPE_TEST);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", MULTI_TYPE_TEST);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id IN (?, ?)", TYPE_A, TYPE_B);
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (?, ?)", TYPE_A, TYPE_B);
        typeOfSampleService.clearCache();
    }

    private AnalyzerResultItem acceptedItem() {
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setId(stagedRowId);
        item.setAccessionNumber(ACCESSION);
        item.setTestId(String.valueOf(MULTI_TYPE_TEST));
        item.setTestName("HoldIT 1145");
        item.setResult("42");
        item.setSampleGroupingNumber(1);
        item.setIsAccepted(true);
        return item;
    }

    @org.junit.Test
    public void acceptingAmbiguousRowWithoutChoice_holdsItAwaitingSpecimen() {
        acceptService.acceptAndPersist(List.of(acceptedItem()), "1");

        assertEquals("the staged row must survive the accept", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric", Integer.class, stagedRowId));
        assertEquals("the hold reason is stamped on the staged row", AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN,
                jdbc.queryForObject("SELECT import_issue_reason FROM clinlims.analyzer_results WHERE id = ?::numeric",
                        String.class, stagedRowId));
        assertEquals("nothing was persisted for the accession", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample WHERE accession_number = ?", Integer.class, ACCESSION));
    }

    @org.junit.Test
    public void reviewerChoice_removesTheHold() {
        AnalyzerResultItem item = acceptedItem();
        item.setTypeOfSampleId(String.valueOf(TYPE_B));
        acceptService.acceptAndPersist(List.of(item), "1");

        assertNull("a chosen sample type must not trigger the hold", jdbc.queryForObject(
                "SELECT max(import_issue_reason) FROM clinlims.analyzer_results" + " WHERE accession_number = ?",
                String.class, ACCESSION));
        String persistedType = jdbc.queryForObject(
                "SELECT si.typeosamp_id::text FROM clinlims.sample s JOIN clinlims.sample_item si ON si.samp_id = s.id"
                        + " WHERE s.accession_number = ?",
                String.class, ACCESSION);
        assertEquals("the sample item carries the reviewer's chosen type, not the primary link", String.valueOf(TYPE_B),
                persistedType);
    }
}
