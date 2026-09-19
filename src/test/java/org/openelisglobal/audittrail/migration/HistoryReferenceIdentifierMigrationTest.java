package org.openelisglobal.audittrail.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/** Executes the actual migration against isolated PostgreSQL schemas. */
public class HistoryReferenceIdentifierMigrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private DataSource dataSource;

    @Test
    public void emptyHistoryCanUpgradeAndRollBack() throws Exception {
        verifyUpgradeAndRollback(0, false);
    }

    @Test
    public void populatedHistoryRetainsExactReferencesAndRefusesToDiscardUuidHistoryOnRollback() throws Exception {
        verifyUpgradeAndRollback(100000, true);
    }

    private void verifyUpgradeAndRollback(int rows, boolean checkUuid) throws Exception {
        String schema = "history_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try {
            jdbcTemplate.execute("CREATE SCHEMA " + schema);
            jdbcTemplate.execute("CREATE TABLE " + schema + ".history (id NUMERIC(10,0) PRIMARY KEY,"
                    + " reference_id NUMERIC(22,0) NOT NULL, changes BYTEA)");
            jdbcTemplate.execute("CREATE INDEX history_reference_idx ON " + schema + ".history(reference_id)");
            jdbcTemplate.execute("INSERT INTO " + schema + ".history (id, reference_id, changes)"
                    + " SELECT id, 9999999999999999000000::numeric + id, convert_to('retained history', 'UTF8')"
                    + " FROM generate_series(1," + rows + ") id");
            String checksum = checksum(schema);
            try (Connection connection = dataSource.getConnection()) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName(schema);
                database.setLiquibaseSchemaName(schema);
                try (Liquibase liquibase = new Liquibase("liquibase/3.5.x.x/104-history-reference-identifiers.xml",
                        new ClassLoaderResourceAccessor(), database)) {
                    liquibase.getChangeLogParameters().set("history.schema", schema);
                    liquibase.update(new Contexts("test"));
                    assertEquals("character varying", referenceType(schema));
                    assertEquals(checksum, checksum(schema));
                    assertEquals(Integer.valueOf(rows), rowCount(schema));
                    liquibase.rollback(1, "test");
                    assertEquals("numeric", referenceType(schema));
                    assertEquals(checksum, checksum(schema));
                    assertEquals(Integer.valueOf(rows), rowCount(schema));

                    if (checkUuid) {
                        liquibase.update(new Contexts("test"));
                        String uuid = UUID.randomUUID().toString();
                        jdbcTemplate.update("INSERT INTO " + schema + ".history (id, reference_id, changes)"
                                + " VALUES (?, ?, convert_to('UUID history', 'UTF8'))", rows + 1, uuid);
                        assertEquals(Integer.valueOf(1),
                                jdbcTemplate.queryForObject(
                                        "SELECT COUNT(*) FROM " + schema + ".history WHERE reference_id = ?",
                                        Integer.class, uuid));
                        LiquibaseException failure = assertThrows(LiquibaseException.class,
                                () -> liquibase.rollback(1, "test"));
                        Throwable cause = failure;
                        while (cause != null && !(cause instanceof SQLException)) {
                            cause = cause.getCause();
                        }
                        assertNotNull("Rollback must fail on the UUID-to-numeric conversion", cause);
                        assertEquals("22P02", ((SQLException) cause).getSQLState());
                        assertEquals("character varying", referenceType(schema));
                        assertEquals(Integer.valueOf(rows + 1), rowCount(schema));
                        assertEquals(uuid,
                                jdbcTemplate.queryForObject(
                                        "SELECT reference_id FROM " + schema + ".history WHERE id = ?", String.class,
                                        rows + 1));
                        assertEquals(checksum, jdbcTemplate.queryForObject(
                                "SELECT md5(string_agg(reference_id::text || encode(changes, 'hex'), ',' ORDER BY id))"
                                        + " FROM " + schema + ".history WHERE id <= ?",
                                String.class, rows));
                    }
                }
            }
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private String referenceType(String schema) {
        return jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns"
                        + " WHERE table_schema = ? AND table_name = 'history' AND column_name = 'reference_id'",
                String.class, schema);
    }

    private String checksum(String schema) {
        return jdbcTemplate.queryForObject(
                "SELECT md5(string_agg(reference_id::text || encode(changes, 'hex'), ',' ORDER BY id)) FROM " + schema
                        + ".history",
                String.class);
    }

    private Integer rowCount(String schema) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + ".history", Integer.class);
    }
}
