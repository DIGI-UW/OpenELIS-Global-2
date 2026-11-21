package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

/**
 * The CSV exports run their query on a session of their own. Each run must give
 * its connection back, on success and on a failing query, or about twenty runs
 * exhaust the pool and hang the application (OGC-1360).
 *
 * <p>
 * The test datasource does not pool: every session opens a connection of its
 * own and a released one is disconnected. Postgres retires the backend of a
 * disconnected client a moment after the close, so the count can lag a release
 * briefly. A leaked connection never closes, so it keeps the count up for good.
 */
public class CSVColumnBuilderConnectionReleaseTest extends BaseWebContextSensitiveTest {

    private static final int RUNS = 5;
    private static final Duration BACKEND_SETTLE = Duration.ofSeconds(5);

    private int openBackends() {
        // The test runs in a transaction, and Postgres caches this view per
        // transaction unless the snapshot is cleared.
        jdbcTemplate.queryForList("SELECT pg_stat_clear_snapshot()");
        return jdbcTemplate.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()",
                Integer.class);
    }

    // Idle connections from earlier test classes can close during a test, so
    // the count may fall below the baseline. Only a leak keeps it above.
    private void assertNoBackendLeaked(int before) throws InterruptedException {
        long deadline = System.nanoTime() + BACKEND_SETTLE.toNanos();
        int after = openBackends();
        while (after > before && System.nanoTime() < deadline) {
            Thread.sleep(50);
            after = openBackends();
        }
        assertTrue("open backends rose from " + before + " to " + after, after <= before);
    }

    private int baseline() throws Exception {
        CSVColumnBuilder warmUp = sampleBuilder("SELECT 1 AS one");
        warmUp.buildDataSource();
        warmUp.closeResultSet();
        return openBackends();
    }

    @Test
    public void sampleBuilder_releasesItsConnectionAfterEachRun() throws Exception {
        int before = baseline();

        for (int i = 0; i < RUNS; i++) {
            CSVColumnBuilder builder = sampleBuilder("SELECT id FROM sample");
            builder.buildDataSource();
            while (builder.next()) {
                // drain the rows the way runReport() does
            }
            builder.closeResultSet();
        }

        assertNoBackendLeaked(before);
    }

    @Test
    public void routineBuilder_releasesItsConnectionAfterEachRun() throws Exception {
        int before = baseline();

        for (int i = 0; i < RUNS; i++) {
            CSVRoutineColumnBuilder builder = routineBuilder("SELECT 1 AS one");
            builder.buildDataSource();
            assertTrue(builder.next());
            assertFalse(builder.next());
            builder.closeResultSet();
        }

        assertNoBackendLeaked(before);
    }

    @Test
    public void failingQuery_releasesItsConnection() throws Exception {
        int before = baseline();

        for (int i = 0; i < RUNS; i++) {
            CSVRoutineColumnBuilder routine = routineBuilder("SELECT no_such_column FROM sample");
            assertThrows(RuntimeException.class, routine::buildDataSource);
            CSVColumnBuilder sample = sampleBuilder("SELECT no_such_column FROM sample");
            assertThrows(RuntimeException.class, sample::buildDataSource);
        }

        assertNoBackendLeaked(before);
    }

    @Test
    public void closeResultSet_isSafeToRepeat() throws Exception {
        int before = baseline();
        CSVColumnBuilder builder = sampleBuilder("SELECT 1 AS one");
        builder.buildDataSource();
        builder.closeResultSet();
        builder.closeResultSet();

        assertNoBackendLeaked(before);
    }

    private static CSVRoutineColumnBuilder routineBuilder(String sql) {
        return new CSVRoutineColumnBuilder(null) {
            @Override
            public void makeSQL() {
                query = new StringBuilder(sql);
            }
        };
    }

    private static CSVColumnBuilder sampleBuilder(String sql) {
        return new CSVColumnBuilder(null) {
            @Override
            public void makeSQL() {
                query = new StringBuilder(sql);
            }
        };
    }
}
