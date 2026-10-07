package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1145 P1b (FR-8) — the analyzer-review awaiting-specimen hold: accepting a
 * result whose test runs on several sample types, with no reviewer-chosen type
 * and no existing sample item pinning one, must NOT first-match — the staged
 * row stays in review flagged {@code awaiting_specimen} and nothing is
 * persisted for its accession.
 */
@Transactional
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
    private javax.sql.DataSource dataSource;
    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbc;
    private String stagedRowId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/status_service.xml");
        org.openelisglobal.patient.util.PatientUtil.invalidateUnknownPatients();
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
        typeOfSampleService.clearCache();
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

    @AfterTransaction
    public void resetUnknownPatientCache() {
        org.openelisglobal.patient.util.PatientUtil.invalidateUnknownPatients();
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
    public void acceptingMixedGroupKeepsUnresolvedObservationStaged() {
        String heldId = String.valueOf(jdbc.queryForObject("SELECT nextval('analyzer_results_seq')", Long.class));
        jdbc.update(
                "INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                        + " iscontrol, test_id, read_only, import_issue_reason, last_updated)"
                        + " VALUES (?::numeric, ?, ?, 'Held result', 'RAW', false, ?, true, ?, NOW())",
                heldId, ANALYZER_ID, ACCESSION, MULTI_TYPE_TEST, AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);

        String heldControlId = String
                .valueOf(jdbc.queryForObject("SELECT nextval('analyzer_results_seq')", Long.class));
        jdbc.update(
                "INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                        + " iscontrol, test_id, read_only, import_issue_reason, last_updated)"
                        + " VALUES (?::numeric, ?, ?, 'Held control', 'RAW', true, ?, true, ?, NOW())",
                heldControlId, ANALYZER_ID, ACCESSION, MULTI_TYPE_TEST,
                AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);

        AnalyzerResultItem accepted = acceptedItem();
        accepted.setTypeOfSampleId(String.valueOf(TYPE_B));
        AnalyzerResultItem unresolved = acceptedItem();
        unresolved.setId(heldId);
        unresolved.setReadOnly(true);
        unresolved.setIsAccepted(false);
        AnalyzerResultItem heldControl = acceptedItem();
        heldControl.setId(heldControlId);
        heldControl.setSampleGroupingNumber(2);
        heldControl.setIsControl(true);
        heldControl.setReadOnly(true);
        heldControl.setIsAccepted(false);
        acceptService.acceptAndPersist(List.of(unresolved, accepted, heldControl), "1");

        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric", Integer.class, heldId));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric", Integer.class, heldControlId));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric", Integer.class, stagedRowId));
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.result r JOIN clinlims.analysis a ON r.analysis_id = a.id"
                                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?",
                        Integer.class, ACCESSION));
    }

    @org.junit.Test
    public void acceptingGroupWithInactiveTestSavesSiblingAndKeepsInactiveRowStaged() {
        long inactiveTest = 97002L;
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'Inactive 1145', 'Inactive 1145', 'N', ?, 'CLINICAL', true, NOW())",
                inactiveTest, UUID.randomUUID().toString());
        insertJunction(TYPE_B, inactiveTest);
        String inactiveRowId = String
                .valueOf(jdbc.queryForObject("SELECT nextval('analyzer_results_seq')", Long.class));
        jdbc.update("INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                + " iscontrol, test_id, last_updated) VALUES (?::numeric, ?, ?, 'Inactive 1145', '7', false,"
                + " ?, NOW())", inactiveRowId, ANALYZER_ID, ACCESSION, inactiveTest);

        AnalyzerResultItem accepted = acceptedItem();
        accepted.setTypeOfSampleId(String.valueOf(TYPE_B));
        AnalyzerResultItem inactive = acceptedItem();
        inactive.setId(inactiveRowId);
        inactive.setTestId(String.valueOf(inactiveTest));
        inactive.setTestName("Inactive 1145");
        inactive.setResult("7");
        inactive.setTypeOfSampleId(String.valueOf(TYPE_B));
        acceptService.acceptAndPersist(List.of(accepted, inactive), "1");

        assertEquals("the accepted sibling leaves staging", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric", Integer.class, stagedRowId));
        assertEquals("the result for the inactive test stays staged", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric", Integer.class, inactiveRowId));
        assertEquals("only the sibling's result is persisted", Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.result r JOIN clinlims.analysis a ON r.analysis_id = a.id"
                                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?",
                        Integer.class, ACCESSION));
    }

    /**
     * Stages a result and accepts it without a specimen so it is held awaiting one.
     */
    private String stageHeldAwaitingSpecimen(String accession, long testId) {
        String id = String.valueOf(jdbc.queryForObject("SELECT nextval('analyzer_results_seq')", Long.class));
        jdbc.update("INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                + " iscontrol, test_id, last_updated) VALUES (?::numeric, ?, ?, 'HoldIT 1145', '42', false,"
                + " ?, NOW())", id, ANALYZER_ID, accession, testId);
        AnalyzerResultItem item = acceptedItem();
        item.setId(id);
        item.setAccessionNumber(accession);
        item.setTestId(String.valueOf(testId));
        acceptService.acceptAndPersist(List.of(item), "1");
        assertEquals(AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN, jdbc.queryForObject(
                "SELECT import_issue_reason FROM clinlims.analyzer_results WHERE id = ?::numeric", String.class, id));
        return id;
    }

    private String specimenTypeOfResult(String accession, long testId) {
        return jdbc.queryForObject("SELECT si.typeosamp_id::text FROM clinlims.analysis a"
                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id JOIN clinlims.sample s ON si.samp_id = s.id"
                + " WHERE s.accession_number = ? AND a.test_id = ?", String.class, accession, testId);
    }

    @org.junit.Test
    public void chosenSpecimenIsUsedForANewTestOnAnExistingOrder() {
        String existingOrder = "123456789";
        String heldId = stageHeldAwaitingSpecimen(existingOrder, MULTI_TYPE_TEST);

        AnalyzerResultItem chosen = acceptedItem();
        chosen.setId(heldId);
        chosen.setAccessionNumber(existingOrder);
        chosen.setTypeOfSampleId(String.valueOf(TYPE_B));
        acceptService.acceptAndPersist(List.of(chosen), "1");

        assertEquals("the reviewer's specimen, not the test's first sample type", String.valueOf(TYPE_B),
                specimenTypeOfResult(existingOrder, MULTI_TYPE_TEST));
    }

    @org.junit.Test
    public void releasingAHeldResultKeepsItsStagedAccession() {
        acceptService.acceptAndPersist(List.of(acceptedItem()), "1");
        AnalyzerResultItem forged = acceptedItem();
        forged.setTypeOfSampleId(String.valueOf(TYPE_B));
        forged.setAccessionNumber("FORGED1145");
        acceptService.acceptAndPersist(List.of(forged), "1");

        assertEquals("no order is created under the submitted accession", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample WHERE accession_number = 'FORGED1145'", Integer.class));
        assertEquals("the result is saved on the staged accession", String.valueOf(TYPE_B),
                specimenTypeOfResult(ACCESSION, MULTI_TYPE_TEST));
    }

    private long seedSingleTypeTest(long testId, long sampleTypeId) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, 'CLINICAL', true, NOW())",
                testId, "Single " + testId, "Single " + testId, UUID.randomUUID().toString());
        insertJunction(sampleTypeId, testId);
        return testId;
    }

    private AnalyzerResultItem stagedSibling(long testId) {
        String id = String.valueOf(jdbc.queryForObject("SELECT nextval('analyzer_results_seq')", Long.class));
        jdbc.update("INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                + " iscontrol, test_id, last_updated) VALUES (?::numeric, ?, ?, 'Single', '5', false, ?, NOW())", id,
                ANALYZER_ID, ACCESSION, testId);
        AnalyzerResultItem item = acceptedItem();
        item.setId(id);
        item.setTestId(String.valueOf(testId));
        item.setTestName("Single");
        item.setResult("5");
        return item;
    }

    @org.junit.Test
    public void aSpecimenChoiceASiblingTestCannotUseKeepsTheNewOrderHeld() {
        AnalyzerResultItem onlyTypeA = stagedSibling(seedSingleTypeTest(97003L, TYPE_A));
        AnalyzerResultItem chosenB = acceptedItem();
        chosenB.setTypeOfSampleId(String.valueOf(TYPE_B));

        acceptService.acceptAndPersist(List.of(onlyTypeA, chosenB), "1");

        assertEquals("one new specimen cannot be both types, so nothing is saved", Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.sample WHERE accession_number = ?", Integer.class,
                        ACCESSION));
        assertEquals("both results stay held for a specimen decision", Integer.valueOf(2),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.analyzer_results WHERE accession_number = ?"
                                + " AND import_issue_reason = ?",
                        Integer.class, ACCESSION, AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN));
    }

    @org.junit.Test
    public void eachTestOnOneSpecimenKeepsItsOwnReviewDecision() {
        AnalyzerResultItem accepted = acceptedItem();
        accepted.setTypeOfSampleId(String.valueOf(TYPE_B));
        AnalyzerResultItem retested = stagedSibling(seedSingleTypeTest(97006L, TYPE_B));
        retested.setIsAccepted(false);
        retested.setIsRejected(true);

        acceptService.acceptAndPersist(List.of(accepted, retested), "1");

        assertEquals("42", resultValueOf(ACCESSION, MULTI_TYPE_TEST));
        assertEquals("the sibling is sent for retest, not accepted with the first test", "XXXX",
                resultValueOf(ACCESSION, 97006L));
    }

    private String resultValueOf(String accession, long testId) {
        return jdbc.queryForObject(
                "SELECT r.value FROM clinlims.result r JOIN clinlims.analysis a"
                        + " ON r.analysis_id = a.id JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                        + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ? AND a.test_id = ?",
                String.class, accession, testId);
    }

    @org.junit.Test
    public void aSpecimenChoiceEverySiblingCanUseIsSavedForTheNewOrder() {
        AnalyzerResultItem onlyTypeB = stagedSibling(seedSingleTypeTest(97004L, TYPE_B));
        AnalyzerResultItem chosenB = acceptedItem();
        chosenB.setTypeOfSampleId(String.valueOf(TYPE_B));

        acceptService.acceptAndPersist(List.of(onlyTypeB, chosenB), "1");

        assertEquals(String.valueOf(TYPE_B), specimenTypeOfResult(ACCESSION, MULTI_TYPE_TEST));
        assertEquals(String.valueOf(TYPE_B), specimenTypeOfResult(ACCESSION, 97004L));
    }

    private AnalyzerResultItem stagedRow(String accession, long testId) {
        String id = String.valueOf(jdbc.queryForObject("SELECT nextval('analyzer_results_seq')", Long.class));
        jdbc.update(
                "INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result,"
                        + " iscontrol, test_id, last_updated) VALUES (?::numeric, ?, ?, 'Row', '42', false, ?, NOW())",
                id, ANALYZER_ID, accession, testId);
        AnalyzerResultItem item = acceptedItem();
        item.setId(id);
        item.setAccessionNumber(accession);
        item.setTestId(String.valueOf(testId));
        return item;
    }

    @org.junit.Test
    public void releasingAHeldResultKeepsItsStagedComponent() {
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, is_primary, is_active,"
                + " lastupdated) VALUES ('c-a-1145', ?, 'A', 'A', true, 'Y', NOW()),"
                + " ('c-b-1145', ?, 'B', 'B', false, 'Y', NOW())", MULTI_TYPE_TEST, MULTI_TYPE_TEST);
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, is_active, sort_order,"
                + " component_id, lastupdated) VALUES (97301, ?, 'N', '', true, 1, 'c-a-1145', NOW()),"
                + " (97302, ?, 'N', '', true, 2, 'c-b-1145', NOW())", MULTI_TYPE_TEST, MULTI_TYPE_TEST);
        jdbc.update("UPDATE clinlims.analyzer_results SET component_id = 'c-a-1145' WHERE id = ?::numeric",
                stagedRowId);
        acceptService.acceptAndPersist(List.of(acceptedItem()), "1");

        AnalyzerResultItem forged = acceptedItem();
        forged.setTypeOfSampleId(String.valueOf(TYPE_B));
        forged.setComponentId("c-b-1145");
        acceptService.acceptAndPersist(List.of(forged), "1");

        assertEquals("the result binds to the staged component's test result", "97301",
                jdbc.queryForObject(
                        "SELECT r.test_result_id::text FROM clinlims.result r"
                                + " JOIN clinlims.analysis a ON r.analysis_id = a.id"
                                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?",
                        String.class, ACCESSION));
    }

    @org.junit.Test
    public void aNumberOnAComponentIsSavedWithTheComponentsDigits() {
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, is_primary, is_active,"
                + " significant_digits, lastupdated) VALUES ('c-main-1145', ?, 'PRIMARY', 'Viral load', true, 'Y', 0,"
                + " NOW()), ('c-log-1145', ?, 'LOG', 'Log viral load', false, 'Y', 2, NOW())", MULTI_TYPE_TEST,
                MULTI_TYPE_TEST);
        jdbc.update(
                "INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, is_active, sort_order,"
                        + " significant_digits, component_id, lastupdated) VALUES (97311, ?, 'N', '', true, 1, 0,"
                        + " 'c-main-1145', NOW()), (97312, ?, 'N', '', true, 2, 2, 'c-log-1145', NOW())",
                MULTI_TYPE_TEST, MULTI_TYPE_TEST);
        jdbc.update("UPDATE clinlims.analyzer_results SET component_id = 'c-log-1145', result = '3.00',"
                + " test_result_type = 'N' WHERE id = ?::numeric", stagedRowId);
        AnalyzerResultItem item = acceptedItem();
        item.setResult("3.00");
        acceptService.acceptAndPersist(List.of(item), "1");

        AnalyzerResultItem released = acceptedItem();
        released.setResult("3.00");
        released.setTypeOfSampleId(String.valueOf(TYPE_B));
        acceptService.acceptAndPersist(List.of(released), "1");

        assertEquals("the log is kept to its component's two decimals", "2",
                jdbc.queryForObject(
                        "SELECT r.significant_digits::text FROM clinlims.result r"
                                + " JOIN clinlims.analysis a ON r.analysis_id = a.id"
                                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?",
                        String.class, ACCESSION));
    }

    @org.junit.Test
    public void releasingAHeldResultKeepsItsStagedCompletionDate() {
        jdbc.update("UPDATE clinlims.analyzer_results SET complete_date = '2026-09-01 10:00:00' WHERE id = ?::numeric",
                stagedRowId);
        acceptService.acceptAndPersist(List.of(acceptedItem()), "1");

        AnalyzerResultItem forged = acceptedItem();
        forged.setTypeOfSampleId(String.valueOf(TYPE_B));
        forged.setCompleteDate("01/01/2000");
        acceptService.acceptAndPersist(List.of(forged), "1");

        assertEquals("the analysis carries the analyzer's completion date", "2026-09-01",
                jdbc.queryForObject(
                        "SELECT to_char(a.completed_date, 'YYYY-MM-DD') FROM clinlims.analysis a"
                                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?",
                        String.class, ACCESSION));
    }

    @org.junit.Test
    public void aReleasedAnalyzerResultIsRecordedAsAnAutomaticAnalysis() {
        acceptService.acceptAndPersist(List.of(acceptedItem()), "1");

        AnalyzerResultItem forged = acceptedItem();
        forged.setTypeOfSampleId(String.valueOf(TYPE_B));
        forged.setManual(true);
        acceptService.acceptAndPersist(List.of(forged), "1");

        assertEquals("analyzer provenance is kept in the analysis type", "AUTO",
                jdbc.queryForObject(
                        "SELECT a.analysis_type FROM clinlims.analysis a"
                                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?",
                        String.class, ACCESSION));
    }

    @org.junit.Test
    public void differentSpecimenChoicesForNewTestsOnAnExistingOrderStayHeld() {
        String existingOrder = "123456789";
        long secondMultiType = 97005L;
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'HoldIT2 1145', 'HoldIT2 1145', 'Y', ?, 'CLINICAL', true, NOW())",
                secondMultiType, UUID.randomUUID().toString());
        insertJunction(TYPE_A, secondMultiType);
        insertJunction(TYPE_B, secondMultiType);
        AnalyzerResultItem chosenB = stagedRow(existingOrder, MULTI_TYPE_TEST);
        chosenB.setTypeOfSampleId(String.valueOf(TYPE_B));
        AnalyzerResultItem chosenA = stagedRow(existingOrder, secondMultiType);
        chosenA.setTypeOfSampleId(String.valueOf(TYPE_A));

        acceptService.acceptAndPersist(List.of(chosenB, chosenA), "1");

        assertEquals("the new analyses of one grouping share one specimen, so neither is saved", Integer.valueOf(0),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.analysis a JOIN clinlims.sample_item si"
                                + " ON a.sampitem_id = si.id JOIN clinlims.sample s ON si.samp_id = s.id"
                                + " WHERE s.accession_number = ? AND a.test_id IN (?, ?)",
                        Integer.class, existingOrder, MULTI_TYPE_TEST, secondMultiType));
        assertEquals("both results wait for a specimen decision", Integer.valueOf(2),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.analyzer_results WHERE accession_number = ?"
                                + " AND import_issue_reason = ?",
                        Integer.class, existingOrder, AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN));
    }

    @org.junit.Test
    public void aNewTestKeepsItsChosenSpecimenBesideAnExistingAnalysis() {
        String existingOrder = "123456789";
        long testWithExistingAnalysis = 8000L;
        // The dataset's analysis has no version stamp; accepting a result updates it.
        jdbc.update("UPDATE clinlims.analysis SET lastupdated = NOW() WHERE id = 9000");
        AnalyzerResultItem existing = stagedRow(existingOrder, testWithExistingAnalysis);
        AnalyzerResultItem chosenB = stagedRow(existingOrder, MULTI_TYPE_TEST);
        chosenB.setTypeOfSampleId(String.valueOf(TYPE_B));

        acceptService.acceptAndPersist(List.of(existing, chosenB), "1");

        assertEquals("the new test's specimen, not the existing analysis's", String.valueOf(TYPE_B),
                specimenTypeOfResult(existingOrder, MULTI_TYPE_TEST));
    }

    @org.junit.Test
    public void aGroupingWithOnlyInactiveTestsCreatesNoOrder() {
        long inactiveTest = 97006L;
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'Inactive2 1145', 'Inactive2 1145', 'N', ?, 'CLINICAL', true, NOW())",
                inactiveTest, UUID.randomUUID().toString());
        insertJunction(TYPE_B, inactiveTest);
        AnalyzerResultItem inactive = stagedRow(ACCESSION, inactiveTest);

        acceptService.acceptAndPersist(List.of(inactive), "1");

        assertEquals("no order is created when no result is saved", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample WHERE accession_number = ?", Integer.class, ACCESSION));
        assertEquals("the inactive test's result stays staged", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric",
                        Integer.class, inactive.getId()));
    }

    private Integer heldAwaitingSpecimen(AnalyzerResultItem first, AnalyzerResultItem second) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id IN (?::numeric, ?::numeric)"
                        + " AND import_issue_reason = ?",
                Integer.class, first.getId(), second.getId(), AnalyzerResults.IMPORT_ISSUE_AWAITING_SPECIMEN);
    }

    private long seedInactiveTest(long testId, long sampleTypeId) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, ?, ?, 'N', ?, 'CLINICAL', true, NOW())",
                testId, "Inactive " + testId, "Inactive " + testId, UUID.randomUUID().toString());
        insertJunction(sampleTypeId, testId);
        return testId;
    }

    @org.junit.Test
    public void newTestsWithNoSpecimenTypeInCommonStayHeldOnANewOrder() {
        AnalyzerResultItem onlyTypeA = stagedSibling(seedSingleTypeTest(97010L, TYPE_A));
        AnalyzerResultItem onlyTypeB = stagedSibling(seedSingleTypeTest(97011L, TYPE_B));

        acceptService.acceptAndPersist(List.of(onlyTypeA, onlyTypeB), "1");

        assertEquals("one new specimen cannot be both types, so nothing is saved", Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.sample WHERE accession_number = ?", Integer.class,
                        ACCESSION));
        assertEquals("both results stay held", Integer.valueOf(2), heldAwaitingSpecimen(onlyTypeA, onlyTypeB));
    }

    @org.junit.Test
    public void newTestsWithNoSpecimenTypeInCommonStayHeldOnAnExistingOrder() {
        String existingOrder = "123456789";
        AnalyzerResultItem onlyTypeA = stagedRow(existingOrder, seedSingleTypeTest(97012L, TYPE_A));
        AnalyzerResultItem onlyTypeB = stagedRow(existingOrder, seedSingleTypeTest(97013L, TYPE_B));

        acceptService.acceptAndPersist(List.of(onlyTypeA, onlyTypeB), "1");

        assertEquals("neither result is added to the order", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analysis WHERE test_id IN (?, ?)", Integer.class, 97012L, 97013L));
        assertEquals("both results stay held", Integer.valueOf(2), heldAwaitingSpecimen(onlyTypeA, onlyTypeB));
    }

    @org.junit.Test
    public void newTestsOnAnExistingOrderShareASpecimenEveryOneCanUse() {
        String existingOrder = "123456789";
        long orderSpecimenType = 8500L;
        long pinnedTest = 97014L;
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'Pinned 1145', 'Pinned 1145', 'Y', ?, 'CLINICAL', true, NOW())",
                pinnedTest, UUID.randomUUID().toString());
        // The order's existing specimen is one of this test's types, so it needs no
        // choice.
        insertJunction(orderSpecimenType, pinnedTest);
        insertJunction(TYPE_B, pinnedTest);
        AnalyzerResultItem onlyTypeB = stagedRow(existingOrder, seedSingleTypeTest(97015L, TYPE_B));
        AnalyzerResultItem pinned = stagedRow(existingOrder, pinnedTest);

        acceptService.acceptAndPersist(List.of(onlyTypeB, pinned), "1");

        assertEquals("the only type the single-type test can use", String.valueOf(TYPE_B),
                specimenTypeOfResult(existingOrder, 97015L));
        assertEquals("the same specimen for its sibling", String.valueOf(TYPE_B),
                specimenTypeOfResult(existingOrder, pinnedTest));
    }

    @org.junit.Test
    public void anInactiveTestOnAnotherSpecimenTypeDoesNotHoldItsSibling() {
        AnalyzerResultItem inactive = stagedSibling(seedInactiveTest(97016L, TYPE_A));
        AnalyzerResultItem onlyTypeB = stagedSibling(seedSingleTypeTest(97017L, TYPE_B));

        acceptService.acceptAndPersist(List.of(inactive, onlyTypeB), "1");

        assertEquals("the active test's result is saved", String.valueOf(TYPE_B),
                specimenTypeOfResult(ACCESSION, 97017L));
        assertEquals("the inactive test's result stays staged", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric",
                        Integer.class, inactive.getId()));
    }

    @org.junit.Test
    public void anExistingOrderIsUnchangedWhenEveryResultForItIsSkipped() {
        String existingOrder = "123456789";
        AnalyzerResultItem inactive = stagedRow(existingOrder, seedInactiveTest(97018L, TYPE_B));

        acceptService.acceptAndPersist(List.of(inactive), "1");
        // The test's transaction never commits; flushing writes what a commit would.
        entityManager.flush();

        assertEquals("the order keeps its entry date", "2024-06-03 00:00:00",
                jdbc.queryForObject("SELECT entered_date::text FROM clinlims.sample WHERE accession_number = ?",
                        String.class, existingOrder));
        assertEquals("the order keeps its status", "101", jdbc.queryForObject(
                "SELECT status_id::text FROM clinlims.sample WHERE accession_number = ?", String.class, existingOrder));
    }

    @org.junit.Test
    public void reviewerChoice_removesTheHold() {
        acceptService.acceptAndPersist(List.of(acceptedItem()), "1");
        AnalyzerResultItem item = acceptedItem();
        item.setReadOnly(true); // A stale client flag must not override the valid specimen choice.
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
