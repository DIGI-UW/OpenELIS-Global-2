package org.openelisglobal.pathology;

import java.sql.Timestamp;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reads and sets a pathology case's stored stage and a slide's label stamp
 * directly, and clears what its moves leave behind, for the service and the
 * controller test supports alike.
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

    /** When the slide's label was checked against its block, or null if never. */
    static Timestamp labelVerifiedAt(JdbcTemplate jdbcTemplate, int slideId) {
        return jdbcTemplate.queryForObject("SELECT label_verified_at FROM clinlims.pathology_slide WHERE id = ?",
                Timestamp.class, slideId);
    }

    /**
     * Stamps a slide's label as checked now by the given user, for a test whose
     * subject is how the stamp is read rather than how it was made.
     */
    static void stampLabel(JdbcTemplate jdbcTemplate, int slideId, String userId) {
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET label_verified_at = now(), label_verified_by = ?"
                + " WHERE id = ?", Integer.parseInt(userId), slideId);
    }

    /** Takes the stamp off a slide's label, as if it had never been checked. */
    static void clearLabelStamp(JdbcTemplate jdbcTemplate, int slideId) {
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET label_verified_at = NULL, label_verified_by = NULL"
                + " WHERE id = ?", slideId);
    }
}
