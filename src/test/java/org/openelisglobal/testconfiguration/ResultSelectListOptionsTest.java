package org.openelisglobal.testconfiguration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.testconfiguration.service.ResultSelectListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * The option list behind Rename Existing Result List Options.
 *
 * <p>
 * It is built from {@code test_result} rows, so it can name a dictionary entry
 * that is no longer there. Those came back as nulls and the rename screen died
 * on the first one, leaving an empty page however many valid options followed.
 */
@Transactional
public class ResultSelectListOptionsTest extends BaseWebContextSensitiveTest {

    private static final long PRESENT = 96601L;
    private static final long CATEGORY = 96602L;
    private static final long TEST = 96603L;
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
        jdbc.update("INSERT INTO clinlims.dictionary_category (id, name, description, lastupdated)"
                + " VALUES (?, 'Result select options test', 'Owned test category', NOW())", CATEGORY);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable, lastupdated)"
                        + " VALUES (?, 'Result select options test', 'Owned test row', 'Y', ?, 'CLINICAL', true, NOW())",
                TEST, java.util.UUID.randomUUID().toString());

        jdbc.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev,"
                        + " dictionary_category_id, sort_order, lastupdated) VALUES (?, 'Y', ?, ?, ?, 1, NOW())",
                PRESENT, "RenameOptionsTestEntry", "RenameOptionsTestEntry", CATEGORY);

        // One resolvable result, one naming a dictionary row that does not exist, one
        // naming nothing at all. The last two are what the screen used to choke on.
        insertTestResult(TEST_RESULTS[0], TEST, String.valueOf(PRESENT));
        insertTestResult(TEST_RESULTS[1], TEST, MISSING_DICTIONARY_ID);
        insertTestResult(TEST_RESULTS[2], TEST, "");
    }

    private void insertTestResult(long id, long testId, String value) {
        jdbc.update("INSERT INTO clinlims.test_result (id, test_id, tst_rslt_type, value, sort_order, is_active,"
                + " lastupdated) VALUES (?, ?, 'D', ?, ?, true, NOW())", id, testId, value, id);
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
}
