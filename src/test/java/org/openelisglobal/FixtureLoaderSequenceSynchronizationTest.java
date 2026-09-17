package org.openelisglobal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

/** Validates actual sequence allocation, including repeated fixture loading. */
@Transactional
public class FixtureLoaderSequenceSynchronizationTest extends BaseWebContextSensitiveTest {

    @Test
    public void fixtureAdvancesPastImportedIdsWithoutReusingPreviouslyAllocatedIds() throws Exception {
        long previousNext = jdbcTemplate.queryForObject(
                "SELECT last_value + CASE WHEN is_called THEN 1 ELSE 0 END FROM clinlims.observation_history_seq",
                Long.class);
        executeDataSetWithStateManagement("testdata/observation-history.xml");
        long importedNext = jdbcTemplate
                .queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM clinlims.observation_history", Long.class);
        long allocated = jdbcTemplate.queryForObject("SELECT nextval('clinlims.observation_history_seq')", Long.class);
        assertEquals("Loading a fixture must avoid both imported IDs and already allocated IDs",
                Math.max(previousNext, importedNext), allocated);

        executeDataSetWithStateManagement("testdata/observation-history.xml");
        assertEquals("Reloading the same small fixture must not reallocate the previous ID",
                Long.valueOf(allocated + 1),
                jdbcTemplate.queryForObject("SELECT nextval('clinlims.observation_history_seq')", Long.class));
    }
}
