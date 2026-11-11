package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;

import java.util.List;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Result entry has one page. Changeset 117 removes the switch between the
 * unified and the legacy pages from Result configuration and deactivates the
 * retired pages' menu rows, leaving Results Entry.
 *
 * <p>
 * Other test fixtures truncate {@code site_information} and {@code menu}, so
 * Liquibase's own record is the primary evidence that the changesets ran; the
 * rows themselves are checked whenever they are present.
 */
public class LegacyResultsEntryRetiredTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void theUnifiedRouteSwitch_isGone() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> executions = jdbc.queryForList(
                "SELECT exectype FROM databasechangelog WHERE id = 'retire-results-entry-unified-route-switch'",
                String.class);
        assertEquals(List.of("EXECUTED"), executions);
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.site_information WHERE name = 'resultsEntryUnifiedRoute'",
                        Integer.class));
    }

    @Test
    public void theRetiredResultEntryMenuRows_areInactive_andResultsEntryStays() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> executions = jdbc.queryForList(
                "SELECT exectype FROM databasechangelog WHERE id = 'retire-legacy-results-entry-menu-rows'",
                String.class);
        assertEquals(List.of("EXECUTED"), executions);
        for (String active : jdbc.queryForList("SELECT cast(is_active as text) FROM clinlims.menu WHERE element_id IN"
                + " ('menu_results_logbook', 'menu_results_patient', 'menu_results_accession', 'menu_results_range',"
                + " 'menu_results_status')", String.class)) {
            assertEquals("false", active);
        }
        for (String active : jdbc.queryForList(
                "SELECT cast(is_active as text) FROM clinlims.menu WHERE element_id = 'menu_results_unified'",
                String.class)) {
            assertEquals("true", active);
        }
    }
}
