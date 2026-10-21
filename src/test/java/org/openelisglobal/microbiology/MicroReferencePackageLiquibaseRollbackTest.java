package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import java.sql.Connection;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

public class MicroReferencePackageLiquibaseRollbackTest {
    @Test
    public void rollbackWorksBeforeImportAndRefusesToLosePackageSemantics() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.start();
            try (Connection connection = postgres.createConnection("")) {
                try (var sql = connection.createStatement()) {
                    sql.execute("CREATE SCHEMA clinlims");
                    for (String table : java.util.List.of("micro_organism", "micro_antibiotic", "micro_breakpoint_rule",
                            "micro_breakpoint_standard")) {
                        sql.execute("CREATE TABLE clinlims." + table + " (id varchar(36) PRIMARY KEY)");
                    }
                    sql.execute("INSERT INTO clinlims.micro_breakpoint_rule(id) VALUES ('legacy')");
                }
                var database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                try (var migration = new Liquibase("liquibase/3.5.x.x/140-amr-reference-package.xml",
                        new ClassLoaderResourceAccessor(), database)) {
                    migration.update(new Contexts());
                    assertEquals("true", scalar(connection,
                            "SELECT resistant_inclusive::text FROM clinlims.micro_breakpoint_rule WHERE id='legacy'"));
                    migration.rollback(1, "");
                    assertEquals("1", scalar(connection, "SELECT count(*) FROM clinlims.micro_breakpoint_rule"));
                    migration.update(new Contexts());
                    try (var sql = connection.createStatement()) {
                        sql.execute(
                                "UPDATE clinlims.micro_breakpoint_rule SET resistant_inclusive=false WHERE id='legacy'");
                    }
                    connection.commit();
                    assertThrows(liquibase.exception.LiquibaseException.class, () -> migration.rollback(1, ""));
                    database.rollback();
                    assertEquals("false", scalar(connection,
                            "SELECT resistant_inclusive::text FROM clinlims.micro_breakpoint_rule WHERE id='legacy'"));
                    try (var sql = connection.createStatement()) {
                        sql.execute(
                                "UPDATE clinlims.micro_breakpoint_rule SET resistant_inclusive=true WHERE id='legacy'");
                        sql.execute(
                                "INSERT INTO clinlims.micro_breakpoint_standard(id,configuration_hash) VALUES ('managed','"
                                        + "a".repeat(64) + "')");
                    }
                    connection.commit();
                    assertThrows(liquibase.exception.LiquibaseException.class, () -> migration.rollback(1, ""));
                    database.rollback();
                    assertEquals("a".repeat(64), scalar(connection,
                            "SELECT configuration_hash FROM clinlims.micro_breakpoint_standard WHERE id='managed'"));
                }
            }
        }
    }

    private String scalar(Connection connection, String query) throws Exception {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(query)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
