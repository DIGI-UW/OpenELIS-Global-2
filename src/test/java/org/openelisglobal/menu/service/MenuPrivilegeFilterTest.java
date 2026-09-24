package org.openelisglobal.menu.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the OGC-1151 menu privilege filter: {@code GET /rest/menu} must omit
 * nodes whose target the caller holds no module grant for.
 *
 * <p>
 * The fixture is built additively over the migrated schema rather than through
 * a DBUnit dataset. A dataset naming {@code menu} or {@code system_module}
 * would be TRUNCATE-d CASCADE by the loader, which both destroys the seeded
 * privilege chain this filter reads and leaves later tests in the shared
 * container without their role grants. Every row inserted here carries a
 * reserved id and is removed again in {@link #removeFixture()}, so the test is
 * order-independent — notably it does not depend on the production menu tree,
 * which {@code MenuRestControllerTest} truncates.
 */
public class MenuPrivilegeFilterTest extends BaseWebContextSensitiveTest {

    private static final long FOLDER_ID = 99115101L;
    private static final long GRANTED_CHILD_ID = 99115102L;
    private static final long DENIED_CHILD_ID = 99115103L;
    private static final long LONELY_FOLDER_ID = 99115104L;
    private static final long PLACEHOLDER_ID = 99115105L;
    private static final long LONELY_FOLDER_CHILD_ID = 99115106L;

    private static final String FOLDER = "ogc1151_folder";
    private static final String GRANTED_CHILD = "ogc1151_child_unmapped";
    private static final String DENIED_CHILD = "ogc1151_child_restricted";
    private static final String LONELY_FOLDER = "ogc1151_folder_all_denied";
    private static final String PLACEHOLDER = "ogc1151_placeholder";

    private static final String RESTRICTED_URL = "/Ogc1151Restricted";
    private static final String UNMAPPED_URL = "/Ogc1151Unmapped";

    private static final long MODULE_ID = 99115110L;
    private static final String MODULE_NAME = "Ogc1151RestrictedModule";
    private static final long MODULE_URL_ID = 99115111L;
    private static final long ROLE_ID = 99115120L;
    private static final String ROLE_NAME = "OGC1151 Bench";
    private static final long ROLE_MODULE_ID = 99115121L;
    private static final long USER_ID = 99115130L;
    private static final String LOGIN_NAME = "ogc1151_bench";

    @Before
    public void insertFixture() throws Exception {
        removeFixture();

        // A folder holding one node with no policy (must survive) and one node
        // pointing at a module the role is not granted (must be removed).
        insertMenu(FOLDER_ID, null, 1, FOLDER, null);
        insertMenu(GRANTED_CHILD_ID, FOLDER_ID, 1, GRANTED_CHILD, UNMAPPED_URL);
        insertMenu(DENIED_CHILD_ID, FOLDER_ID, 2, DENIED_CHILD, RESTRICTED_URL);

        // A folder whose only child is removed, so the folder goes with it.
        insertMenu(LONELY_FOLDER_ID, null, 2, LONELY_FOLDER, null);
        insertMenu(LONELY_FOLDER_CHILD_ID, LONELY_FOLDER_ID, 1, DENIED_CHILD + "_2", RESTRICTED_URL);

        // No url and no children: a placeholder whose target an admin supplies at
        // runtime (the menu_billing shape). Must survive.
        insertMenu(PLACEHOLDER_ID, null, 3, PLACEHOLDER, null);

        jdbcTemplate.update("INSERT INTO system_module (id, name, description, has_select_flag)"
                + " VALUES (?, ?, 'OGC-1151 test module', 'Y')", MODULE_ID, MODULE_NAME);
        jdbcTemplate.update("INSERT INTO system_module_url (id, url_path, system_module_id) VALUES (?, ?, ?)",
                MODULE_URL_ID, RESTRICTED_URL, MODULE_ID);

        jdbcTemplate.update("INSERT INTO system_role (id, name, description, active) VALUES (?, ?, ?, true)", ROLE_ID,
                ROLE_NAME, "OGC-1151 test role");
        jdbcTemplate.update("INSERT INTO system_user (id, login_name, last_name, first_name, initials,"
                + " is_active, is_employee, lastupdated) VALUES (?, ?, 'Bench', 'OGC1151', 'OB', 'Y', 'Y', now())",
                USER_ID, LOGIN_NAME);
        jdbcTemplate.update("INSERT INTO system_user_role (system_user_id, role_id) VALUES (?, ?)", USER_ID, ROLE_ID);

        MenuUtil.forceRebuild();
        authenticateAsBenchUser();
    }

    @After
    public void removeFixture() {
        jdbcTemplate.update("DELETE FROM system_role_module WHERE id = ?", ROLE_MODULE_ID);
        jdbcTemplate.update("DELETE FROM system_user_role WHERE system_user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM system_user WHERE id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM system_role WHERE id = ?", ROLE_ID);
        jdbcTemplate.update("DELETE FROM system_module_url WHERE id = ?", MODULE_URL_ID);
        jdbcTemplate.update("DELETE FROM system_module WHERE id = ?", MODULE_ID);
        jdbcTemplate.update("DELETE FROM menu WHERE element_id LIKE 'ogc1151_%'");
        MenuUtil.forceRebuild();
    }

    @Test
    public void getMenuTree_omitsUnprivilegedNodesAndRestoresThemWhenTheModuleIsGranted() throws Exception {
        Set<String> visible = fetchMenuElementIds();

        assertFalse("a node whose module the role lacks must not be returned", visible.contains(DENIED_CHILD));
        assertFalse("a folder left with no surviving child must be pruned", visible.contains(LONELY_FOLDER));
        assertTrue("a node with no declared policy must stay visible", visible.contains(GRANTED_CHILD));
        assertTrue("a folder keeping one surviving child must stay visible", visible.contains(FOLDER));
        assertTrue("a childless node with no url is a placeholder and must stay visible",
                visible.contains(PLACEHOLDER));

        // Inversion: the filter must be reading the grant data, not a hardcoded list.
        jdbcTemplate.update("INSERT INTO system_role_module (id, has_select, system_role_id, system_module_id)"
                + " VALUES (?, 'Y', ?, ?)", ROLE_MODULE_ID, ROLE_ID, MODULE_ID);

        Set<String> afterGrant = fetchMenuElementIds();

        assertTrue("granting the module must restore the node", afterGrant.contains(DENIED_CHILD));
        assertTrue("granting the module must restore its pruned parent", afterGrant.contains(LONELY_FOLDER));
    }

    @Test
    public void getMenuTree_returnsEveryNodeForAnAdminAuthority() throws Exception {
        setDefaultTestAuthentication();

        Set<String> visible = fetchMenuElementIds();

        assertEquals("an admin authority bypasses the filter entirely",
                collectElementIds(MenuUtil.getUnfilteredMenuTree()), visible);
        assertTrue("the restricted node is visible to an admin", visible.contains(DENIED_CHILD));
    }

    private void authenticateAsBenchUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(LOGIN_NAME, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_OGC1151_BENCH"))));
    }

    private void insertMenu(Long id, Long parentId, int order, String elementId, String actionUrl) {
        jdbcTemplate.update(
                "INSERT INTO menu (id, parent_id, presentation_order, element_id, action_url, display_key,"
                        + " new_window, is_active, hide_in_old_ui) VALUES (?, ?, ?, ?, ?, ?, false, true, false)",
                id, parentId, order, elementId, actionUrl, "banner.menu." + elementId);
    }

    private Set<String> fetchMenuElementIds() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/menu").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        List<MenuItem> tree = new ObjectMapper().readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<MenuItem>>() {
                });
        return collectElementIds(tree);
    }

    private Set<String> collectElementIds(List<MenuItem> tree) {
        Set<String> elementIds = new HashSet<>();
        collectElementIds(tree, elementIds);
        return elementIds;
    }

    private void collectElementIds(List<MenuItem> tree, Set<String> sink) {
        for (MenuItem menuItem : tree) {
            sink.add(menuItem.getMenu().getElementId());
            collectElementIds(menuItem.getChildMenus(), sink);
        }
    }
}
