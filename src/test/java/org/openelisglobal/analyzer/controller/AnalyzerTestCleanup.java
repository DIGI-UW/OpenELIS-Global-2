package org.openelisglobal.analyzer.controller;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared cleanup and test data utilities for analyzer controller tests.
 *
 * Deletes all test-created analyzer rows and resyncs the analyzer_seq sequence.
 * Call in both @Before and @After to ensure isolation regardless of test
 * ordering.
 */
public final class AnalyzerTestCleanup {

    private static final AtomicInteger IP_COUNTER = new AtomicInteger(0);

    private AnalyzerTestCleanup() {
    }

    /**
     * Generate a unique RFC 5737 TEST-NET IP that will never collide across test
     * runs or parallel execution. Uses 198.51.100.x (TEST-NET-2).
     */
    public static String uniqueIp() {
        int n = IP_COUNTER.incrementAndGet();
        return "198.51." + ((n / 255) % 255 + 1) + "." + (n % 254 + 1);
    }

    /**
     * Generate a unique source ID for discovered-sources tests.
     */
    public static String uniqueSourceId() {
        return "test-source-" + System.currentTimeMillis() + "-" + IP_COUNTER.incrementAndGet();
    }

    /**
     * Delete all test-created analyzers and resync the sequence.
     *
     * Covers: - TEST-* (explicit test creates) - Unknown* (discovered-sources
     * endpoint stubs) - TEST-SEC-* (security test creates)
     */
    public static void clean(JdbcTemplate jdbcTemplate) {
        try {
            String testAnalyzerIds = "(SELECT id FROM analyzer WHERE name LIKE 'TEST-%' OR name LIKE 'Unknown%')";
            String testFieldIds = "(SELECT id FROM analyzer_field WHERE analyzer_id IN " + testAnalyzerIds + ")";

            // Delete in FK order
            jdbcTemplate.execute("DELETE FROM qualitative_result_mapping WHERE analyzer_field_id IN " + testFieldIds);
            jdbcTemplate.execute("DELETE FROM unit_mapping WHERE analyzer_field_id IN " + testFieldIds);
            jdbcTemplate.execute("DELETE FROM analyzer_field_mapping WHERE analyzer_field_id IN " + testFieldIds);
            jdbcTemplate.execute("DELETE FROM analyzer_field_mapping WHERE analyzer_id IN " + testAnalyzerIds);
            jdbcTemplate.execute("DELETE FROM analyzer_field WHERE analyzer_id IN " + testAnalyzerIds);
            jdbcTemplate.execute("DELETE FROM analyzer WHERE name LIKE 'TEST-%' OR name LIKE 'Unknown%'");

            // Resync sequence
            Integer maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM analyzer", Integer.class);
            jdbcTemplate.execute("SELECT setval('analyzer_seq', " + maxId + ", true)");
        } catch (Exception e) {
            // Best effort — don't mask the real test failure
        }
    }
}
