package org.openelisglobal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class FixtureLoaderSequenceSynchronizationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void observationHistoryFixture_advancesItsStandaloneSequence() throws Exception {
        executeDataSetWithStateManagement("testdata/observation-history.xml");

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement
                        .executeQuery("SELECT (SELECT MAX(id) FROM clinlims.observation_history), last_value, is_called"
                                + " FROM clinlims.observation_history_seq")) {
            result.next();
            assertEquals(result.getLong(1) + 1, result.getLong(2));
            assertFalse(result.getBoolean(3));
        }
    }
}
