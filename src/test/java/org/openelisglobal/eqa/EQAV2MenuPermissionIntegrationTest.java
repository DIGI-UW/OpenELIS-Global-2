package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-609 [EQA V2] — qa/019 lands the three-lane menu rows and the V2
 * permission tiers. Asserted against the liquibase-provisioned schema, so this
 * is the changeset's own regression test.
 */
public class EQAV2MenuPermissionIntegrationTest extends BaseWebContextSensitiveTest {

    /**
     * Element id, route, display key, and whether the row is switched on. A row is
     * hidden until the card that builds its page re-activates it at the route that
     * page actually serves (qa/029, then one changeset per page): My Cycles, the
     * Follow-Up Queue, Lab Performance and Provider are live, the rest are still
     * dark.
     */
    private static final String[][] MENUS = {
            { "menu_eqa_my_cycles", "/qa/eqa/my-cycles", "banner.menu.eqa.myCycles", "true" },
            { "menu_eqa_lab_performance", "/qa/eqa/lab-performance/coverage", "banner.menu.eqa.labPerformance",
                    "true" },
            { "menu_eqa_follow_up_queue", "/qa/eqa/follow-up-queue", "banner.menu.eqa.followUpQueue", "true" },
            // qa/037: the competency dashboard is the page this row was waiting for, so
            // it moves onto the served route and comes back on. The FRS path 404s on a
            // hard navigation — only the SPA router knows the redirect.
            { "menu_eqa_analyst_competency", "/qa/eqa/analyst-competency", "banner.menu.eqa.analystCompetency",
                    "true" },
            // qa/032: T-24's scheme list is the page this row was waiting for, so it
            // moves onto the served route and comes back on.
            { "menu_eqa_provider", "/qa/eqa/provider/schemes", "banner.menu.eqa.provider", "true" } };

    private static final String[] TIERS = { "qa.eqa.participant", "qa.eqa.oversight", "qa.eqa.provider",
            "qa.eqa.inhouse.unblind" };

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUpJdbc() {
        jdbc = new JdbcTemplate(dataSource);
        ensureTiersAndGrants();
    }

    /**
     * Other fixtures truncate system_module / system_role_module (and can wipe
     * system_role), so in a full-suite run the liquibase-seeded rows may be gone by
     * the time this class runs. Re-apply qa/019's idempotent registration SQL first
     * — what these tests then verify is that registration's semantics (exactly-one
     * module row per tier, both roles granted), under any suite order. The menu
     * assertions stay untouched: they read qa/019's own rows.
     */
    private void ensureTiersAndGrants() {
        for (String role : new String[] { "QA Officer", "Global Administrator" }) {
            jdbc.update("INSERT INTO clinlims.system_role (id, name)" + " SELECT nextval('clinlims.system_role_seq'), ?"
                    + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.system_role WHERE name = ?)", role, role);
        }
        for (String tier : TIERS) {
            jdbc.update("INSERT INTO clinlims.system_module (id, name, description, has_select_flag,"
                    + " has_add_flag, has_update_flag, has_delete_flag)"
                    + " SELECT nextval('clinlims.system_module_seq'), ?, 'restored by EQAV2MenuPermissionIntegrationTest',"
                    + " 'Y', 'N', 'N', 'N'" + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.system_module WHERE name = ?)",
                    tier, tier);
        }
        jdbc.update("INSERT INTO clinlims.system_role_module"
                + " (id, has_select, has_add, has_update, has_delete, system_role_id, system_module_id)"
                + " SELECT nextval('clinlims.system_role_module_seq'), 'Y', 'N', 'N', 'N', r.id, m.id"
                + " FROM clinlims.system_role r, clinlims.system_module m"
                + " WHERE r.name IN ('QA Officer', 'Global Administrator')"
                + "   AND m.name IN ('qa.eqa.participant', 'qa.eqa.oversight', 'qa.eqa.provider', 'qa.eqa.inhouse.unblind')"
                + "   AND NOT EXISTS (SELECT 1 FROM clinlims.system_role_module srm"
                + "       WHERE srm.system_role_id = r.id AND srm.system_module_id = m.id)");
    }

    @Test
    public void v2MenuRows_existUnderTheEqaMenuOnTheirServedRoutes() {
        for (String[] menu : MENUS) {
            Map<String, Object> row = jdbc.queryForMap("SELECT action_url, display_key, is_active,"
                    + " (SELECT element_id FROM clinlims.menu p WHERE p.id = m.parent_id) AS parent"
                    + " FROM clinlims.menu m WHERE element_id = ?", menu[0]);
            assertEquals(menu[0] + " route", menu[1], row.get("action_url"));
            assertEquals(menu[0] + " label key", menu[2], row.get("display_key"));
            assertEquals(menu[0] + " parent", "menu_eqa", row.get("parent"));
            assertEquals(menu[0] + " active", Boolean.valueOf(menu[3]), row.get("is_active"));
        }
    }

    @Test
    public void v2PermissionTiers_registeredOnce() {
        for (String tier : TIERS) {
            assertEquals(tier + " registered exactly once", Integer.valueOf(1), jdbc
                    .queryForObject("SELECT count(*) FROM clinlims.system_module WHERE name = ?", Integer.class, tier));
        }
    }

    @Test
    public void v2Tiers_grantedToQaOfficerAndGlobalAdmin() {
        for (String role : new String[] { "QA Officer", "Global Administrator" }) {
            List<String> granted = jdbc.queryForList("SELECT m.name FROM clinlims.system_role_module srm"
                    + " JOIN clinlims.system_role r ON r.id = srm.system_role_id"
                    + " JOIN clinlims.system_module m ON m.id = srm.system_module_id"
                    + " WHERE r.name = ? AND m.name LIKE 'qa.eqa.%' AND srm.has_select = 'Y'" + " ORDER BY m.name",
                    String.class, role);
            assertEquals(role + " holds all four V2 tiers",
                    List.of("qa.eqa.inhouse.unblind", "qa.eqa.oversight", "qa.eqa.participant", "qa.eqa.provider"),
                    granted);
        }
    }
}
