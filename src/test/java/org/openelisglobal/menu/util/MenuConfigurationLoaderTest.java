package org.openelisglobal.menu.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.menu.valueholder.Menu;

public class MenuConfigurationLoaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadConfiguredMenus_shouldMaterializeChildUnderExistingParent() throws Exception {
        Menu existingMicrobiology = menu("17", "menu_microbiology", 3);
        List<Menu> menus = new ArrayList<>();
        menus.add(existingMicrobiology);

        File config = writeConfig("""
                {
                  "menus": [
                    {
                      "elementId": "menu_microbiology",
                      "displayKey": "sidenav.label.microbiology",
                      "hideInOldUI": true,
                      "childMenus": [
                        {
                          "elementId": "menu_microbiology_worklist",
                          "actionURL": "/Microbiology/worklist",
                          "displayKey": "sidenav.label.microbiology.worklist",
                          "presentationOrder": 1,
                          "hideInOldUI": true
                        }
                      ]
                    }
                  ]
                }
                """);

        List<Menu> configuredMenus = MenuConfigurationLoader.loadConfiguredMenus(config, menus);

        assertEquals(1, configuredMenus.size());
        assertEquals("sidenav.label.microbiology", menus.get(0).getDisplayKey());
        assertTrue(menus.get(0).isHideInOldUI());
        Menu worklist = configuredMenus.get(0);
        assertEquals("menu_microbiology_worklist", worklist.getElementId());
        assertEquals("/Microbiology/worklist", worklist.getActionURL());
        assertEquals("17", worklist.getParent().getId());
        assertTrue(worklist.isHideInOldUI());
    }

    @Test
    public void loadConfiguredMenus_shouldMaterializeAnEntireMenuWhenNoDatabaseRowExists() throws Exception {
        List<Menu> menus = new ArrayList<>();
        File config = writeConfig("""
                {
                  "menus": [
                    {
                      "elementId": "menu_microbiology",
                      "displayKey": "sidenav.label.microbiology",
                      "presentationOrder": 6,
                      "childMenus": [
                        {
                          "elementId": "menu_microbiology_worklist",
                          "actionURL": "/Microbiology/worklist",
                          "displayKey": "sidenav.label.microbiology.worklist",
                          "presentationOrder": 1
                        }
                      ]
                    }
                  ]
                }
                """);

        List<Menu> configuredMenus = MenuConfigurationLoader.loadConfiguredMenus(config, menus);

        assertEquals(2, configuredMenus.size());
        assertEquals("menu_microbiology", menus.get(0).getElementId());
        assertEquals("configuration:menu_microbiology", menus.get(0).getId());
        assertEquals("menu_microbiology", configuredMenus.get(1).getParent().getElementId());
    }

    @Test
    public void loadConfiguredMenus_shouldPreserveAdminDashboardWhenAddingConfiguredChildren() throws Exception {
        Menu existingAdministration = menu("110", "menu_administration", 110);
        existingAdministration.setActionURL("/MasterListsPage");
        existingAdministration.setDisplayKey("sidenav.label.admin");
        List<Menu> menus = new ArrayList<>();
        menus.add(existingAdministration);

        File config = writeConfig("""
                {
                  "menus": [
                    {
                      "elementId": "menu_administration",
                      "childMenus": [
                        {
                          "elementId": "menu_administration_dashboard",
                          "actionURL": "/MasterListsPage",
                          "displayKey": "admin.dashboard.title",
                          "presentationOrder": 1,
                          "hideInOldUI": true
                        },
                        {
                          "elementId": "menu_administration_stuck_analyzer_events",
                          "actionURL": "/AnalyzerResults?view=import-issues",
                          "displayKey": "analyzer.importIssues.events.title",
                          "presentationOrder": 2,
                          "hideInOldUI": true
                        }
                      ]
                    }
                  ]
                }
                """);

        List<Menu> configuredMenus = MenuConfigurationLoader.loadConfiguredMenus(config, menus);

        assertEquals(2, configuredMenus.size());
        assertEquals("/MasterListsPage", existingAdministration.getActionURL());
        assertEquals("menu_administration_dashboard", configuredMenus.get(0).getElementId());
        assertEquals("menu_administration", configuredMenus.get(0).getParent().getElementId());
        assertEquals("menu_administration_stuck_analyzer_events", configuredMenus.get(1).getElementId());
        assertEquals("/AnalyzerResults?view=import-issues", configuredMenus.get(1).getActionURL());
        assertTrue(configuredMenus.stream().allMatch(Menu::isHideInOldUI));
    }

    @Test
    public void distributionMenu_shouldExposeAdminDashboardAndStuckAnalyzerEvents() {
        Menu existingAdministration = menu("110", "menu_administration", 110);
        existingAdministration.setActionURL("/MasterListsPage");
        List<Menu> menus = new ArrayList<>();
        menus.add(existingAdministration);

        MenuConfigurationLoader.loadConfiguredMenus(new File("volume/menu/menu_config.json"), menus);

        Menu adminDashboard = findMenu(menus, "menu_administration_dashboard");
        Menu stuckAnalyzerEvents = findMenu(menus, "menu_administration_stuck_analyzer_events");
        assertEquals("/MasterListsPage", adminDashboard.getActionURL());
        assertEquals("menu_administration", adminDashboard.getParent().getElementId());
        assertEquals("/AnalyzerResults?view=import-issues", stuckAnalyzerEvents.getActionURL());
        assertEquals("menu_administration", stuckAnalyzerEvents.getParent().getElementId());
    }

    @Test
    public void distributionMenu_shouldExposeWhonetOnlyUnderReports() {
        Menu existingMicrobiology = menu("17", "menu_microbiology", 6);
        Menu existingReports = menu("70", "menu_reports", 14);
        List<Menu> menus = new ArrayList<>();
        menus.add(existingMicrobiology);
        menus.add(existingReports);

        MenuConfigurationLoader.loadConfiguredMenus(new File("volume/menu/menu_config.json"), menus);

        Menu whonet = findMenu(menus, "menu_microbiology_whonet");
        assertEquals("/Microbiology/whonet", whonet.getActionURL());
        assertEquals("menu_reports", whonet.getParent().getElementId());
        assertTrue(menus.stream().noneMatch(menu -> "menu_microbiology_whonet".equals(menu.getElementId())
                && menu.getParent() != null && "menu_microbiology".equals(menu.getParent().getElementId())));
    }

    private Menu menu(String id, String elementId, int presentationOrder) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setElementId(elementId);
        menu.setPresentationOrder(presentationOrder);
        menu.setIsActive(true);
        return menu;
    }

    private Menu findMenu(List<Menu> menus, String elementId) {
        return menus.stream().filter(menu -> elementId.equals(menu.getElementId())).findFirst()
                .orElseThrow(() -> new AssertionError("Missing configured menu " + elementId));
    }

    private File writeConfig(String contents) throws Exception {
        File file = temporaryFolder.newFile("menu_config.json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(contents);
        }
        return file;
    }
}
