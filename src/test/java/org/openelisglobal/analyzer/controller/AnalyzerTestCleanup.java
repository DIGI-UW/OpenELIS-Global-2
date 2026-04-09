package org.openelisglobal.analyzer.controller;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared cleanup utility for analyzer controller tests.
 *
 * Deletes all test-created analyzer rows and resyncs the analyzer_seq sequence.
 * Call in both @Before and @After to ensure isolation regardless of test
 * ordering.
 */
public final class AnalyzerTestCleanup {

    private AnalyzerTestCleanup() {
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
