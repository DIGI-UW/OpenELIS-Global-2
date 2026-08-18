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
 * OGC-609 [EQA V2 / T-12] — qa/019 lands the three-lane menu rows and the V2
 * permission tiers. Asserted against the liquibase-provisioned schema, so this
 * is the changeset's own regression test.
 */
public class EQAV2MenuPermissionIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String[][] MENUS = {
            { "menu_eqa_my_cycles", "/eqa/participant/cycles", "banner.menu.eqa.myCycles" },
            { "menu_eqa_lab_performance", "/eqa/oversight/lab-performance/coverage", "banner.menu.eqa.labPerformance" },
            { "menu_eqa_follow_up_queue", "/eqa/oversight/follow-up-queue", "banner.menu.eqa.followUpQueue" },
            { "menu_eqa_analyst_competency", "/eqa/oversight/analyst-track", "banner.menu.eqa.analystCompetency" },
            { "menu_eqa_provider", "/eqa/management/provider/schemes", "banner.menu.eqa.provider" } };

    private static final String[] TIERS = { "qa.eqa.participant", "qa.eqa.oversight", "qa.eqa.provider",
            "qa.eqa.inhouse.unblind" };

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUpJdbc() {
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    public void v2MenuRows_existUnderTheEqaMenuWithFrsRoutes() {
        for (String[] menu : MENUS) {
            Map<String, Object> row = jdbc.queryForMap("SELECT action_url, display_key, is_active,"
                    + " (SELECT element_id FROM clinlims.menu p WHERE p.id = m.parent_id) AS parent"
                    + " FROM clinlims.menu m WHERE element_id = ?", menu[0]);
            assertEquals(menu[0] + " route", menu[1], row.get("action_url"));
            assertEquals(menu[0] + " label key", menu[2], row.get("display_key"));
            assertEquals(menu[0] + " parent", "menu_eqa", row.get("parent"));
            assertEquals(menu[0] + " active", Boolean.TRUE, row.get("is_active"));
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
