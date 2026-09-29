package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

/**
 * The Routine CSV export pivots results into one column per test of the
 * selected lab unit. Two active tests can share a display name, and Java and
 * Postgres sort test descriptions differently, so each result column must be
 * tied to its test, not to its name or its sort position.
 *
 * <p>
 * The fixture commits its rows because the builder reads on a session of its
 * own, and removes them afterwards. Every id is in a range reserved for this
 * class.
 */
public class CSVRoutineColumnBuilderResultColumnsTest extends BaseWebContextSensitiveTest {

    private static final int UNIT = 990100;
    private static final int SAMPLE_ITEM = 990100;
    private static final int FIRST_ID = 990100;
    private static final int LAST_ID = 990199;
    private static final Date ENTERED = Date.valueOf("2001-02-03");

    @Before
    public void insertUnitAndSample() {
        insertLocalization(UNIT, "CSV unit");
        jdbcTemplate.update("INSERT INTO test_section (id, name, description, name_localization_id, is_active)"
                + " VALUES (?, 'CSV unit', 'CSV unit', ?, 'Y')", UNIT, UNIT);
        jdbcTemplate.update("INSERT INTO sample (id, accession_number, entered_date, received_date)"
                + " VALUES (?, 'CSV-0001', ?, ?)", SAMPLE_ITEM, ENTERED, ENTERED);
        jdbcTemplate.update("INSERT INTO sample_item (id, samp_id, sort_order, status_id) VALUES (?, ?, 1, 1)",
                SAMPLE_ITEM, SAMPLE_ITEM);
    }

    @After
    public void deleteFixture() {
        for (String table : new String[] { "result", "test_result", "analysis", "sample_item", "sample", "test",
                "test_section", "localization_value", "localization" }) {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE id BETWEEN ? AND ?", FIRST_ID, LAST_ID);
        }
    }

    @Test
    public void testsSharingADisplayName_exportEachResultUnderItsOwnColumn() throws Exception {
        // A legacy test and its replacement, both shown to users as "Hematocrit".
        insertTestWithResult(990101, "Hematocrit", "CSV Hémotocrite", "41");
        insertTestWithResult(990102, "Hematocrit", "CSV Hematocrit", "42");

        // Columns follow the builder's description order: "CSV Hematocrit" before
        // "CSV Hémotocrite".
        assertEquals(new ExportedRow("Hematocrit,Hematocrit", "42,41"), exportUnit());
    }

    @Test
    public void descriptionsSortedDifferentlyByJavaAndPostgres_keepEachResultUnderItsTest() throws Exception {
        // Java orders "CSV HCT" before "CSV Hb" (uppercase first); Postgres en_US
        // orders "CSV Hb" first. Descriptions are unique across the catalog, hence
        // the prefix.
        insertTestWithResult(990111, "Hemoglobin", "CSV Hb", "13.5");
        insertTestWithResult(990112, "Hematocrit", "CSV HCT", "40");

        assertEquals(new ExportedRow("Hematocrit,Hemoglobin", "40,13.5"), exportUnit());
    }

    @Test
    public void resultWithoutATestResultLink_isStillExported() throws Exception {
        // A free-text result on a test with no configured result options is saved
        // with no test_result row behind it.
        insertTestWithResult(990121, "Hematocrit", "CSV Hematocrit", "41");
        insertTestWithUnlinkedResult(990122, "Blood film", "CSV Blood film", "normal");

        assertEquals(new ExportedRow("Blood film,Hematocrit", "normal,41"), exportUnit());
    }

    private ExportedRow exportUnit() throws Exception {
        CSVRoutineColumnBuilder builder = new CSVRoutineColumnBuilder(null) {
            {
                selectedLabUnit = String.valueOf(UNIT);
                defineAllTestsAndResults();
                addAllResultsColumns();
            }

            @Override
            public void makeSQL() {
                query = new StringBuilder("SELECT result.* FROM (SELECT 1) AS one");
                appendResultCrosstab(ENTERED, ENTERED);
            }
        };
        try {
            builder.buildDataSource();
            assertTrue("the sample item has a row", builder.next());
            ExportedRow row = new ExportedRow(builder.getColumnNamesLine().trim(), builder.nextLine().trim());
            assertFalse("only the one sample item has results", builder.next());
            return row;
        } finally {
            builder.closeResultSet();
        }
    }

    private void insertTestWithResult(int id, String name, String description, String value) {
        insertTestAndAnalysis(id, name, description);
        jdbcTemplate.update("INSERT INTO test_result (id, test_id, tst_rslt_type) VALUES (?, ?, 'N')", id, id);
        jdbcTemplate.update("INSERT INTO result (id, analysis_id, test_result_id, value) VALUES (?, ?, ?, ?)", id, id,
                id, value);
    }

    private void insertTestWithUnlinkedResult(int id, String name, String description, String value) {
        insertTestAndAnalysis(id, name, description);
        jdbcTemplate.update("INSERT INTO result (id, analysis_id, value) VALUES (?, ?, ?)", id, id, value);
    }

    private void insertTestAndAnalysis(int id, String name, String description) {
        insertLocalization(id, name);
        jdbcTemplate.update(
                "INSERT INTO test (id, name, description, guid, test_section_id, is_active,"
                        + " name_localization_id) VALUES (?, ?, ?, ?, ?, 'Y', ?)",
                id, name, description, "csv-" + id, UNIT, id);
        jdbcTemplate.update("INSERT INTO analysis (id, sampitem_id, test_id, analysis_type) VALUES (?, ?, ?, 'MANUAL')",
                id, SAMPLE_ITEM, id);
    }

    private void insertLocalization(int id, String english) {
        jdbcTemplate.update("INSERT INTO localization (id, description) VALUES (?, ?)", id, english);
        jdbcTemplate.update(
                "INSERT INTO localization_value (id, localization_id, locale, value) VALUES (?, ?, 'en', ?)", id, id,
                english);
    }

    private record ExportedRow(String header, String values) {
    }
}
