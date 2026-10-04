package org.openelisglobal.alert;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * qa-057 redeclared chk_alert_type with a narrower list than the core line had
 * already installed. On a database that held alerts of the dropped types (the
 * testing server, 2026-09-29) the ADD CONSTRAINT failed, Liquibase stopped and
 * the application never started, so no build could be deployed there. The
 * database here is brought to that state: the core line's constraint, one
 * FREEZER_HUMIDITY alert, and qa-057/qa-084 not yet applied.
 */
public class AlertTypeCheckUpgradeLiquibaseTest {

    private static final String QA_057 = "qa-057-extend-alert-type-check-for-eqa-submission";
    private static final String QA_084 = "qa-084-alert-type-union";

    @Test
    public void upgradeWithAlertsOfCoreOnlyTypesKeepsThemAndAllowsEqaSubmissionFailures() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                    "/docker-entrypoint-initdb.d");
            postgres.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
            postgres.withDatabaseName("clinlims");
            postgres.withUsername("clinlims");
            postgres.withPassword("clinlims");
            postgres.start();

            try (Connection connection = postgres.createConnection("")) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName("clinlims");
                ClassLoaderResourceAccessor resources = new ClassLoaderResourceAccessor();
                new Liquibase("liquibase/base-changelog.xml", resources, database).update(new Contexts("test"));
                rewindToCoreLineAlertTypesWithAHumidityAlert(connection);

                new Liquibase("liquibase/base-changelog.xml", resources, database).update(new Contexts("test"));

                assertEquals("MARK_RAN", execType(connection, QA_057));
                assertEquals("EXECUTED", execType(connection, QA_084));
                assertEquals(1,
                        count(connection, "SELECT count(*) FROM clinlims.alert WHERE alert_type = 'FREEZER_HUMIDITY'"));
                insertAlert(connection, "EQA_SUBMISSION_FAILED");
                assertEquals(1, count(connection,
                        "SELECT count(*) FROM clinlims.alert WHERE alert_type = 'EQA_SUBMISSION_FAILED'"));
            }
        }
    }

    private void rewindToCoreLineAlertTypesWithAHumidityAlert(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "DELETE FROM clinlims.databasechangelog WHERE id IN ('" + QA_057 + "', '" + QA_084 + "')");
            statement.executeUpdate("ALTER TABLE clinlims.alert DROP CONSTRAINT IF EXISTS chk_alert_type");
            statement.executeUpdate("ALTER TABLE clinlims.alert ADD CONSTRAINT chk_alert_type CHECK (alert_type IN ("
                    + "'FREEZER_TEMPERATURE', 'FREEZER_HUMIDITY', 'FREEZER_OFFLINE', 'EQUIPMENT_FAILURE',"
                    + " 'INVENTORY_LOW', 'SAMPLE_TRACKING', 'OTHER', 'EQA_DEADLINE', 'SAMPLE_EXPIRATION',"
                    + " 'STAT_UPCOMING', 'STAT_OVERDUE', 'CRITICAL_UNACKNOWLEDGED', 'CRITICAL_RESULT',"
                    + " 'REFERRAL_CRITICAL_RESULT', 'REFERRAL_REJECTED', 'REQUIRED_BY_DEADLINE',"
                    + " 'MICROBIOLOGY_CRITICAL'))");
        }
        insertAlert(connection, "FREEZER_HUMIDITY");
    }

    private void insertAlert(Connection connection, String alertType) throws Exception {
        String sql = "INSERT INTO clinlims.alert (id, alert_type, alert_entity_type, alert_entity_id, severity, status,"
                + " start_time, message) VALUES ((SELECT coalesce(max(id), 0) + 1 FROM clinlims.alert), ?, 'FREEZER',"
                + " 1, 'WARNING', 'OPEN', now(), 'upgrade probe')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, alertType);
            statement.executeUpdate();
        }
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    private String execType(Connection connection, String id) throws Exception {
        try (PreparedStatement statement = connection
                .prepareStatement("SELECT exectype FROM clinlims.databasechangelog WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private int count(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }
}
