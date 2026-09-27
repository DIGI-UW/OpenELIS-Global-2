package org.openelisglobal.liquibase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Exercises the full application changelog with data present before an upgrade.
 */
@RunWith(Parameterized.class)
public class DatabaseUpgradeIntegrationTest {

    private static final String ROOT = "database-upgrades/";
    private static final String CHANGELOG = "liquibase/base-changelog.xml";
    private final String scenario;

    public DatabaseUpgradeIntegrationTest(String scenario) {
        this.scenario = scenario;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<String> scenarios() throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource(ROOT + "index.txt"), StandardCharsets.UTF_8))) {
            List<String> scenarios = reader.lines().map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
            assertTrue("At least one existing-data upgrade fixture is required", !scenarios.isEmpty());
            return scenarios;
        }
    }

    @Test
    public void fullChangelogHandlesExistingData() throws Exception {
        Properties fixture = new Properties();
        try (InputStream input = resource(ROOT + scenario + ".properties")) {
            fixture.load(input);
        }
        Contexts contexts = new Contexts(fixture.getProperty("contexts", "default"));
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4").withDatabaseName("clinlims")
                .withUsername("clinlims").withPassword("clinlims").withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5")
                .withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                        "/docker-entrypoint-initdb.d")) {
            postgres.start();
            try (Liquibase baseline = liquibase(postgres, CHANGELOG)) {
                List<ChangeSet> pending = baseline.listUnrunChangeSets(contexts, new LabelExpression());
                int boundary = -1;
                for (int index = 0; index < pending.size(); index++) {
                    ChangeSet change = pending.get(index);
                    if (change.getId().equals(required(fixture, "before.id"))
                            && change.getFilePath().equals(required(fixture, "before.file"))) {
                        boundary = index;
                        break;
                    }
                }
                assertTrue("Upgrade boundary must be present and pending: " + scenario, boundary >= 0);
                if (boundary > 0) {
                    baseline.update(boundary, contexts, new LabelExpression());
                }
                assertEquals(required(fixture, "before.id"), pendingOneTimeChanges(baseline, contexts).get(0).getId());
            }
            apply(postgres, required(fixture, "data"), contexts);
            try (Liquibase upgrade = liquibase(postgres, CHANGELOG)) {
                String expectedFailure = fixture.getProperty("expected.failure");
                try {
                    upgrade.update(contexts);
                    if (expectedFailure != null) {
                        fail("Unsafe upgrade unexpectedly succeeded: " + scenario);
                    }
                } catch (LiquibaseException failure) {
                    if (expectedFailure == null) {
                        throw failure;
                    }
                    StringBuilder messages = new StringBuilder();
                    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                        messages.append(cause.getMessage()).append('\n');
                    }
                    assertTrue(messages.toString(), messages.toString().contains(expectedFailure));
                }
                if (expectedFailure == null) {
                    assertTrue("All candidate changes must run", pendingOneTimeChanges(upgrade, contexts).isEmpty());
                    upgrade.update(contexts);
                    assertTrue("A repeated upgrade must have no unapplied changes",
                            pendingOneTimeChanges(upgrade, contexts).isEmpty());
                }
            }
            apply(postgres, required(fixture, "assertions"), contexts);
        }
    }

    private static List<ChangeSet> pendingOneTimeChanges(Liquibase liquibase, Contexts contexts) throws Exception {
        // CONTINUE changes deliberately remain pending until their optional data
        // exists.
        return liquibase.listUnrunChangeSets(contexts).stream().filter(change -> !change.shouldAlwaysRun())
                .filter(change -> change.getPreconditions() == null
                        || change.getPreconditions().getOnFail() != PreconditionContainer.FailOption.CONTINUE)
                .toList();
    }

    private static void apply(PostgreSQLContainer<?> postgres, String changelog, Contexts contexts) throws Exception {
        try (Liquibase liquibase = liquibase(postgres, ROOT + changelog)) {
            liquibase.update(contexts);
        }
    }

    private static Liquibase liquibase(PostgreSQLContainer<?> postgres, String changelog) throws Exception {
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword());
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName("clinlims");
        database.setLiquibaseSchemaName("clinlims");
        return new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing upgrade fixture property: " + key);
        }
        return value;
    }

    private static InputStream resource(String path) {
        InputStream input = DatabaseUpgradeIntegrationTest.class.getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalArgumentException("Missing upgrade fixture: " + path);
        }
        return input;
    }
}
