package org.openelisglobal.testconfiguration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The option list behind Rename Existing Result List Options.
 *
 * <p>
 * It is built from {@code test_result} rows, so it can name a dictionary entry
 * that is no longer there. Those came back as nulls and the rename screen died
 * on the first one, leaving an empty page however many valid options followed.
 */
public class ResultSelectListOptionsTest extends BaseWebContextSensitiveTest {

    private static final long PRESENT = 96601L;
    private static final String MISSING_DICTIONARY_ID = "9660499";
    private static final long[] TEST_RESULTS = { 96605L, 96606L, 96607L };

    @Autowired
    private ResultSelectListService resultSelectListService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();

        Long categoryId = jdbc.queryForObject("SELECT min(id) FROM clinlims.dictionary_category", Long.class);
        Long testId = jdbc.queryForObject("SELECT min(id) FROM clinlims.test", Long.class);

        jdbc.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev,"
                        + " dictionary_category_id, sort_order, lastupdated) VALUES (?, 'Y', ?, ?, ?, 1, NOW())",
                PRESENT, "RenameOptionsTestEntry", "RenameOptionsTestEntry", categoryId);

        // One resolvable result, one naming a dictionary row that does not exist, one
        // naming nothing at all. The last two are what the screen used to choke on.
        insertTestResult(TEST_RESULTS[0], testId, String.valueOf(PRESENT));
        insertTestResult(TEST_RESULTS[1], testId, MISSING_DICTIONARY_ID);
        insertTestResult(TEST_RESULTS[2], testId, "");
    }

    private void insertTestResult(long id, Long testId, String value) {
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, sort_order, is_active,"
                + " lastupdated) VALUES (?, ?, 'D', ?, ?, true, NOW())", id, testId, value, id);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void theOptionListNeverContainsANull() {
        List<Dictionary> options = resultSelectListService.getAllSelectListOptions();

        assertFalse("the list is not empty", options.isEmpty());
        assertFalse("a null here empties the rename screen", options.contains(null));
    }

    @Test
    public void theOptionsThatDoResolveAreStillListed() {
        // Leaving out the ones that cannot be resolved must not cost the valid ones
        // beside them.
        List<Dictionary> options = resultSelectListService.getAllSelectListOptions();

        assertTrue(options.stream().anyMatch(o -> String.valueOf(PRESENT).equals(o.getId())));
        assertTrue("nothing resolves to the missing entry",
                options.stream().noneMatch(o -> MISSING_DICTIONARY_ID.equals(o.getId())));
    }

    private void cleanup() {
        for (long id : TEST_RESULTS) {
            jdbc.update("DELETE FROM clinlims.test_result WHERE id = ?", id);
        }
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id = ?", PRESENT);
    }
}
