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
    public void clinicalContextRollbackCannotTurnConditionalRulesIntoUnconditionalRules() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.start();
            try (Connection connection = postgres.createConnection("")) {
                try (var sql = connection.createStatement()) {
                    sql.execute("CREATE SCHEMA clinlims");
                    sql.execute("CREATE TABLE clinlims.micro_ast_run (id varchar(36) PRIMARY KEY)");
                    sql.execute("CREATE TABLE clinlims.micro_breakpoint_rule (id varchar(36) PRIMARY KEY)");
                }
                var database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                try (var migration = new Liquibase("liquibase/3.5.x.x/142-amr-clinical-context.xml",
                        new ClassLoaderResourceAccessor(), database)) {
                    migration.update(new Contexts());
                    migration.rollback(1, "");
                    migration.update(new Contexts());
                    try (var sql = connection.createStatement()) {
                        sql.execute(
                                "INSERT INTO clinlims.micro_breakpoint_rule VALUES ('conditional', 'NON_MENINGITIS')");
                    }
                    connection.commit();
                    assertThrows(liquibase.exception.LiquibaseException.class, () -> migration.rollback(1, ""));
                    database.rollback();
                    assertEquals("NON_MENINGITIS", scalar(connection,
                            "SELECT clinical_context FROM clinlims.micro_breakpoint_rule WHERE id='conditional'"));
                }
            }
        }
    }

    @Test
    public void diskContentRollbackCannotDiscardWhatARunRecorded() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.start();
            try (Connection connection = postgres.createConnection("")) {
                try (var sql = connection.createStatement()) {
                    sql.execute("CREATE SCHEMA clinlims");
                    sql.execute("CREATE TABLE clinlims.micro_breakpoint_rule (id varchar(36) PRIMARY KEY)");
                    sql.execute("CREATE TABLE clinlims.micro_ast_panel_antibiotic (id varchar(36) PRIMARY KEY)");
                    sql.execute("CREATE TABLE clinlims.micro_ast_run_antibiotic (id varchar(36) PRIMARY KEY)");
                }
                var database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                try (var migration = new Liquibase("liquibase/3.5.x.x/143-amr-disk-content.xml",
                        new ClassLoaderResourceAccessor(), database)) {
                    migration.update(new Contexts());
                    migration.rollback(1, "");
                    migration.update(new Contexts());
                    try (var sql = connection.createStatement()) {
                        sql.execute("INSERT INTO clinlims.micro_ast_run_antibiotic VALUES ('drug', '30ug')");
                    }
                    connection.commit();
                    assertThrows(liquibase.exception.LiquibaseException.class, () -> migration.rollback(1, ""));
                    database.rollback();
                    assertEquals("30ug", scalar(connection,
                            "SELECT disk_content FROM clinlims.micro_ast_run_antibiotic WHERE id='drug'"));
                }
            }
        }
    }

    @Test
    public void lookupIndexMigrationAndRollbackPreserveRules() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.start();
            try (Connection connection = postgres.createConnection("")) {
                try (var sql = connection.createStatement()) {
                    sql.execute("CREATE SCHEMA clinlims");
                    sql.execute("CREATE TABLE clinlims.micro_breakpoint_rule (id varchar(36) PRIMARY KEY, "
                            + "standard_id varchar(36), antibiotic_id varchar(36), organism_id varchar(36))");
                    sql.execute(
                            "INSERT INTO clinlims.micro_breakpoint_rule VALUES ('rule','edition','agent','organism')");
                }
                var database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                try (var migration = new Liquibase("liquibase/3.5.x.x/141-amr-breakpoint-lookup-index.xml",
                        new ClassLoaderResourceAccessor(), database)) {
                    migration.update(new Contexts());
                    assertEquals("1", scalar(connection, "SELECT count(*) FROM pg_indexes WHERE schemaname='clinlims' "
                            + "AND indexname='idx_micro_breakpoint_rule_lookup'"));
                    migration.rollback(1, "");
                    assertEquals("0", scalar(connection, "SELECT count(*) FROM pg_indexes WHERE schemaname='clinlims' "
                            + "AND indexname='idx_micro_breakpoint_rule_lookup'"));
                    assertEquals("organism", scalar(connection,
                            "SELECT organism_id FROM clinlims.micro_breakpoint_rule WHERE id='rule'"));
                }
            }
        }
    }

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
