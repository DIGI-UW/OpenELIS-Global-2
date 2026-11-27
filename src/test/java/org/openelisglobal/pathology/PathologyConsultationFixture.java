package org.openelisglobal.pathology;

import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.program.service.PathologyConsultationService;
import org.openelisglobal.program.service.PathologySampleService;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The people, reasons and diagnoses a second opinion needs, added for a test
 * and removed after it, with every second opinion and its audit entries: the
 * base fixture has no role rows, and no dataset names the reasons.
 *
 * <p>
 * Users 1002 and 1003 are pathologists (1002 asks, 1003 is asked), 1001 and
 * 1004 hold no role, and 1005 is a pathologist who is no longer active.
 */
public final class PathologyConsultationFixture implements AutoCloseable {

    public static final String TECHNICIAN = "1001";
    public static final String REQUESTER = "1002";
    public static final String CONSULTED = "1003";
    public static final String OUTSIDER = "1004";
    public static final String INACTIVE_PATHOLOGIST = "1005";

    /** The consulted pathologist's name as the refusals give it. */
    public static final String CONSULTED_NAME = "Smith,Mariam";

    public static final String DIFFICULT_DIAGNOSIS = "9401";
    public static final String OLD_REASON = "9402";

    /** A diagnosis of the conclusions category that carries no code. */
    public static final String UNCODED_DIAGNOSIS = "9105";

    private static final int[] REASONS_AND_UNCODED = { 9401, 9402, 9105 };
    private static final int[] PATHOLOGISTS = { 1002, 1003, 1005 };

    private static final int REASON_CATEGORY_ID = 9411;
    private static final int PATHOLOGIST_ROLE_ID = 9412;

    private final JdbcTemplate jdbcTemplate;
    private final PathologyFindingsDictionary findings;
    private final boolean addedReasonCategory;
    private final boolean addedPathologistRole;

    private PathologyConsultationFixture(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        removeRows();
        jdbcTemplate.update("INSERT INTO clinlims.system_user (id, external_id, login_name, first_name, last_name,"
                + " initials, is_active, is_employee, lastupdated)"
                + " VALUES (1005, 'EXT005', 'pathologist3', 'Ruth', 'Okello', 'RO', 'N', 'Y', now())");
        // The role ships with the application, and a dataset that names
        // system_role empties it in the same way as the dictionary tables.
        addedPathologistRole = jdbcTemplate.update(
                "INSERT INTO clinlims.system_role (id, name, description, active, editable)"
                        + " SELECT ?, ?, ?, true, false WHERE NOT EXISTS"
                        + " (SELECT 1 FROM clinlims.system_role WHERE name = ?)",
                PATHOLOGIST_ROLE_ID, Constants.ROLE_PATHOLOGIST, Constants.ROLE_PATHOLOGIST,
                Constants.ROLE_PATHOLOGIST) == 1;
        for (int user : PATHOLOGISTS) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.system_user_role (system_user_id, role_id)"
                            + " SELECT ?, id FROM clinlims.system_role WHERE name = ?",
                    user, Constants.ROLE_PATHOLOGIST);
        }
        // Liquibase makes the category, but a dataset that names the dictionary
        // tables empties them for every later test class in the same fork.
        addedReasonCategory = DictionaryRows.insertCategory(jdbcTemplate, REASON_CATEGORY_ID,
                PathologyConsultationService.REASON_CATEGORY, "Pathology - Second Opinion Reasons", "PathConsRs");
        int reasons = DictionaryRows.idOfCategory(jdbcTemplate, PathologyConsultationService.REASON_CATEGORY);
        DictionaryRows.insertEntry(jdbcTemplate, 9401, reasons, "Difficult diagnosis", null, true);
        DictionaryRows.insertEntry(jdbcTemplate, 9402, reasons, "Old reason", null, false);
        findings = PathologyFindingsDictionary.seed(jdbcTemplate);
        DictionaryRows.insertEntry(jdbcTemplate, 9105,
                DictionaryRows.idOfCategory(jdbcTemplate, PathologySampleService.CONCLUSION_CATEGORY), "Inflammation",
                null, true);
    }

    public static PathologyConsultationFixture seed(JdbcTemplate jdbcTemplate) {
        return new PathologyConsultationFixture(jdbcTemplate);
    }

    @Override
    public void close() {
        DictionaryRows.removeEntries(jdbcTemplate, 9105);
        findings.close();
        removeRows();
        if (addedReasonCategory) {
            DictionaryRows.removeCategory(jdbcTemplate, REASON_CATEGORY_ID);
        }
        if (addedPathologistRole) {
            jdbcTemplate.update("DELETE FROM clinlims.system_role WHERE id = ?", PATHOLOGIST_ROLE_ID);
        }
    }

    /**
     * Second opinions point at the case, the users and the reasons, so they go
     * first; then the rows this fixture adds.
     */
    private void removeRows() {
        jdbcTemplate.update(
                "DELETE FROM clinlims.history WHERE reference_table ="
                        + " (SELECT id FROM clinlims.reference_tables WHERE name = ?)",
                PathologyConsultationService.REFERENCE_TABLE);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_consultation");
        DictionaryRows.removeEntries(jdbcTemplate, REASONS_AND_UNCODED);
        jdbcTemplate.update(
                "DELETE FROM clinlims.system_user_role WHERE system_user_id IN (1002, 1003, 1005)"
                        + " AND role_id = (SELECT id FROM clinlims.system_role WHERE name = ?)",
                Constants.ROLE_PATHOLOGIST);
        jdbcTemplate.update("DELETE FROM clinlims.system_user WHERE id = 1005");
    }
}
