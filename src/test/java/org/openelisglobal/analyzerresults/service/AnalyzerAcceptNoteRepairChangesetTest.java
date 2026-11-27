package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

/**
 * Changeset 135 moves the note a reviewer wrote on accept from the id of the
 * accepted result, where the code before this release saved it, to that
 * result's analysis. The test runs the changeset's own SQL against rows shaped
 * like the ones that code wrote, including the analysis whose id happens to
 * equal the result's, which is where such a note used to show up.
 */
@Transactional
public class AnalyzerAcceptNoteRepairChangesetTest extends BaseWebContextSensitiveTest {

    private static final String CHANGESET = "liquibase/3.5.x.x/135-analyzer-accept-notes-on-their-analysis.xml";
    private static final String ACCEPT_SUBJECT = "Analyzer Result Note";

    private static final long SAMPLE = 98300L;
    private static final long ANALYSIS_OF_RESULT = 98301L;
    private static final long RESULT = 98302L;
    private static final long ANALYSIS_WITH_RESULTS_ID = RESULT;
    private static final long MISFILED_NOTE = 98311L;
    private static final long NOTE_ON_THE_OTHER_ANALYSIS = 98312L;
    private static final long NOTE_ON_NO_RESULT = 98313L;
    private static final long NO_SUCH_RESULT = 98399L;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private long analysisTable;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        analysisTable = jdbc.queryForObject("SELECT id FROM clinlims.reference_tables WHERE name = 'ANALYSIS'",
                Long.class);
        jdbc.update(
                "INSERT INTO clinlims.sample (id, accession_number, domain, entered_date, received_date,"
                        + " is_confirmation, lastupdated) VALUES (?, 'NOTE135', 'H', now(), now(), false, now())",
                SAMPLE);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                + " VALUES (?, ?, 1, 1, now())", SAMPLE, SAMPLE);
        jdbc.update("INSERT INTO clinlims.analysis (id, sampitem_id, analysis_type) VALUES (?, ?, 'MANUAL')",
                ANALYSIS_OF_RESULT, SAMPLE);
        jdbc.update("INSERT INTO clinlims.analysis (id, sampitem_id, analysis_type) VALUES (?, ?, 'MANUAL')",
                ANALYSIS_WITH_RESULTS_ID, SAMPLE);
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, lastupdated) VALUES (?, ?, now())", RESULT,
                ANALYSIS_OF_RESULT);
        insertNote(MISFILED_NOTE, RESULT, ACCEPT_SUBJECT, "Rerun confirmed");
        insertNote(NOTE_ON_THE_OTHER_ANALYSIS, ANALYSIS_WITH_RESULTS_ID, "Result note", "Sample haemolysed");
        insertNote(NOTE_ON_NO_RESULT, NO_SUCH_RESULT, ACCEPT_SUBJECT, "Orphaned");
    }

    private void insertNote(long id, long referenceId, String subject, String text) {
        jdbc.update(
                "INSERT INTO clinlims.note (id, reference_id, reference_table, note_type, subject, text,"
                        + " lastupdated) VALUES (?, ?, ?, 'I', ?, ?, now())",
                id, referenceId, analysisTable, subject, text);
    }

    @Test
    public void theNoteSavedUnderTheResultsIdMovesToThatResultsAnalysis() throws Exception {
        jdbc.update(changesetSql());

        assertEquals(ANALYSIS_OF_RESULT, referenceOf(MISFILED_NOTE));
    }

    @Test
    public void aNoteTheOtherAnalysisCarriesUnderItsOwnIdStays() throws Exception {
        jdbc.update(changesetSql());

        assertEquals(ANALYSIS_WITH_RESULTS_ID, referenceOf(NOTE_ON_THE_OTHER_ANALYSIS));
    }

    @Test
    public void anAcceptNoteWhoseResultIsGoneIsLeftWhereItIs() throws Exception {
        jdbc.update(changesetSql());

        assertEquals(NO_SUCH_RESULT, referenceOf(NOTE_ON_NO_RESULT));
    }

    private long referenceOf(long noteId) {
        return jdbc.queryForObject("SELECT reference_id FROM clinlims.note WHERE id = ?", Long.class, noteId);
    }

    /**
     * The statement the changeset ships, so the test runs what a deployment runs.
     */
    private static String changesetSql() throws Exception {
        try (InputStream xml = AnalyzerAcceptNoteRepairChangesetTest.class.getClassLoader()
                .getResourceAsStream(CHANGESET)) {
            Document changeset = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
            return changeset.getElementsByTagName("sql").item(0).getTextContent();
        }
    }
}
