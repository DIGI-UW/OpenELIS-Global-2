package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * An analyzer result lands on the tube and analysis the instrument's specimen
 * ID names, replaces a held result only visibly, and never takes the first of
 * several matching analyses.
 */
@Transactional
public class AnalyzerResultsPlacementIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TYPE = 97301L;
    private static final long TEST = 97302L;
    private static final long ANALYZER = 97303L;
    private static final String ACCESSION = "PLACE1B0001";

    @Autowired
    private AnalyzerResultsAcceptService acceptService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private javax.sql.DataSource dataSource;
    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/status_service.xml");
        statusService.refreshCache();
        org.openelisglobal.patient.util.PatientUtil.invalidateUnknownPatients();
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, 'Place 1b', NOW())",
                TYPE);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, local_abbrev, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, 'Place 1b', 'H', 'P1B', 'true', ?, ?, NOW())",
                TYPE, TYPE, TYPE);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'Place 1b test', 'Place 1b test', 'Y', ?, 'CLINICAL', true, NOW())",
                TEST, UUID.randomUUID().toString());
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id, is_panel)"
                + " VALUES (nextval('sample_type_test_seq'), ?, ?, 'false')", TYPE, TEST);
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, last_updated)"
                + " VALUES (?, 'Place 1b analyzer', true, NOW())", ANALYZER);
        typeOfSampleService.clearCache();
    }

    @AfterTransaction
    public void resetCaches() {
        org.openelisglobal.patient.util.PatientUtil.invalidateUnknownPatients();
        typeOfSampleService.clearCache();
    }

    @Test
    public void tubeIdPlacesTheResultOnThatTubesAnalysis() {
        acceptNew("1");
        long firstTube = onlyTube();
        long secondTube = addTube("PLACE1B0001-2", AnalysisStatus.NotStarted);
        jdbc.update("UPDATE clinlims.sample_item SET external_id = ? WHERE id = ?", ACCESSION + "-1", firstTube);

        accept(stage(ACCESSION + "-2", ACCESSION, "2"), "2");

        assertEquals("the second tube's analysis took the result", "2", resultOnTube(secondTube));
        assertEquals("the first tube's analysis was not touched", "1", resultOnTube(firstTube));
    }

    @Test
    public void aResultForAnAnalysisThatHoldsOneReplacesItAndRaisesTheRevision() {
        acceptNew("1");
        long tube = onlyTube();
        assertEquals("0", revisionOnTube(tube));

        accept(stage(ACCESSION, ACCESSION, "2"), "2");

        assertEquals("the one analysis holds the new result", "2", resultOnTube(tube));
        assertEquals("a replaced result is a corrected one", "2", revisionOnTube(tube));
    }

    @Test
    public void sameTestOnTwoTubesHoldsTheRowAwaitingPlacement() {
        acceptNew("1");
        long first = onlyTube();
        jdbc.update("UPDATE clinlims.analysis SET status_id = ? WHERE sampitem_id = ?",
                Long.valueOf(statusService.getStatusID(AnalysisStatus.NotStarted)), first);
        addTube(ACCESSION + "-2", AnalysisStatus.NotStarted);
        String stagedId = stage(ACCESSION, ACCESSION, "2");

        accept(stagedId, "2");

        assertEquals("the row stays staged", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric", Integer.class, stagedId));
        assertEquals("flagged awaiting placement", AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT,
                jdbc.queryForObject("SELECT import_issue_reason FROM clinlims.analyzer_results WHERE id = ?::numeric",
                        String.class, stagedId));
        assertEquals("neither analysis got the result", Integer.valueOf(0),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.result r JOIN clinlims.analysis a ON a.id = r.analysis_id"
                                + " WHERE r.value = '2' AND a.test_id = ?",
                        Integer.class, TEST));
    }

    @Test
    public void choosingAnAnalysisPlacesTheHeldRowOnIt() {
        acceptNew("1");
        long first = onlyTube();
        jdbc.update("UPDATE clinlims.analysis SET status_id = ? WHERE sampitem_id = ?",
                Long.valueOf(statusService.getStatusID(AnalysisStatus.NotStarted)), first);
        long second = addTube(ACCESSION + "-2", AnalysisStatus.NotStarted);
        String stagedId = stage(ACCESSION, ACCESSION, "2");
        accept(stagedId, "2");
        String analysisOnSecond = String.valueOf(
                jdbc.queryForObject("SELECT id FROM clinlims.analysis WHERE sampitem_id = ?", Long.class, second));

        AnalyzerResultItem item = acceptedItem(stagedId, "2");
        item.setChosenAnalysisId(analysisOnSecond);
        acceptService.acceptAndPersist(List.of(item), "1");
        entityManager.flush();

        assertEquals("the chosen tube's analysis took the result", "2", resultOnTube(second));
        assertEquals("the staged row is resolved", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE id = ?::numeric", Integer.class, stagedId));
    }

    @Test
    public void aMistypedSpecimenIdIsPlacedOnTheRightOrderWithItsJustification() {
        acceptNew("1");
        long tube = onlyTube();
        jdbc.update("UPDATE clinlims.analysis SET status_id = ? WHERE sampitem_id = ?",
                Long.valueOf(statusService.getStatusID(AnalysisStatus.NotStarted)), tube);
        String stagedId = stage("PLACE1B0OO1", "PLACE1B0OO1", "2");

        AnalyzerResultItem item = acceptedItem(stagedId, "2");
        item.setAccessionNumber("PLACE1B0OO1");
        item.setRedirectAccession(ACCESSION);
        item.setRedirectReason("Label misread by the instrument");
        acceptService.acceptAndPersist(List.of(item), "1");
        entityManager.flush();

        assertEquals("the right order's analysis took the result", "2", resultOnTube(tube));
        assertEquals("the mistyped ID created no order", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample WHERE accession_number = 'PLACE1B0OO1'", Integer.class));
        assertEquals("the justification is on the result it explains", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.note n JOIN clinlims.result r ON r.id = n.reference_id"
                        + " JOIN clinlims.analysis a ON a.id = r.analysis_id"
                        + " WHERE a.sampitem_id = ? AND n.note_type = 'I'"
                        + " AND n.text LIKE '%Label misread by the instrument%' AND n.text LIKE '%PLACE1B0OO1%'",
                Integer.class, tube));
    }

    @Test
    public void aRedirectWithoutAJustificationHoldsTheRow() {
        acceptNew("1");
        long tube = onlyTube();
        String stagedId = stage("PLACE1B0OO1", "PLACE1B0OO1", "2");

        AnalyzerResultItem item = acceptedItem(stagedId, "2");
        item.setAccessionNumber("PLACE1B0OO1");
        item.setRedirectAccession(ACCESSION);
        acceptService.acceptAndPersist(List.of(item), "1");
        entityManager.flush();

        assertEquals("the row stays staged", AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT,
                jdbc.queryForObject("SELECT import_issue_reason FROM clinlims.analyzer_results WHERE id = ?::numeric",
                        String.class, stagedId));
        assertEquals("the held result is untouched", "1", resultOnTube(tube));
    }

    @Test
    public void aRedirectToAnOrderThatDoesNotExistHoldsTheRow() {
        String stagedId = stage("PLACE1B0OO1", "PLACE1B0OO1", "2");

        AnalyzerResultItem item = acceptedItem(stagedId, "2");
        item.setAccessionNumber("PLACE1B0OO1");
        item.setRedirectAccession("NO-SUCH-ORDER");
        item.setRedirectReason("Typed it wrong too");
        acceptService.acceptAndPersist(List.of(item), "1");
        entityManager.flush();

        assertEquals("the row stays staged", AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT,
                jdbc.queryForObject("SELECT import_issue_reason FROM clinlims.analyzer_results WHERE id = ?::numeric",
                        String.class, stagedId));
        assertEquals("no order was created for either ID", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample WHERE accession_number IN ('PLACE1B0OO1', 'NO-SUCH-ORDER')",
                Integer.class));
    }

    @Test
    public void aMismatchedInstrumentPatientHoldsTheRowUntilTheReviewerSaysWhy() {
        acceptNew("1");
        long tube = onlyTube();
        awaitResultOn(tube);
        registerPatientOnOrder("EXT-REAL-1");
        String stagedId = stage(ACCESSION, ACCESSION, "2", "SOMEONE-ELSE", "Roe, Rick");

        accept(stagedId, "2");

        assertEquals("the row stays staged", AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT,
                jdbc.queryForObject("SELECT import_issue_reason FROM clinlims.analyzer_results WHERE id = ?::numeric",
                        String.class, stagedId));
        assertEquals("the held result is untouched", "1", resultOnTube(tube));
    }

    @Test
    public void aMismatchedInstrumentPatientIsSavedOnceTheReviewerExplainsIt() {
        acceptNew("1");
        long tube = onlyTube();
        awaitResultOn(tube);
        registerPatientOnOrder("EXT-REAL-1");
        String stagedId = stage(ACCESSION, ACCESSION, "2", "SOMEONE-ELSE", "Roe, Rick");

        AnalyzerResultItem item = acceptedItem(stagedId, "2");
        item.setNote("Instrument operator keyed the wrong patient");
        acceptService.acceptAndPersist(List.of(item), "1");
        entityManager.flush();

        assertEquals("the result is saved", "2", resultOnTube(tube));
    }

    @Test
    public void aMatchingInstrumentPatientIsSavedWithoutANote() {
        acceptNew("1");
        long tube = onlyTube();
        awaitResultOn(tube);
        registerPatientOnOrder("EXT-REAL-1");

        accept(stage(ACCESSION, ACCESSION, "2", "ext-real-1", "Doe, Jane"), "2");

        assertEquals("the result is saved", "2", resultOnTube(tube));
    }

    private void awaitResultOn(long tube) {
        jdbc.update("UPDATE clinlims.analysis SET status_id = ? WHERE sampitem_id = ?",
                Long.valueOf(statusService.getStatusID(AnalysisStatus.NotStarted)), tube);
    }

    private void registerPatientOnOrder(String externalId) {
        long person = jdbc.queryForObject("SELECT nextval('person_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.person (id, last_name, first_name, lastupdated)"
                + " VALUES (?, 'Doe', 'Jane', NOW())", person);
        long patient = jdbc.queryForObject("SELECT nextval('patient_seq')", Long.class);
        jdbc.update("INSERT INTO clinlims.patient (id, person_id, gender, birth_date, external_id, lastupdated)"
                + " VALUES (?, ?, 'F', '1990-01-01', ?, NOW())", patient, person, externalId);
        jdbc.update("UPDATE clinlims.sample_human SET patient_id = ? WHERE samp_id ="
                + " (SELECT id FROM clinlims.sample WHERE accession_number = ?)", patient, ACCESSION);
        entityManager.clear();
    }

    private void acceptNew(String value) {
        accept(stage(ACCESSION, ACCESSION, value), value);
    }

    private void accept(String stagedId, String value) {
        acceptService.acceptAndPersist(List.of(acceptedItem(stagedId, value)), "1");
        entityManager.flush();
    }

    private AnalyzerResultItem acceptedItem(String stagedId, String value) {
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setId(stagedId);
        item.setResult(value);
        item.setAccessionNumber(ACCESSION);
        item.setTestId(String.valueOf(TEST));
        item.setTestName("Place 1b test");
        item.setSampleGroupingNumber(1);
        item.setIsAccepted(true);
        return item;
    }

    private String stage(String instrumentId, String accession, String result) {
        return stage(instrumentId, accession, result, null, null);
    }

    private String stage(String instrumentId, String accession, String result, String patientId, String patientName) {
        String id = String.valueOf(jdbc.queryForObject("SELECT nextval('analyzer_results_seq')", Long.class));
        jdbc.update(
                "INSERT INTO clinlims.analyzer_results (id, analyzer_id, accession_number,"
                        + " instrument_specimen_id, instrument_patient_id, instrument_patient_name, test_name, result,"
                        + " iscontrol, test_id, last_updated)"
                        + " VALUES (?::numeric, ?, ?, ?, ?, ?, 'Place 1b test', ?, false, ?, NOW())",
                id, ANALYZER, accession, instrumentId, patientId, patientName, result, TEST);
        return id;
    }

    private long onlyTube() {
        return jdbc.queryForObject("SELECT si.id FROM clinlims.sample_item si JOIN clinlims.sample s"
                + " ON s.id = si.samp_id WHERE s.accession_number = ?", Long.class, ACCESSION);
    }

    private long addTube(String externalId, AnalysisStatus status) {
        long sampleId = jdbc.queryForObject("SELECT id FROM clinlims.sample WHERE accession_number = ?", Long.class,
                ACCESSION);
        long tube = jdbc.queryForObject("SELECT nextval('sample_item_seq')", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.sample_item (id, samp_id, sort_order, typeosamp_id, external_id,"
                        + " status_id, lastupdated) SELECT ?, samp_id, 2, typeosamp_id, ?, status_id, NOW()"
                        + " FROM clinlims.sample_item WHERE samp_id = ? ORDER BY id LIMIT 1",
                tube, externalId, sampleId);
        long analysis = jdbc.queryForObject("SELECT nextval('analysis_seq')", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_id, status_id, analysis_type, revision,"
                        + " is_reportable, lastupdated) VALUES (?, ?, ?, ?, 'MANUAL', '0', 'Y', NOW())",
                analysis, tube, TEST, Long.valueOf(statusService.getStatusID(status)));
        return tube;
    }

    private String resultOnTube(long tube) {
        List<String> values = jdbc.queryForList("SELECT r.value FROM clinlims.result r JOIN clinlims.analysis a"
                + " ON a.id = r.analysis_id WHERE a.sampitem_id = ?", String.class, tube);
        return values.isEmpty() ? null : values.get(0);
    }

    private String revisionOnTube(long tube) {
        return jdbc.queryForObject("SELECT revision FROM clinlims.analysis WHERE sampitem_id = ?", String.class, tube);
    }
}
