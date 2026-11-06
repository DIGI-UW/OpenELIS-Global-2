package org.openelisglobal.pathology;

import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reads and sets a pathology case's stored stage directly, and clears what its
 * moves leave behind, for the service and the controller test supports alike.
 */
final class PathologyCaseRows {

    private PathologyCaseRows() {
    }

    static String storedStage(JdbcTemplate jdbcTemplate, int caseId) {
        return jdbcTemplate.queryForObject("SELECT status FROM clinlims.pathology_sample WHERE id = ?", String.class,
                caseId);
    }

    /**
     * Puts the case at a stage directly, for a test whose subject is not how it got
     * there; it leaves no stage event, as a case on file before the upgrade has
     * none.
     */
    static void storeAt(JdbcTemplate jdbcTemplate, int caseId, PathologyStatus stage) {
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET status = ? WHERE id = ?", stage.name(), caseId);
    }

    /**
     * The case's stage events and its own audit entries, which survive the fixture
     * reload.
     */
    static void clearStageHistory(JdbcTemplate jdbcTemplate, int caseId) {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ?", caseId);
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_id = ? AND reference_table ="
                + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_SAMPLE')", caseId);
    }
}
