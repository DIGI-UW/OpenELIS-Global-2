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
                // Exclude PROTECTED_SEED_TABLES (reference_tables) from the dataset entirely —
                // same as the rollback base — so a fixture's reference_tables rows neither
                // truncate nor REFRESH over the Liquibase seed.
                IDataSet dataset = new FilteredDataSet(new ExcludeTableFilter(PROTECTED_SEED_TABLES),
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
