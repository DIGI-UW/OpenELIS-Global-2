package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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

/** Exercises the actual migration and rollback against legacy-shaped data. */
public class ReportingDecimalPlacesMigrationIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private javax.sql.DataSource dataSource;

    @Before
    public void loadFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
    }

    @Test
    public void upgradeAndRollbackPreserveNullExplicitZeroAndClinicalValues() throws Exception {
        String databaseName = "decimal_policy_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = dataSource.getConnection(); Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE " + databaseName);
            String url = admin.getMetaData().getURL().replace("/" + admin.getCatalog(), "/" + databaseName);
            try (Connection connection = DriverManager.getConnection(url, System.getProperty("db.user"),
                    System.getProperty("db.pass"))) {
                try (Statement setup = connection.createStatement()) {
                    setup.execute("CREATE SCHEMA clinlims");
                    for (String table : new String[] { "result", "test_result", "test_result_component" }) {
                        setup.execute("CREATE TABLE clinlims." + table
                                + " (id integer PRIMARY KEY, value text, significant_digits "
                                + (table.equals("test_result_component") ? "integer" : "numeric DEFAULT 0") + ")");
                        setup.execute("INSERT INTO clinlims." + table
                                + " VALUES (1, '1.1760', NULL), (2, '1.1760', 0), (3, '1.1760', 2)");
                    }
                }
                try (Liquibase migration = new Liquibase("liquibase/3.5.x.x/145-optional-reporting-decimal-places.xml",
                        new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
                    migration.update(new Contexts(), new LabelExpression());
                    verifyRows(connection, "reporting_decimal_places");
                    try (Statement query = connection.createStatement();
                            ResultSet rows = query.executeQuery(
                                    "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'clinlims'"
                                            + " AND column_name = 'reporting_decimal_places' AND column_default IS NULL")) {
                        rows.next();
                        assertEquals(3, rows.getInt(1));
                    }
                    migration.rollback(1, new Contexts(), new LabelExpression());
                    verifyRows(connection, "significant_digits");
                    migration.update(new Contexts(), new LabelExpression());
                    verifyRows(connection, "reporting_decimal_places");
                }
            } finally {
                statement.execute("DROP DATABASE " + databaseName);
            }
        }
    }

    private void verifyRows(Connection connection, String column) throws Exception {
        for (String table : new String[] { "result", "test_result", "test_result_component" }) {
            try (Statement query = connection.createStatement();
                    ResultSet rows = query
                            .executeQuery("SELECT value, " + column + " FROM clinlims." + table + " ORDER BY id")) {
                String[] expected = { null, "0", "2" };
                int count = 0;
                while (rows.next()) {
                    assertEquals("1.1760", rows.getString(1));
                    assertEquals(expected[count++], rows.getString(2));
                }
                assertEquals(3, count);
            }
        }
    }
}
