package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class LogDisplayMigrationIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private javax.sql.DataSource dataSource;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
    }

    @Test
    public void seedUpgradeAndRollbackRespectIdentityAndLaboratoryEdits() throws Exception {
        String databaseName = "log_policy_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = dataSource.getConnection(); Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + databaseName);
            String url = admin.getMetaData().getURL().replace("/" + admin.getCatalog(), "/" + databaseName);
            try (Connection connection = DriverManager.getConnection(url, System.getProperty("db.user"),
                    System.getProperty("db.pass"))) {
                try (Statement setup = connection.createStatement()) {
                    setup.execute("CREATE SCHEMA clinlims");
                    setup.execute(
                            "CREATE TABLE clinlims.test (id integer PRIMARY KEY, guid varchar(36), name text, loinc text)");
                    setup.execute(
                            "CREATE TABLE clinlims.test_result_component (id varchar(36) PRIMARY KEY, test_id integer, is_primary boolean, is_active char(1), result_type char(1), last_updated timestamp)");
                    setup.execute("CREATE TABLE clinlims.result (value text)");
                    setup.execute("INSERT INTO clinlims.result VALUES ('0.0064')");
                }
                try (Liquibase initial = migration(connection, "146-explicit-log-display.xml")) {
                    initial.update(new Contexts(), new LabelExpression());
                    try (Statement setup = connection.createStatement()) {
                        setup.execute(
                                "INSERT INTO clinlims.test VALUES (1, 'b50d156e-0f6f-40cd-921c-4e831602a623', 'Renamed seed', NULL), (2, '00000000-0000-0000-0000-000000000002', 'Viral Load', '25836-8')");
                        setup.execute(
                                "INSERT INTO clinlims.test_result_component VALUES ('seed', 1, true, 'Y', 'A', '2000-01-01', false), ('local', 2, true, 'Y', 'N', '2000-01-01', false), ('secondary', 1, false, 'Y', 'N', '2000-01-01', false)");
                    }
                    connection.commit();
                    // A separate Liquibase handle shares the same live JDBC connection.
                    Liquibase upgrade = migration(connection, "147-log-display-source-and-seed.xml");
                    connection.commit();
                    upgrade.update(new Contexts(), new LabelExpression());
                    assertEquals("true,false,false", flags(connection));
                    connection.commit();
                    upgrade.rollback(2, new Contexts(), new LabelExpression());
                    assertEquals("false,false,false", flags(connection));
                    try (Statement edit = connection.createStatement()) {
                        edit.execute(
                                "UPDATE clinlims.test_result_component SET last_updated='2100-01-01' WHERE id='seed'");
                    }
                    connection.commit();
                    upgrade.update(new Contexts(), new LabelExpression());
                    assertEquals("false,false,false", flags(connection));
                    connection.commit();
                    upgrade.rollback(2, new Contexts(), new LabelExpression());
                    try (Statement edit = connection.createStatement()) {
                        edit.execute(
                                "UPDATE clinlims.test_result_component SET last_updated='2000-01-01' WHERE id='seed'");
                    }
                    connection.commit();
                    upgrade.update(new Contexts(), new LabelExpression());
                    try (Statement edit = connection.createStatement()) {
                        edit.execute(
                                "UPDATE clinlims.test_result_component SET display_log10=false, last_updated='2100-01-01' WHERE id='seed'");
                    }
                    connection.commit();
                    upgrade.rollback(2, new Contexts(), new LabelExpression());
                    assertEquals("false,false,false", flags(connection));
                    try (Statement edit = connection.createStatement();
                            var row = edit.executeQuery("SELECT value FROM clinlims.result")) {
                        row.next();
                        assertEquals("0.0064", row.getString(1));
                    }
                    try (Statement edit = connection.createStatement();
                            var row = edit.executeQuery(
                                    "SELECT last_updated::date::text FROM clinlims.test_result_component WHERE id='seed'")) {
                        row.next();
                        assertEquals("2100-01-01", row.getString(1));
                    }
                    try (Statement edit = connection.createStatement()) {
                        edit.execute(
                                "UPDATE clinlims.test_result_component SET display_log10=true, last_updated='2000-01-01' WHERE id='seed'");
                    }
                    connection.commit();
                    upgrade.update(new Contexts(), new LabelExpression());
                    upgrade.rollback(2, new Contexts(), new LabelExpression());
                    assertEquals("true,false,false", flags(connection));
                }
            } finally {
                statement.execute("DROP DATABASE " + databaseName);
            }
        }
    }

    private Liquibase migration(Connection connection, String file) throws Exception {
        return new Liquibase("liquibase/3.5.x.x/" + file, new ClassLoaderResourceAccessor(),
                new JdbcConnection(connection));
    }

    private String flags(Connection connection) throws Exception {
        try (Statement query = connection.createStatement();
                var rows = query.executeQuery(
                        "SELECT display_log10 FROM clinlims.test_result_component ORDER BY CASE id WHEN 'seed' THEN 1 WHEN 'local' THEN 2 ELSE 3 END")) {
            java.util.List<String> flags = new java.util.ArrayList<>();
            while (rows.next())
                flags.add(Boolean.toString(rows.getBoolean(1)));
            return String.join(",", flags);
        }
    }
}
