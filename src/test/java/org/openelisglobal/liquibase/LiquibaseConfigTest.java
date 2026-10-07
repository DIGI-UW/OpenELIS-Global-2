package org.openelisglobal.liquibase;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.Map;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

public class LiquibaseConfigTest {
    @Test
    public void ordinaryStartupDoesNotInventCutoverAttribution() {
        SpringLiquibase migration = configure(new MockEnvironment());
        assertEquals(Map.of(), ReflectionTestUtils.getField(migration, "parameters"));
        assertEquals("classpath:liquibase/base-changelog.xml", migration.getChangeLog());
        assertEquals("default", migration.getContexts());
    }

    @Test
    public void explicitConfigurationIsPassedToMigrationWithoutChangingRoot() {
        SpringLiquibase migration = configure(validEnvironment());
        assertEquals(
                Map.of("amr.cutover.mappingFile", "file:/approved/cases.csv", "amr.cutover.actorId", "42",
                        "amr.cutover.at", "2026-10-06 12:00:00"),
                ReflectionTestUtils.getField(migration, "parameters"));
        assertEquals("classpath:liquibase/base-changelog.xml", migration.getChangeLog());
    }

    @Test
    public void partialConfigurationIsRejectedBeforeDatabaseAccess() {
        for (String missing : new String[] { "amr.cutover.mappingFile", "amr.cutover.actorId", "amr.cutover.at" }) {
            assertThrows(IllegalArgumentException.class,
                    () -> configure(validEnvironment().withProperty(missing, " ")));
        }
    }

    @Test
    public void invalidActorCannotReachSqlSubstitution() {
        for (String actor : new String[] { "-1", "1.5", "1'; SELECT 1; --", "migration-user" }) {
            assertThrows(IllegalArgumentException.class,
                    () -> configure(validEnvironment().withProperty("amr.cutover.actorId", actor)));
        }
    }

    @Test
    public void invalidOrSqlBearingTimestampIsRejected() {
        for (String at : new String[] { "2026-02-30 12:00:00", "2026-10-06 25:00:00", "now()",
                "2026-10-06 12:00:00'; SELECT 1; --" }) {
            assertThrows(IllegalArgumentException.class,
                    () -> configure(validEnvironment().withProperty("amr.cutover.at", at)));
        }
    }

    private MockEnvironment validEnvironment() {
        return new MockEnvironment().withProperty("amr.cutover.mappingFile", "file:/approved/cases.csv")
                .withProperty("amr.cutover.actorId", "42").withProperty("amr.cutover.at", "2026-10-06 12:00:00");
    }

    private SpringLiquibase configure(MockEnvironment environment) {
        LiquibaseConfig config = new LiquibaseConfig();
        ReflectionTestUtils.setField(config, "dataSource", mock(DataSource.class));
        ReflectionTestUtils.setField(config, "contexts", "default");
        ReflectionTestUtils.setField(config, "environment", environment);
        return config.liquibase();
    }
}
