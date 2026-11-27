package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.PriorResultsService;
import org.openelisglobal.program.valueholder.AnatomicPathologyBench;
import org.openelisglobal.program.valueholder.PriorResultRow;
import org.openelisglobal.program.valueholder.PriorResults;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A case view lists the patient's earlier pathology, immunohistochemistry and
 * cytology cases, newest first, each with the conclusion it was signed out with
 * and nothing a case in progress has not yet reported.
 *
 * <p>
 * The dictionary entries, sample types and sample items are seeded here rather
 * than in the dataset, because naming dictionary, type_of_sample, localization
 * or sample_item in a dataset truncates them, and through localization also
 * test, test_section and panel, for every later test class in the same Surefire
 * fork.
 */
public class PriorResultsServiceTest extends BaseWebContextSensitiveTest {

    static final String DATASET = "testdata/prior-results.xml";

    /** The fixture holds no system user, so no lab unit restricts this one. */
    static final String VIEWER = "1";

    private static final int CATEGORY = 9420;
    private static final String CATEGORY_NAME = "Prior results test conclusions";
    private static final int ADENOCARCINOMA = 9001;
    private static final int OLD_TERM = 9002;
    private static final int HSIL = 9003;

    private static final int TISSUE_BIOPSY = 9421;
    private static final int FLUID = 9422;
    private static final int FIRST_ITEM = 9421;
    private static final int LAST_ITEM = 9440;

    /** Order 104 has no items; every other order of the fixture has one. */
    private static final int[] ORDERS_WITH_ONE_ITEM = { 102, 103, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 201,
            301 };

    private static final String NEGATIVE = "Negative For Intraepithelial Lesion or Malignancy";
    private static final String UNSATISFACTORY = "Unsatisfactory for evaluation";

    @Autowired
    private PriorResultsService priorResultsService;

    @Autowired
    private PathologySampleService pathologySampleService;

    private String rowsSetting;

    @Before
    public void init() throws Exception {
        removeSeededRows(jdbcTemplate);
        executeDataSetWithStateManagement(DATASET);
        seedRows(jdbcTemplate);
        rowsSetting = ConfigurationProperties.getInstance().getPropertyValue(Property.PATHOLOGY_PRIOR_RESULTS_ROWS);
        rows("5");
    }

    @After
    public void cleanUp() {
        rows(rowsSetting == null ? "5" : rowsSetting);
        removeSeededRows(jdbcTemplate);
    }

    // ---- which cases are listed ----

    @Test
    public void listsEarlierCasesOfAllThreeBenches_newestFirst() {
        PriorResults results = everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15);

        assertEquals("newest request first, then order, then bench, then case",
                List.of("CYTOLOGY 34", "PATHOLOGY 14", "IMMUNOHISTOCHEMISTRY 22", "CYTOLOGY 33", "CYTOLOGY 32",
                        "CYTOLOGY 31", "PATHOLOGY 13", "PATHOLOGY 12", "PATHOLOGY 17", "PATHOLOGY 20", "PATHOLOGY 11",
                        "IMMUNOHISTOCHEMISTRY 21"),
                keys(results));
        assertEquals(12, results.total());
        assertEquals("PR-0109 is on screen; PR-0101 is the oldest", "PR-0101", results.rows().get(10).labNumber());
        assertEquals("2026-01-10", results.rows().get(10).requestDate());
    }

    @Test
    public void leavesOutTheCaseOnScreen_laterCases_otherPatients_andOtherPrograms() {
        List<String> listed = keys(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15));

        assertFalse("the case on screen is not its own prior case", listed.contains("PATHOLOGY 15"));
        assertFalse("a case entered later is not prior", listed.contains("PATHOLOGY 16"));
        assertFalse("another patient's case is not listed", listed.contains("PATHOLOGY 18"));
        assertFalse("a case of an order with no patient is not listed", listed.contains("PATHOLOGY 19"));
        assertTrue("a plain programme row is never listed", listed.stream().noneMatch(key -> key.endsWith(" 41")));
    }

    @Test
    public void anIhcCaseListsThePathologyCaseThatReferredIt() {
        assertEquals(List.of("PATHOLOGY 11"), keys(everyPriorOf(AnatomicPathologyBench.IMMUNOHISTOCHEMISTRY, 21)));
    }

    @Test
    public void aPathologyCaseListsItsOwnReferredIhcCase() {
        assertEquals("the referred case shares the order; the earlier programme row is not anatomic pathology",
                List.of("IMMUNOHISTOCHEMISTRY 21"), keys(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 11)));
    }

    @Test
    public void aCytologyCaseListsEarlierCasesOfTheOtherBenches() {
        assertEquals(List.of("CYTOLOGY 31", "PATHOLOGY 13", "PATHOLOGY 12", "PATHOLOGY 17", "PATHOLOGY 20",
                "PATHOLOGY 11", "IMMUNOHISTOCHEMISTRY 21"), keys(everyPriorOf(AnatomicPathologyBench.CYTOLOGY, 32)));
    }

    @Test
    public void twoCasesEnteredTheSameDay_theSmallerOrderNumberIsThePriorOne() {
        assertTrue("order 103 is smaller than order 104, so 13 came first",
                keys(everyPriorOf(AnatomicPathologyBench.CYTOLOGY, 31)).contains("PATHOLOGY 13"));
        assertFalse("and 31 is not prior to 13",
                keys(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 13)).contains("CYTOLOGY 31"));
    }

    // ---- the conclusion cell ----

    @Test
    public void aSignedOutPathologyCase_showsItsCodesInStoredOrder_retiredNamed_textLeftOut() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "PATHOLOGY", 11);

        assertEquals("REPORTED", row.conclusionState());
        assertEquals("Adenocarcinoma, Old term", row.conclusion());
    }

    @Test
    public void aSignedOutPathologyCaseWithNoCode_showsItsText() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "PATHOLOGY", 12);

        assertEquals("REPORTED", row.conclusionState());
        assertEquals("Chronic gastritis", row.conclusion());
    }

    @Test
    public void aPathologyCaseInProgress_isNotReported_andItsDraftIsNotSent() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "PATHOLOGY", 13);

        assertEquals("NOT_REPORTED", row.conclusionState());
        assertNull("the draft text stays on the case", row.conclusion());
    }

    @Test
    public void aReopenedCase_readsNotReported_untilSignedOutAgain() {
        // Signed out as the record reads it: at Completed, entered forward.
        jdbcTemplate.update("INSERT INTO clinlims.pathology_stage_event (id, pathology_sample_id, stage, entered_at,"
                + " entered_by, direction, last_updated) VALUES (9441, 11, 'COMPLETED', now(), 1, 'FORWARD', now())");
        try {
            pathologySampleService.reopen(11, PathologyStatus.COMPLETED, "Amended diagnosis", VIEWER);

            PriorResultRow reopened = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "PATHOLOGY", 11);
            assertEquals("a reopened case reads as not reported", "NOT_REPORTED", reopened.conclusionState());
            assertNull("and sends no conclusion while it is amended", reopened.conclusion());

            jdbcTemplate.update("UPDATE clinlims.pathology_sample SET status = 'COMPLETED' WHERE id = 11");
            assertEquals("signed out again, it is reported", "REPORTED",
                    rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "PATHOLOGY", 11).conclusionState());
        } finally {
            PathologyCaseRows.clearStageHistory(jdbcTemplate, 11);
        }
    }

    @Test
    public void aSignedOutPathologyCaseWithABlankTextAndNoCode_hasNoneRecorded() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "PATHOLOGY", 14);

        assertEquals("NONE_RECORDED", row.conclusionState());
        assertNull(row.conclusion());
    }

    @Test
    public void aSignedOutPathologyCaseWhoseOnlyCodeIsUnknown_showsItsText() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "PATHOLOGY", 17);

        assertEquals("REPORTED", row.conclusionState());
        assertEquals("unknown code kept", row.conclusion());
    }

    @Test
    public void aSignedOutNegativeCytologyCase_readsAsItsReportDoes() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "CYTOLOGY", 31);

        assertEquals("REPORTED", row.conclusionState());
        assertEquals(NEGATIVE, row.conclusion());
    }

    @Test
    public void aSignedOutCytologyCaseWithFindings_readsAsItsReportDoes() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "CYTOLOGY", 32);

        assertEquals("REPORTED", row.conclusionState());
        assertEquals("HSIL, text note", row.conclusion());
    }

    @Test
    public void aSignedOutCytologyCaseWithNothingFilled_hasNoneRecorded() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "CYTOLOGY", 34);

        assertEquals("NONE_RECORDED", row.conclusionState());
        assertNull(row.conclusion());
    }

    @Test
    public void aCytologyCaseInProgress_isNotReported_thoughItsDiagnosisDefaultsToNegative() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "CYTOLOGY", 33);

        assertEquals("NOT_REPORTED", row.conclusionState());
        assertNull("an untouched diagnosis is not read as negative", row.conclusion());
    }

    @Test
    public void aSignedOutUnsatisfactoryCytologyCase_leadsWithItsAdequacy() {
        PriorResults results = everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 23);
        assertEquals("patient 3's own cases only", List.of("CYTOLOGY 38", "CYTOLOGY 37", "CYTOLOGY 36", "CYTOLOGY 35"),
                keys(results));

        PriorResultRow row = rowOf(results, "CYTOLOGY", 35);

        assertEquals("REPORTED", row.conclusionState());
        assertEquals("the report prints the adequacy line beside the default negative",
                UNSATISFACTORY + ", " + NEGATIVE, row.conclusion());
    }

    @Test
    public void aSignedOutUnsatisfactoryCytologyCaseWithNoDiagnosis_isReportedAsItsAdequacy() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 23), "CYTOLOGY", 36);

        assertEquals("REPORTED", row.conclusionState());
        assertEquals(UNSATISFACTORY, row.conclusion());
    }

    @Test
    public void aSignedOutSatisfactoryCytologyCase_readsAsItsDiagnosisAlone() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 23), "CYTOLOGY", 38);

        assertEquals("REPORTED", row.conclusionState());
        assertEquals(NEGATIVE, row.conclusion());
    }

    @Test
    public void aSignedOutCytologyCaseWithNoDiagnosisRow_hasNoneRecorded() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 23), "CYTOLOGY", 37);

        assertEquals("NONE_RECORDED", row.conclusionState());
        assertNull(row.conclusion());
    }

    @Test
    public void aSignedOutIhcCase_saysItsConclusionIsInTheReport() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "IMMUNOHISTOCHEMISTRY", 22);

        assertEquals("IN_REPORT", row.conclusionState());
        assertNull(row.conclusion());
    }

    @Test
    public void anIhcCaseInProgress_isNotReported() {
        PriorResultRow row = rowOf(everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15), "IMMUNOHISTOCHEMISTRY", 21);

        assertEquals("NOT_REPORTED", row.conclusionState());
        assertNull(row.conclusion());
    }

    // ---- specimen ----

    @Test
    public void specimenIsTheOrdersSampleTypesInItemOrder_andBlankWithoutItems() {
        PriorResults results = everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15);

        assertEquals("Tissue biopsy, Fluid", rowOf(results, "PATHOLOGY", 11).specimen());
        assertEquals("the referred case reads the same order", "Tissue biopsy, Fluid",
                rowOf(results, "IMMUNOHISTOCHEMISTRY", 21).specimen());
        assertEquals("Tissue biopsy", rowOf(results, "PATHOLOGY", 12).specimen());
        assertEquals("order 104 has no items", "", rowOf(results, "CYTOLOGY", 31).specimen());
    }

    // ---- how many rows ----

    @Test
    public void capsTheRowsAtTheSetting_andCountsEveryPriorCase() {
        PriorResults five = priorResultsService.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, 15, false, VIEWER);
        assertEquals(List.of("CYTOLOGY 34", "PATHOLOGY 14", "IMMUNOHISTOCHEMISTRY 22", "CYTOLOGY 33", "CYTOLOGY 32"),
                keys(five));
        assertEquals("the total counts the rows left out", 12, five.total());

        rows("3");
        PriorResults three = priorResultsService.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, 15, false, VIEWER);

        assertEquals(List.of("CYTOLOGY 34", "PATHOLOGY 14", "IMMUNOHISTOCHEMISTRY 22"), keys(three));
        assertEquals(12, three.total());
    }

    @Test
    public void allReturnsEveryRow() {
        rows("3");

        PriorResults results = everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 15);

        assertEquals(12, results.rows().size());
        assertEquals(12, results.total());
    }

    @Test
    public void aSettingThatIsNotANumberFrom1To50_meansFive() {
        for (String setting : List.of("abc", "0", "-1", "51", "2.5", "")) {
            rows(setting);

            PriorResults results = priorResultsService.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, 15, false,
                    VIEWER);

            assertEquals("the setting \"" + setting + "\" means five rows", 5, results.rows().size());
            assertEquals(12, results.total());
        }
        rows("50");
        assertEquals("50 is allowed", 12,
                priorResultsService.priorResultsOf(AnatomicPathologyBench.PATHOLOGY, 15, false, VIEWER).rows().size());
    }

    // ---- edges ----

    @Test
    public void anOrderWithNoPatient_hasNoPriorResults() {
        PriorResults results = everyPriorOf(AnatomicPathologyBench.PATHOLOGY, 19);

        assertTrue(results.rows().isEmpty());
        assertEquals(0, results.total());
        assertFalse("the empty answer still says whether the viewer is restricted", results.restricted());
    }

    @Test
    public void anUnknownCaseOfEachBench_isNull_andACaseIdOfAnotherBenchIsNotFound() {
        assertNotNull(priorResultsService.caseOf(AnatomicPathologyBench.PATHOLOGY, 11));
        for (AnatomicPathologyBench bench : AnatomicPathologyBench.values()) {
            assertNull(bench + " has no case 999", priorResultsService.caseOf(bench, 999));
            assertEquals(0, priorResultsService.priorResultsOf(bench, 999, true, VIEWER).total());
        }
        assertNull("11 is a pathology case, not a cytology one",
                priorResultsService.caseOf(AnatomicPathologyBench.CYTOLOGY, 11));
        assertNull("31 is a cytology case, not a pathology one",
                priorResultsService.caseOf(AnatomicPathologyBench.PATHOLOGY, 31));
    }

    // helpers

    private PriorResults everyPriorOf(AnatomicPathologyBench bench, int caseId) {
        return priorResultsService.priorResultsOf(bench, caseId, true, VIEWER);
    }

    private static List<String> keys(PriorResults results) {
        return results.rows().stream().map(row -> row.bench() + " " + row.caseId()).collect(Collectors.toList());
    }

    private static PriorResultRow rowOf(PriorResults results, String bench, int caseId) {
        return results.rows().stream().filter(row -> bench.equals(row.bench()) && row.caseId() == caseId).findFirst()
                .orElseThrow(() -> new AssertionError(bench + " " + caseId + " is not listed"));
    }

    private static void rows(String setting) {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_PRIOR_RESULTS_ROWS, setting);
    }

    /** The dictionary entries, sample types and items the fixture's cases name. */
    static void seedRows(JdbcTemplate jdbcTemplate) {
        DictionaryRows.insertCategory(jdbcTemplate, CATEGORY, CATEGORY_NAME, CATEGORY_NAME, "PriorRes");
        DictionaryRows.insertEntry(jdbcTemplate, ADENOCARCINOMA, CATEGORY, "Adenocarcinoma", "PRAdeno", true);
        DictionaryRows.insertEntry(jdbcTemplate, OLD_TERM, CATEGORY, "Old term", "PROld", false);
        DictionaryRows.insertEntry(jdbcTemplate, HSIL, CATEGORY, "HSIL", "PRHsil", true);
        insertSampleType(jdbcTemplate, TISSUE_BIOPSY, "Tissue biopsy", "PRTisBx");
        insertSampleType(jdbcTemplate, FLUID, "Fluid", "PRFluid");
        int item = FIRST_ITEM;
        insertItem(jdbcTemplate, item++, 101, 1, TISSUE_BIOPSY);
        insertItem(jdbcTemplate, item++, 101, 2, FLUID);
        for (int order : ORDERS_WITH_ONE_ITEM) {
            insertItem(jdbcTemplate, item++, order, 1, TISSUE_BIOPSY);
        }
    }

    static void removeSeededRows(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM clinlims.sample_item WHERE id BETWEEN ? AND ?", FIRST_ITEM, LAST_ITEM);
        jdbcTemplate.update("DELETE FROM clinlims.type_of_sample WHERE id IN (?, ?)", TISSUE_BIOPSY, FLUID);
        jdbcTemplate.update("DELETE FROM clinlims.localization_value WHERE id IN (?, ?)", TISSUE_BIOPSY, FLUID);
        jdbcTemplate.update("DELETE FROM clinlims.localization WHERE id IN (?, ?)", TISSUE_BIOPSY, FLUID);
        DictionaryRows.removeEntries(jdbcTemplate, ADENOCARCINOMA, OLD_TERM, HSIL);
        DictionaryRows.removeCategory(jdbcTemplate, CATEGORY);
    }

    private static void insertSampleType(JdbcTemplate jdbcTemplate, int id, String name, String abbreviation) {
        jdbcTemplate.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, now())", id,
                "PriorResultsSampleType" + id);
        jdbcTemplate.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value,"
                + " last_updated) VALUES (?, ?, 'en', ?, now())", id, id, name);
        jdbcTemplate.update("INSERT INTO clinlims.type_of_sample (id, description, domain, lastupdated,"
                + " local_abbrev, is_active, sort_order, name_localization_id)"
                + " VALUES (?, ?, 'H', now(), ?, true, 1, ?)", id, name, abbreviation, id);
    }

    // status_id is a plain column on sample_item with no foreign key, and nothing
    // the panel reads looks at it.
    private static void insertItem(JdbcTemplate jdbcTemplate, int id, int order, int sortOrder, int sampleType) {
        jdbcTemplate.update("INSERT INTO clinlims.sample_item (id, sort_order, samp_id, typeosamp_id, status_id,"
                + " lastupdated) VALUES (?, ?, ?, ?, 1, now())", id, sortOrder, order, sampleType);
    }
}
