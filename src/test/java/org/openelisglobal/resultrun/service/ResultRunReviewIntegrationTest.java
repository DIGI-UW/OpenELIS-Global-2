package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultrun.form.ResultRunChip;
import org.openelisglobal.resultrun.form.ResultRunChipCounts;
import org.openelisglobal.resultrun.form.ResultRunRow;
import org.openelisglobal.resultrun.form.ResultRunRowCategory;
import org.openelisglobal.resultrun.form.ResultRunRowsPage;
import org.openelisglobal.resultrun.form.ResultRunSelection;
import org.openelisglobal.resultrun.form.ResultRunSelectionSummary;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1200 slice 2: a run's rows are judged against the patient's result limit,
 * counted per chip over the whole run, listed in review order a page at a time,
 * and a selection is summarised from the run, the chip and its exclusions.
 */
@Transactional
public class ResultRunReviewIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String UNIT = "97750";
    private static final String TEST = "97751";
    private static final String ANALYZER = "97770";

    @Autowired
    private ResultRunService resultRunService;
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TestService testService;
    @Autowired
    private javax.sql.DataSource dataSource;
    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbc;
    private Patient patient;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?::numeric, ?, NOW())",
                UNIT, "Review unit");
        jdbc.update("INSERT INTO clinlims.test_section (id, name, description, name_localization_id, is_active,"
                + " domain, lastupdated) VALUES (?::numeric, 'Review unit', 'Review unit', ?::numeric, 'Y',"
                + " 'CLINICAL', NOW())", UNIT, UNIT);
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable,"
                + " test_section_id, lastupdated) VALUES (?::numeric, 'Review test', 'Review test', 'Y', ?,"
                + " 'CLINICAL', true, ?::numeric, NOW())", TEST, UUID.randomUUID().toString(), UNIT);
        jdbc.update(
                "INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, low_normal, high_normal,"
                        + " low_valid, high_valid, low_critical, high_critical, lastupdated) VALUES"
                        + " (nextval('clinlims.result_limits_seq'), ?::numeric, 4, 10, 20, 0, 1000, 5, 50, NOW())",
                TEST);
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, test_unit_ids, last_updated)"
                + " VALUES (?::numeric, 'Review analyzer 1200', true, ?, NOW())", ANALYZER, UNIT);

        patient = fixtures.createPatient("RVW");
    }

    private String newSampleOfPatient(TypeOfSample specimen) {
        SampleItem item = newSampleItemOfPatient();
        item.setTypeOfSample(specimen);
        sampleItemService.update(item);
        fixtures.createAnalysis(item, testService.get(TEST));
        entityManager.flush();
        return item.getSample().getAccessionNumber();
    }

    private String newSampleOfPatient() {
        return newSampleItemOfPatient().getSample().getAccessionNumber();
    }

    private SampleItem newSampleItemOfPatient() {
        SampleItem item = fixtures.createSampleWithSampleItem("RVW");
        jdbc.update(
                "INSERT INTO clinlims.sample_human (id, samp_id, patient_id, lastupdated) VALUES"
                        + " (nextval('clinlims.sample_human_seq'), ?::numeric, ?::numeric, NOW())",
                item.getSample().getId(), patient.getId());
        return item;
    }

    @Test
    public void rowsAreJudgedAgainstThePatientsLimitAndCountedOverTheWholeRun() {
        ResultRun run = open("m-counts",
                List.of(row("15"), row("25"), row("60"), row("2000"), control("12"), withIssue(row("14"))));

        ResultRunChipCounts counts = resultRunService.getHeader(run.getId(), viewer()).orElseThrow().chipCounts();

        assertEquals(6, counts.all());
        assertEquals(1, counts.normal());
        assertEquals(1, counts.abnormal());
        assertEquals(1, counts.critical());
        assertEquals("the value outside the valid range and the import issue", 2, counts.exceptions());
        assertEquals(4, counts.needsReview());
        assertEquals(0, counts.held());
    }

    @Test
    public void aChipListsItsRowsInReviewOrderAPageAtATime() {
        List<AnalyzerResults> rows = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            rows.add(row(i % 3 == 0 ? "25" : "15"));
        }
        rows.add(row("60"));
        ResultRun run = open("m-page", rows);

        ResultRunRowsPage all = resultRunService.getRows(run.getId(), viewer(), ResultRunChip.ALL, 1, 25).orElseThrow();
        assertEquals(31, all.totalRows());
        assertEquals(ResultRunRowCategory.CRITICAL, all.rows().get(0).category());
        assertEquals(ResultRunRowCategory.ABNORMAL, all.rows().get(1).category());

        ResultRunRowsPage abnormal = resultRunService.getRows(run.getId(), viewer(), ResultRunChip.ABNORMAL, 1, 25)
                .orElseThrow();
        assertEquals(10, abnormal.totalRows());
        assertEquals(List.of("ABNORMAL"), abnormal.rows().stream().map(ResultRunRow::flag).distinct().toList());

        ResultRunRowsPage normalPage2 = resultRunService.getRows(run.getId(), viewer(), ResultRunChip.NORMAL, 2, 25)
                .orElseThrow();
        assertEquals(20, normalPage2.totalRows());
        assertEquals(1, normalPage2.page());
        assertEquals("a page past the end shows the last page", 20, normalPage2.rows().size());
    }

    @Test
    public void aSelectionIsSummarisedFromTheChipLessItsExclusions() {
        AnalyzerResults issue = withIssue(row("14"));
        AnalyzerResults critical = row("60");
        ResultRun run = open("m-select", List.of(row("15"), row("16"), row("25"), critical, issue, control("12")));

        ResultRunSelectionSummary all = resultRunService.summarizeSelection(run.getId(), viewer(),
                new ResultRunSelection("ALL", true, List.of(), List.of(critical.getId()))).orElseThrow();
        assertEquals(5, all.selected());
        assertEquals(0, all.critical());
        assertEquals(1, all.abnormal());
        assertEquals(2, all.normal());
        assertEquals(Set.of(ResultRunRowCategory.EXCEPTION, ResultRunRowCategory.CONTROL),
                Set.copyOf(all.skipped().stream().map(ResultRunSelectionSummary.SkippedRow::reason).toList()));

        ResultRunSelectionSummary picked = resultRunService
                .summarizeSelection(run.getId(), viewer(),
                        new ResultRunSelection("NORMAL", false, List.of(critical.getId(), issue.getId()), List.of()))
                .orElseThrow();
        assertEquals(2, picked.selected());
        assertEquals(1, picked.critical());
        assertEquals(List.of(issue.getId()),
                picked.skipped().stream().map(ResultRunSelectionSummary.SkippedRow::id).toList());
        ResultRunSelectionSummary normalPlusCritical = resultRunService.summarizeSelection(run.getId(), viewer(),
                new ResultRunSelection("NORMAL", true, List.of(critical.getId()), List.of())).orElseThrow();
        assertEquals("both normal rows and the critical row picked outside the chip", 3, normalPlusCritical.selected());
        assertEquals(1, normalPlusCritical.critical());
        assertEquals(2, normalPlusCritical.normal());
    }

    @Test
    public void aRangeScopedToTheSpecimenWinsAsOnTheWorklist() {
        TypeOfSample plasma = fixtures.getOrCreateActiveSampleType();
        jdbc.update("INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, low_normal, high_normal,"
                + " low_valid, high_valid, low_critical, high_critical, sample_type_id, lastupdated) VALUES"
                + " (nextval('clinlims.result_limits_seq'), ?::numeric, 4, 1, 5, 0, 1000, 0, 12, ?::numeric, NOW())",
                TEST, plasma.getId());
        AnalyzerResults onPlasma = row("15");
        onPlasma.setAccessionNumber(newSampleOfPatient(plasma));
        AnalyzerResults withoutAnalysis = row("15");
        ResultRun run = open("m-specimen", List.of(onPlasma, withoutAnalysis));

        List<ResultRunRow> rows = resultRunService.getRows(run.getId(), viewer(), ResultRunChip.ALL, 1, 25)
                .orElseThrow().rows();

        ResultRunRow plasmaRow = rows.stream().filter(r -> r.id().equals(onPlasma.getId())).findFirst().orElseThrow();
        assertEquals("15 is above the Plasma range's critical high of 12", ResultRunRowCategory.CRITICAL,
                plasmaRow.category());
        ResultRunRow sharedRow = rows.stream().filter(r -> r.id().equals(withoutAnalysis.getId())).findFirst()
                .orElseThrow();
        assertEquals("15 is inside the shared range of 10 to 20", ResultRunRowCategory.NORMAL, sharedRow.category());
    }

    @Test
    public void everyPatientRowOfAHeldRunIsHeld() {
        ResultRun run = open("m-held", List.of(row("15"), row("60"), withIssue(row("14"))));
        run.setState(ResultRunState.HELD);
        resultRunService.update(run);
        entityManager.flush();

        ResultRunChipCounts counts = resultRunService.getHeader(run.getId(), viewer()).orElseThrow().chipCounts();

        assertEquals(2, counts.held());
        assertEquals(1, counts.exceptions());
        assertEquals(0, counts.critical());
    }

    /**
     * A run row is accepted into its worklist row with the value the import
     * resolved: the dictionary choice, while the instrument's text stays on view. A
     * main value staged without a component names the test's primary component,
     * which is the row the worklist shows it on.
     */
    @Test
    public void aRowNamesTheChoiceItIsAcceptedAsAndTheMainValueItsPrimaryPart() {
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, is_active,"
                + " lastupdated, is_primary) VALUES ('rvw-main', ?::numeric, 'MAIN', 'Main', 0, 'Y', NOW(), true),"
                + " ('rvw-call', ?::numeric, 'CALL', 'Call', 1, 'Y', NOW(), false)", TEST, TEST);
        AnalyzerResults positive = row("9101");
        positive.setRawResultValue("POS");
        positive.setResultType("D");
        AnalyzerResults call = row("12");
        call.setComponentId("rvw-call");
        ResultRun run = open("m-entry", List.of(positive, call));

        List<ResultRunRow> rows = resultRunService.getRows(run.getId(), viewer(), ResultRunChip.ALL, 1, 25)
                .orElseThrow().rows();

        ResultRunRow main = rows.stream().filter(r -> r.id().equals(positive.getId())).findFirst().orElseThrow();
        assertEquals("what the instrument sent", "POS", main.value());
        assertEquals("the dictionary choice the import resolved", "9101", main.stagedResult());
        assertEquals("rvw-main", main.componentId());
        ResultRunRow part = rows.stream().filter(r -> r.id().equals(call.getId())).findFirst().orElseThrow();
        assertEquals("rvw-call", part.componentId());
    }

    /**
     * A test with one part shows one worklist row that names no part, so a value of
     * that test names none either, whether or not the import gave it the part.
     */
    @Test
    public void aValueOfATestWithOnePartNamesNoPartLikeItsWorklistRow() {
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, is_active,"
                + " lastupdated, is_primary) VALUES ('rvw-only', ?::numeric, 'ONLY', 'Only', 0, 'Y', NOW(), true)",
                TEST);
        AnalyzerResults named = row("15");
        named.setComponentId("rvw-only");
        AnalyzerResults unnamed = row("16");
        ResultRun run = open("m-one-part", List.of(named, unnamed));

        List<ResultRunRow> rows = resultRunService.getRows(run.getId(), viewer(), ResultRunChip.ALL, 1, 25)
                .orElseThrow().rows();

        assertEquals(List.of(), rows.stream().map(ResultRunRow::componentId).filter(Objects::nonNull).toList());
    }

    private AnalyzerResults row(String value) {
        AnalyzerResults row = new AnalyzerResults();
        row.setAnalyzerId(ANALYZER);
        row.setAccessionNumber(newSampleOfPatient());
        row.setTestId(TEST);
        row.setTestName("Review test");
        row.setResult(value);
        row.setRawResultValue(value);
        row.setResultType("N");
        row.setIsControl(false);
        row.setInstrumentOperator("op-7");
        row.setRawSubIdentity("");
        return row;
    }

    private AnalyzerResults control(String value) {
        AnalyzerResults row = row(value);
        row.setIsControl(true);
        return row;
    }

    private static AnalyzerResults withIssue(AnalyzerResults row) {
        row.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        return row;
    }

    private ResultRun open(String messageId, List<AnalyzerResults> rows) {
        Analyzer analyzer = analyzerService.getWithMapping(ANALYZER).orElseThrow();
        ResultRun run = resultRunService.openAnalyzerRun(analyzer, "conn-review-1200", messageId, rows).orElseThrow();
        analyzerResultsService.insertAnalyzerResults(rows, "1");
        entityManager.flush();
        return run;
    }

    private static RunViewer viewer() {
        return new RunViewer(Set.of(TEST), Set.of(UNIT));
    }
}
