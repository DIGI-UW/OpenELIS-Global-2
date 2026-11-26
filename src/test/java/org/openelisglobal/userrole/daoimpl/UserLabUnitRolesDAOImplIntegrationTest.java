package org.openelisglobal.userrole.daoimpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.userrole.dao.UserLabUnitRolesDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises {@link UserLabUnitRolesDAOImpl#getActiveUsersWithRoleOnLabUnit}
 * against a real Postgres (PR #4684 review, optional N+1 fix). The schema and
 * column names used by the query were read from the Liquibase changelog
 * ({@code 2.6.x.x/add_lab_roles_map_table.xml}) and the JPA entities, not
 * guessed; this is the test that proves that reading was actually right, not
 * just plausible — {@code LabUnitStaffServiceImplTest} already covers that the
 * query is called once instead of per candidate user, with a mock.
 *
 * <p>
 * {@code @Transactional} here (default {@code REQUIRED} propagation) overrides
 * {@link BaseWebContextSensitiveTest}'s class-level {@code NOT_SUPPORTED} and
 * restores the usual Spring Test behaviour: each test method runs in its own
 * transaction, rolled back afterwards, so the fixture below needs no manual
 * cleanup — exactly how {@code UserRoleServiceTest} (same service tree) already
 * does it.
 */
@Transactional
public class UserLabUnitRolesDAOImplIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String LAB_UNIT = "N1-TEST-UNIT";
    private static final String OTHER_LAB_UNIT = "N1-TEST-OTHER-UNIT";
    private static final String ALL_LAB_UNITS = "AllLabUnits";

    @Autowired
    private UserLabUnitRolesDAO userLabUnitRolesDAO;

    private String resultsRoleId;

    /** active, Results on {@link #LAB_UNIT} — must match. */
    private String onUnit;
    /** active, Results on AllLabUnits — must match. */
    private String onAllUnits;
    /** active, Reports (not Results) on {@link #LAB_UNIT} — must not match. */
    private String wrongRole;
    /** inactive, Results on {@link #LAB_UNIT} — must not match. */
    private String inactive;
    /** active, Results on a different unit — must not match. */
    private String onOtherUnit;

    @Before
    public void seedFixture() {
        resultsRoleId = ensureRoleId("Results");
        String reportsRoleId = ensureRoleId("Reports");

        onUnit = seedUser("n1test.onunit");
        onAllUnits = seedUser("n1test.onallunits");
        wrongRole = seedUser("n1test.wrongrole");
        inactive = seedUser("n1test.inactive");
        onOtherUnit = seedUser("n1test.onother");
        deactivate(inactive);

        grantLabUnitRole(onUnit, LAB_UNIT, resultsRoleId);
        grantLabUnitRole(onAllUnits, ALL_LAB_UNITS, resultsRoleId);
        grantLabUnitRole(wrongRole, LAB_UNIT, reportsRoleId);
        grantLabUnitRole(inactive, LAB_UNIT, resultsRoleId);
        grantLabUnitRole(onOtherUnit, OTHER_LAB_UNIT, resultsRoleId);
    }

    @Test
    public void getActiveUsersWithRoleOnLabUnit_matchesOnlyActiveUsersWithTheRoleOnTheUnitOrAllUnits() {
        List<IdValuePair> matches = userLabUnitRolesDAO.getActiveUsersWithRoleOnLabUnit(resultsRoleId, LAB_UNIT);
        Set<String> matchedIds = matches.stream().map(IdValuePair::getId).collect(Collectors.toSet());

        // Checked by membership, not set equality: the shared test database
        // also carries the seeded admin user, who already holds Results on
        // AllLabUnits and so legitimately matches every unit queried. The
        // fixture's own five users are what this join's correctness turns on.
        assertTrue("the unit's own grant must match", matchedIds.contains(onUnit));
        assertTrue("an AllLabUnits grant must match", matchedIds.contains(onAllUnits));
        assertFalse("the wrong role must not match", matchedIds.contains(wrongRole));
        assertFalse("an inactive user must not match", matchedIds.contains(inactive));
        assertFalse("a grant on a different unit must not match", matchedIds.contains(onOtherUnit));
    }

    private String ensureRoleId(String name) {
        // Idempotent insert-if-absent already on the base class; this fixture
        // only needs the id back.
        ensureSystemRole(name);
        return jdbcTemplate.queryForObject("SELECT cast(id as varchar) FROM clinlims.system_role WHERE name = ?",
                String.class, name);
    }

    private String seedUser(String loginName) {
        return jdbcTemplate.queryForObject("INSERT INTO clinlims.system_user "
                + "(id, external_id, login_name, last_name, first_name, initials, is_active, is_employee, lastupdated) "
                + "VALUES (nextval('clinlims.system_user_seq'), ?, ?, 'N1Test', 'User', 'XX', 'Y', 'Y', now()) "
                + "RETURNING cast(id as varchar)", String.class, loginName, loginName);
    }

    private void deactivate(String userId) {
        jdbcTemplate.update("UPDATE clinlims.system_user SET is_active = 'N' WHERE id = ?", Integer.parseInt(userId));
    }

    private void grantLabUnitRole(String userId, String labUnit, String roleId) {
        jdbcTemplate.update("INSERT INTO clinlims.user_lab_unit_roles (system_user_id, last_updated) "
                + "VALUES (?, now()) ON CONFLICT (system_user_id) DO NOTHING", Integer.parseInt(userId));
        String mapId = jdbcTemplate.queryForObject("INSERT INTO clinlims.lab_unit_role_map (lab_unit) VALUES (?) "
                + "RETURNING cast(lab_unit_role_map_id as varchar)", String.class, labUnit);
        jdbcTemplate.update("INSERT INTO clinlims.lab_roles (lab_unit_role_map_id, role) VALUES (?, ?)",
                Integer.parseInt(mapId), roleId);
        jdbcTemplate.update(
                "INSERT INTO clinlims.lab_unit_roles (system_user_id, lab_unit_role_map_id) " + "VALUES (?, ?)",
                Integer.parseInt(userId), Integer.parseInt(mapId));
    }
}
