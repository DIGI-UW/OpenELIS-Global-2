package org.openelisglobal.qaevent.criticalcallback;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The row chain a critical callback needs before it can exist: a test carrying
 * a 10–90 critical band on its default demographic row, a sample and sample
 * item to hang analyses off, then one analysis with a saved numeric result per
 * scenario. Three suites need it — the compute, the capture endpoint and the
 * quality-indicator breach evaluator — and each of them used to spell the same
 * seven inserts and seven deletes out in full.
 *
 * <p>
 * Seeded through JDBC rather than as a DbUnit dataset on purpose: the loader
 * truncates every table a dataset names, and the breach suite loads a second
 * dataset that also declares {@code analysis}, {@code sample} and
 * {@code result} — so a dataset here would wipe whichever of the two loaded
 * first.
 *
 * <p>
 * Each suite owns one id block, with the test, the result limit, the sample and
 * the sample item all keyed on {@code testId} and the analyses running from
 * there.
 */
public class CriticalResultFixture {

    private final JdbcTemplate jdbc;
    private final long testId;
    private final long firstAnalysisId;
    private final long lastAnalysisId;

    public CriticalResultFixture(JdbcTemplate jdbc, long testId, long firstAnalysisId, long lastAnalysisId) {
        this.jdbc = jdbc;
        this.testId = testId;
        this.firstAnalysisId = firstAnalysisId;
        this.lastAnalysisId = lastAnalysisId;
    }

    /**
     * @param testName        names the test in the detail rows
     * @param accessionPrefix prefixes the sample's accession number, which the
     *                        detail rows show as the lab number
     */
    public void seedTestWithCriticalBand(String testName, String accessionPrefix) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                testId, testName, testName + " desc", UUID.randomUUID().toString());
        // Default demographic row (blank gender, full age span) with a 10–90 critical
        // band — mirrors the shipped result_limits shape.
        jdbc.update(
                "INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                        + " low_critical, high_critical, lastupdated) VALUES (?, ?, 4, 0, ?, 10, 90, NOW())",
                testId, testId, Double.POSITIVE_INFINITY);
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                + " lastupdated) VALUES (?, ?, NOW(), NOW(), false, NOW())", testId, accessionPrefix + testId);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                + " VALUES (?, ?, 1, 1, NOW())", testId, testId);
    }

    /**
     * One analysis with a saved numeric result. The result takes the analysis's own
     * id, so a scenario is named by a single number.
     *
     * @param releasedAt {@code yyyy-MM-dd HH:mm:ss}, or null to leave the analysis
     *                   unreleased — the compute windows on the release date
     * @param value      the saved result; inside 10–90 is non-critical, outside it
     *                   is critical
     */
    public void seedResult(long analysisId, String releasedAt, String value) {
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, analysis_type, test_id, sampitem_id, released_date, lastupdated)"
                        + " VALUES (?, 'MANUAL', ?, ?, CAST(? AS timestamp), NOW())",
                analysisId, testId, testId, releasedAt);
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, lastupdated)"
                + " VALUES (?, ?, ?, 'N', NOW())", analysisId, analysisId, value);
    }

    /** One callback attempt against an analysis. */
    public void seedCallback(long analysisId, String status, String loggedAt, String recipient) {
        jdbc.update(
                "INSERT INTO clinlims.critical_callback (id, result_id, analysis_id, result_value, logged_by,"
                        + " logged_at, recipient_name, status, last_updated)"
                        + " VALUES (?, ?, ?, 'seeded', 1, CAST(? AS timestamp), ?, ?, NOW())",
                UUID.randomUUID().toString(), analysisId, analysisId, loggedAt, recipient, status);
    }

    public void deleteCallbacksFor(long analysisId) {
        jdbc.update("DELETE FROM clinlims.critical_callback WHERE analysis_id = ?", analysisId);
    }

    /**
     * Everything this fixture writes, children first. Safe to call before seeding.
     */
    public void clean() {
        jdbc.update("DELETE FROM clinlims.critical_callback WHERE analysis_id BETWEEN ? AND ?", firstAnalysisId,
                lastAnalysisId);
        jdbc.update("DELETE FROM clinlims.result WHERE id BETWEEN ? AND ?", firstAnalysisId, lastAnalysisId);
        jdbc.update("DELETE FROM clinlims.analysis WHERE id BETWEEN ? AND ?", firstAnalysisId, lastAnalysisId);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id = ?", testId);
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", testId);
        jdbc.update("DELETE FROM clinlims.result_limits WHERE id = ?", testId);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", testId);
    }
}
