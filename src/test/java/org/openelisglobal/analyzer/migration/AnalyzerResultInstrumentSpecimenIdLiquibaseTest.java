package org.openelisglobal.analyzer.migration;

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
 * The instrument specimen ID column was added by changeset 120 until develop
 * took that number, and has been changeset 132 since. A database that ran it as
 * 120 already has the column; 132 must be marked ran there, or its ADD COLUMN
 * fails and the application never starts.
 */
public class AnalyzerResultInstrumentSpecimenIdLiquibaseTest {

    private static final String CHANGESET = "132-analyzer-result-instrument-specimen-id";
    private static final String OLD_CHANGESET = "120-analyzer-result-instrument-specimen-id";

    @Test
    public void aDatabaseThatAddedTheColumnAs120StillStarts() throws Exception {
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
                recordTheColumnAsAddedBy120(connection);

                new Liquibase("liquibase/base-changelog.xml", resources, database).update(new Contexts("test"));

                assertEquals("MARK_RAN", execType(connection, CHANGESET));
                assertEquals(1,
                        count(connection,
                                "SELECT count(*) FROM information_schema.columns"
                                        + " WHERE table_schema = 'clinlims' AND table_name = 'analyzer_results'"
                                        + " AND column_name = 'instrument_specimen_id'"));
            }
        }
    }

    private void recordTheColumnAsAddedBy120(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE clinlims.databasechangelog SET id = '" + OLD_CHANGESET + "', filename = '"
                    + "liquibase/3.5.x.x/" + OLD_CHANGESET + ".xml' WHERE id = '" + CHANGESET + "'");
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
