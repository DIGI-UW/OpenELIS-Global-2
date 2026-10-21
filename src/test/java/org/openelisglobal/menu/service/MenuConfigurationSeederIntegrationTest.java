package org.openelisglobal.menu.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.menu.util.MenuConfigurationExport;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class MenuConfigurationSeederIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ROOT = "menu_seed_test_root";
    private static final String HIDDEN = "menu_seed_test_hidden";
    private static final String SEEDED = "menu_seed_test_seeded";

    @Autowired
    private DataSource dataSource;
    @Autowired
    private MenuConfigurationSeeder seeder;
    @Autowired
    private MenuService menuService;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update("INSERT INTO clinlims.menu (id, element_id, display_key, presentation_order, is_active,"
                + " new_window, hide_in_old_ui) VALUES (nextval('clinlims.menu_seq'), ?,"
                + " 'seed.test.root', 900, true, false, false)", ROOT);
        jdbc.update("INSERT INTO clinlims.menu (id, parent_id, element_id, display_key, action_url,"
                + " presentation_order, is_active, new_window, hide_in_old_ui) SELECT"
                + " nextval('clinlims.menu_seq'), id, ?, 'seed.test.hidden', '/SeedTestHidden', 1, true, false,"
                + " false FROM clinlims.menu WHERE element_id = ?", HIDDEN, ROOT);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.menu WHERE element_id IN (?, ?)", HIDDEN, SEEDED);
        jdbc.update("DELETE FROM clinlims.menu WHERE element_id = ?", ROOT);
    }

    private File config(String json) throws Exception {
        File file = File.createTempFile("menu_config", ".json");
        file.deleteOnExit();
        Files.writeString(file.toPath(), json);
        return file;
    }

    private boolean active(String elementId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT is_active FROM clinlims.menu WHERE element_id = ?",
                Boolean.class, elementId));
    }

    @Test
    public void configuredUpdatesAndDeactivationAreAuditedButRepeatStartupIsNot() throws Exception {
        File file = config("{\"menus\": [{\"elementId\": \"" + HIDDEN
                + "\", \"isActive\": true, \"actionURL\": \"/ChangedByConfiguration\"}], \"excludes\": [{\"elementId\": \""
                + HIDDEN + "\"}]}");
        int before = historyCount();
        seeder.seed(file, true);
        assertEquals(before + 1, historyCount());
        List<byte[]> changes = jdbc.queryForList("SELECT h.changes FROM clinlims.history h"
                + " JOIN clinlims.reference_tables r ON r.id = h.reference_table"
                + " JOIN clinlims.menu m ON m.id = h.reference_id"
                + " WHERE lower(r.name) = 'menu' AND m.element_id = ? ORDER BY h.id", byte[].class, HIDDEN);
        String recorded = changes.stream().map(bytes -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
                .collect(java.util.stream.Collectors.joining());
        assertTrue(recorded, recorded.contains("/SeedTestHidden"));
        assertTrue(recorded, recorded.contains("isActive"));
        seeder.seed(file, true);
        assertEquals("unchanged configuration must not create audit noise", before + 1, historyCount());
    }

    @Test
    public void activeDefinitionOmittedFromIncludesIsSavedOnce() throws Exception {
        File file = config("{\"menus\": [{\"elementId\": \"" + HIDDEN
                + "\", \"isActive\": true}], \"includes\": [{\"elementId\": \"" + ROOT + "\"}]}");
        int before = historyCount();
        seeder.seed(file, true);
        assertFalse(active(HIDDEN));
        assertEquals(before + 1, historyCount());
        seeder.seed(file, true);
        assertFalse(active(HIDDEN));
        assertEquals("unchanged selection must not create audit noise", before + 1, historyCount());
    }

    private int historyCount() {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.history h"
                + " JOIN clinlims.reference_tables r ON r.id = h.reference_table"
                + " JOIN clinlims.menu m ON m.id = h.reference_id"
                + " WHERE lower(r.name) = 'menu' AND m.element_id = ?", Integer.class, HIDDEN);
    }

    @Test
    public void anItemTheFileHidesThenEnabledInAdminStaysShownWhenSeededAgain() throws Exception {
        File file = config("{\"excludes\": [{\"elementId\": \"" + HIDDEN + "\"}]}");

        seeder.seed(file, true);
        assertFalse("the file's exclude is written into the row", active(HIDDEN));

        Menu hidden = menuService.getMenuByElementId(HIDDEN);
        hidden.setIsActive(true);
        MenuItem edit = new MenuItem();
        edit.setMenu(hidden);
        menuService.save(edit);

        seeder.seed(file, true);
        assertTrue("an Admin edit wins over the file", active(HIDDEN));
    }

    @Test
    public void anEntryAProgramAddsAtRuntimeTakesTheFilesSelectionToo() throws Exception {
        Menu runtime = new Menu();
        runtime.setElementId("menu_seed_test_runtime");
        MenuItem runtimeItem = new MenuItem();
        runtimeItem.setMenu(runtime);
        MenuItem hidden = new MenuItem();
        hidden.setMenu(menuService.getMenuByElementId(HIDDEN));
        MenuItem root = new MenuItem();
        root.setMenu(menuService.getMenuByElementId(ROOT));
        root.getChildMenus().add(hidden);
        root.getChildMenus().add(runtimeItem);

        assertEquals(java.util.Set.of("menu_seed_test_runtime"),
                seeder.leftOut(config("{\"excludes\": [{\"elementId\": \"menu_seed_test_runtime\"}]}"), List.of(root)));
    }

    @Test
    public void theFileSelectionIsIgnoredWhenFilteringIsOff() throws Exception {
        seeder.seed(config("{\"excludes\": [{\"elementId\": \"" + HIDDEN + "\"}]}"), false);

        assertTrue(active(HIDDEN));
    }

    @Test
    public void aMenuMissingFromTheDatabaseIsSeededFromTheFileOnce() throws Exception {
        File file = config("{\"menus\": [{\"elementId\": \"" + ROOT + "\", \"childMenus\": [{\"elementId\": \"" + SEEDED
                + "\", \"displayKey\": \"seed.test.seeded\", \"actionURL\": \"/SeedTestSeeded\","
                + " \"presentationOrder\": 2}]}]}");

        seeder.seed(file, false);
        seeder.seed(file, false);

        List<String> parents = jdbc.queryForList("SELECT p.element_id FROM clinlims.menu m JOIN clinlims.menu p"
                + " ON p.id = m.parent_id WHERE m.element_id = ?", String.class, SEEDED);
        assertEquals(List.of(ROOT), parents);
        assertEquals("/SeedTestSeeded",
                jdbc.queryForObject("SELECT action_url FROM clinlims.menu WHERE element_id = ?", String.class, SEEDED));
    }

    @Test
    public void theFileDoesNotChangeARowEditedInAdmin() throws Exception {
        jdbc.update("UPDATE clinlims.menu SET action_url = '/EditedHere', config_source = 'admin' WHERE element_id = ?",
                HIDDEN);

        seeder.seed(config("{\"menus\": [{\"elementId\": \"" + HIDDEN + "\", \"actionURL\": \"/FromFile\"}]}"), false);

        assertEquals("/EditedHere",
                jdbc.queryForObject("SELECT action_url FROM clinlims.menu WHERE element_id = ?", String.class, HIDDEN));
    }

    @Test
    public void anExportedMenuSeedsTheSameNavigationBack() throws Exception {
        MenuItem hidden = new MenuItem();
        hidden.setMenu(menuService.getMenuByElementId(HIDDEN));
        MenuItem root = new MenuItem();
        root.setMenu(menuService.getMenuByElementId(ROOT));
        root.getChildMenus().add(hidden);
        File exported = config(MenuConfigurationExport.of(List.of(root)).toString());
        jdbc.update("UPDATE clinlims.menu SET is_active = false, action_url = '/Drifted' WHERE element_id = ?", HIDDEN);

        seeder.seed(exported, false);

        assertTrue(active(HIDDEN));
        assertEquals("/SeedTestHidden",
                jdbc.queryForObject("SELECT action_url FROM clinlims.menu WHERE element_id = ?", String.class, HIDDEN));
    }
}
