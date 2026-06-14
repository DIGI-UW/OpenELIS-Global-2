package org.openelisglobal;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.filter.ExcludeTableFilter;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for the small set of integration tests that exercise genuinely
 * asynchronous, {@code @TransactionalEventListener(AFTER_COMMIT)} /
 * {@code @Async} production paths. Those handlers run on a separate thread
 * after the triggering transaction commits, so they can only observe COMMITTED
 * fixture data — pure per-test rollback (the default in
 * {@link BaseWebContextSensitiveTest}) would never let the event fire.
 *
 * <p>
 * This class therefore opts out of the test transaction ({@code NOT_SUPPORTED})
 * and commits fixtures, then truncates whatever it loaded in an {@code @After}
 * teardown so it does not pollute the 250 rollback tests. It is the deliberate,
 * isolated exception to the rollback model — keep its membership minimal.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public abstract class BaseCommittedFixtureTest extends BaseWebContextSensitiveTest {

    /**
     * Tables loaded by this test's fixtures, truncated on teardown to avoid
     * pollution.
     */
    private final List<String> committedTables = new ArrayList<>();

    /**
     * On top of the parent's {@link #PROTECTED_SEED_TABLES}, the committed loader
     * must also never touch the {@code system_user} family. Its truncate is
     * permanent (it commits), so a dataset that redeclares {@code system_user} —
     * all current committed datasets redeclare the admin {@code id=1} — would
     * {@code CASCADE}-wipe the seeded
     * {@code system_user_role}/{@code module}/{@code section} permission rows and
     * leave the admin without permissions for the rest of the suite. The
     * Liquibase/SQL seed already provides the admin user every fixture FKs to, so
     * excluding the whole family keeps that seed intact; datasets fall back to the
     * seeded admin.
     */
    private static final String[] COMMITTED_PROTECTED_SEED_TABLES = java.util.stream.Stream.concat(
            java.util.Arrays.stream(PROTECTED_SEED_TABLES),
            java.util.stream.Stream.of("system_user", "system_user_role", "system_user_module", "system_user_section"))
            .toArray(String[]::new);

    /**
     * Committed counterpart to the parent's rollback loader: truncate-then-REFRESH
     * in a single connection, then commit so async/AFTER_COMMIT handlers can see
     * the data. Records the loaded tables for teardown.
     */
    @Override
    protected void executeDataSetWithStateManagement(String datasetFileName) throws Exception {
        if (datasetFileName == null) {
            throw new NullPointerException("Please provide test dataset file to execute!");
        }
        try (Connection jdbcConn = dataSource.getConnection()) {
            jdbcConn.setAutoCommit(false);
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(datasetFileName)) {
                if (inputStream == null) {
                    throw new IllegalArgumentException("Dataset file '" + datasetFileName + "' not found in classpath");
                }
                // Exclude reference_tables AND the system_user family from the dataset
                // entirely (see COMMITTED_PROTECTED_SEED_TABLES) so a fixture's rows for
                // those tables neither truncate nor REFRESH over the committed Liquibase
                // seed — the committed truncate would otherwise permanently wipe the admin's
                // permission rows.
                IDataSet dataset = new FilteredDataSet(new ExcludeTableFilter(COMMITTED_PROTECTED_SEED_TABLES),
                        new FlatXmlDataSetBuilder().setColumnSensing(true).build(inputStream));
                String[] tables = dataset.getTableNames();
                truncateTablesInConnection(jdbcConn, tables);
                DatabaseOperation.REFRESH.execute(buildDbUnitConnection(jdbcConn), dataset);
                jdbcConn.commit();
                for (String table : tables) {
                    committedTables.add(table);
                }
                if (statusService != null) {
                    statusService.refreshCache();
                }
            } catch (Exception e) {
                jdbcConn.rollback();
                throw e;
            }
        }
    }

    /**
     * Truncate everything this test committed and restore the admin seed, so
     * subsequent tests (rollback or committed) start from the clean Liquibase
     * baseline.
     */
    @After
    public void teardownCommittedFixture() throws SQLException {
        if (!committedTables.isEmpty()) {
            cleanRowsInCurrentConnection(committedTables.toArray(new String[0]));
            committedTables.clear();
        }
        // A fixture may have truncated/replaced system_user; make sure the audit admin
        // (id=1) the rest of the suite assumes is present again.
        ensureAuditSystemUser();
    }
}
