package org.openelisglobal.resultvalidation;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Grants system user 1 the Validation role on every lab unit, so the queue and
 * review endpoints (which filter rows through
 * {@code UserService.filterAnalysisResultsByLabUnitRoles}) serve the fixture's
 * rows to the test session.
 *
 * <p>
 * The grant is self-sufficient on purpose. The role id is resolved by name from
 * {@code system_role}, and several fixtures in the suite (permission-module,
 * role-module, role, user-role, ...) truncate that table and re-insert only
 * their own roles. Surefire's class order is filesystem-dependent, so whether
 * the seeded "Validation" row still exists when a validation test class runs
 * varies from machine to machine: when it is gone, {@code RoleServiceImpl}
 * returns a stub role with id -1, the lab-unit lookup matches nothing and every
 * row is filtered out (CI run 33876275249 on PR #4188). Re-creating the role
 * when it is missing, and re-pointing the map's roles at the current id, makes
 * the outcome independent of what ran before.
 */
final class ValidationLabUnitRoles {

    private static final int SYSTEM_USER_ID = 1;

    private ValidationLabUnitRoles() {
    }

    static void grantValidationOnAllLabUnits(JdbcTemplate jdbcTemplate, int labUnitRoleMapId) {
        jdbcTemplate.update("INSERT INTO clinlims.system_role (id, name, description, display_key, active, editable)"
                + " SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM clinlims.system_role), 'Validation',"
                + " 'A person who can validate results', 'role.validator', true, false"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.system_role WHERE name = 'Validation')");
        jdbcTemplate.update("INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) VALUES (?, NOW())"
                + " ON CONFLICT (system_user_id) DO NOTHING", SYSTEM_USER_ID);
        jdbcTemplate.update("INSERT INTO clinlims.lab_unit_role_map (lab_unit_role_map_id, lab_unit) VALUES (?,"
                + " 'AllLabUnits') ON CONFLICT (lab_unit_role_map_id) DO NOTHING", labUnitRoleMapId);
        jdbcTemplate.update("INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) VALUES (?, ?)"
                + " ON CONFLICT DO NOTHING", SYSTEM_USER_ID, labUnitRoleMapId);
        jdbcTemplate.update("DELETE FROM clinlims.lab_roles WHERE lab_unit_role_map_id = ?", labUnitRoleMapId);
        jdbcTemplate.update(
                "INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role)"
                        + " SELECT ?, CAST(id AS VARCHAR) FROM clinlims.system_role WHERE name = 'Validation'",
                labUnitRoleMapId);
    }
}
