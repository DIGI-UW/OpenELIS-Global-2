package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;

import java.util.List;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The unified Results Entry worklist is the default: changeset 089 switches the
 * site flag seeded off by changeset 059 to true on every database.
 *
 * <p>
 * Other test fixtures truncate {@code site_information}, so Liquibase's own
 * record is the primary evidence: the changeset must have run (EXECUTED, not
 * MARK_RAN because the seed row was missing). The value itself is checked
 * whenever the seed row is still present.
 */
public class ResultsEntryUnifiedRouteDefaultTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void unifiedRouteFlag_isOnByDefault() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> executions = jdbc.queryForList(
                "SELECT exectype FROM databasechangelog WHERE id = 'results-unified-route-default-on'", String.class);
        assertEquals(List.of("EXECUTED"), executions);
        for (String value : jdbc.queryForList(
                "SELECT value FROM clinlims.site_information WHERE name = 'resultsEntryUnifiedRoute'", String.class)) {
            assertEquals("true", value);
        }
    }
}
