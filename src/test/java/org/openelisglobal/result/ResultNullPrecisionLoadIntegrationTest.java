package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1170 — loading a result that records no precision must not fail.
 *
 * <p>
 * {@code result.significant_digits} and {@code result.grouping} are nullable,
 * and both were mapped onto primitive {@code int} fields. Hibernate cannot
 * assign null to a primitive, so reading one such row threw
 * {@code PropertyAccessException} and aborted the whole query — the worklist
 * for the lab unit that happened to contain the row returned HTTP 500 while
 * every other unit returned 200, and the page showed the technician an empty
 * queue.
 *
 * <p>
 * This exercises the mapping, not the accessors: it nulls the columns in the
 * database and reads the row back through the service.
 */
public class ResultNullPrecisionLoadIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultService resultService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    public void aResultWithNoRecordedPrecisionLoadsInsteadOfFailingTheQuery() {
        List<Result> all = resultService.getAll();
        assertNotNull("fixture must provide a result to blank", all);
        String id = all.get(0).getId();

        jdbc.update("UPDATE clinlims.result SET significant_digits = NULL, grouping = NULL WHERE id = ?",
                Integer.valueOf(id));

        // Reading it back is what used to throw; the query it belonged to went
        // down with it, taking every other row in the same lab unit.
        Result reloaded = resultService.get(id);

        assertNotNull("the row loads", reloaded);
        assertEquals("an unrecorded precision reports the value exactly as stored", -1,
                reloaded.getSignificantDigits());
        assertEquals("an unrecorded grouping is the only group", 0, reloaded.getGrouping());
    }

    /**
     * The worklist's own failure mode: one blank row must not stop the rest being
     * listed.
     */
    @Test
    public void oneSuchRowDoesNotStopTheOthersBeingListed() {
        int before = resultService.getAll().size();
        String id = resultService.getAll().get(0).getId();

        jdbc.update("UPDATE clinlims.result SET significant_digits = NULL WHERE id = ?", Integer.valueOf(id));

        assertEquals("every result still lists", before, resultService.getAll().size());
    }
}
