package org.openelisglobal.menu.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void distributionMenu_offersEachOrderEntryDomainDirectlyUnderTheOrderMenu() throws Exception {
        // Strict: the deployed file must be one complete JSON document, not merely
        // start with one, since the application reads it leniently and would hide
        // trailing garbage.
        com.fasterxml.jackson.databind.JsonNode config = new com.fasterxml.jackson.databind.ObjectMapper()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readTree(new File("volume/menu/menu_config.json"));
        com.fasterxml.jackson.databind.JsonNode sample = null;
        for (com.fasterxml.jackson.databind.JsonNode node : config.get("includes")) {
            if ("menu_sample".equals(node.path("elementId").asText())) {
                sample = node;
            }
        }
        assertNotNull(sample);
        java.util.List<String> children = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode child : sample.get("childMenus")) {
            children.add(child.path("elementId").asText());
        }
        assertTrue(children.containsAll(
                java.util.List.of("menu_clinical_workflow", "menu_environmental_workflow", "menu_vector_workflow")));
        assertTrue("the generic parent is not something a deployment can select", !children.contains("menu_add_order"));
    }

    @Test
    public void presentationOverlayPreservesUnspecifiedDatabaseSettingsAndUnlistedMenus() throws Exception {
        Menu reports = menu("70", "menu_reports", 14);
        reports.setActionURL("/InstanceReports");
        reports.setDisplayKey("instance.reports");
        reports.setIsActive(false);
        reports.setOpenInNewWindow(true);
        Menu other = menu("80", "instance_extension", 15);
        List<Menu> menus = new ArrayList<>(List.of(reports, other));
        File config = writeConfig("""
                {"menus": [{"elementId": "menu_reports", "icon": "reports", "presentationStyle": "section"}]}
                """);
        MenuConfigurationLoader.loadConfiguredMenus(config, menus);
        Menu overlaid = findMenu(menus, "menu_reports");
        assertEquals("70", overlaid.getId());
        assertEquals("/InstanceReports", overlaid.getActionURL());
        assertEquals("instance.reports", overlaid.getDisplayKey());
        assertEquals(14, overlaid.getPresentationOrder());
        assertTrue(!overlaid.getIsActive());
        assertTrue(overlaid.isOpenInNewWindow());
        assertEquals("reports", overlaid.getIcon());
        assertEquals("section", overlaid.getPresentationStyle());
        assertEquals(null, reports.getIcon());
        assertTrue(findMenu(menus, "instance_extension") == other);
        assertEquals(2, menus.size());
    }

    @Test
    public void nestedConfigurationReorganizesExistingMenusWithoutChangingDatabaseObjects() throws Exception {
        Menu reports = menu("70", "menu_reports", 14);
        Menu routine = menu("71", "menu_reports_routine", 1);
        routine.setParent(reports);
        Menu status = menu("72", "menu_reports_status_patient", 1);
        status.setParent(routine);
        status.setActionURL("/Report?type=patient&report=patientCILNSP_vreduit");
        List<Menu> menus = new ArrayList<>(List.of(reports, routine, status));
        File config = writeConfig("""
                {"menus": [{"elementId": "menu_reports", "childMenus": [
                  {"elementId": "menu_reports_status_patient"},
                  {"elementId": "menu_reports_other", "displayKey": "reporting.menu.otherReports", "childMenus": [
                    {"elementId": "menu_reports_routine"}
                  ]}
                ]}]}
                """);
        MenuConfigurationLoader.loadConfiguredMenus(config, menus);
        assertEquals("menu_reports", findMenu(menus, "menu_reports_status_patient").getParent().getElementId());
        assertEquals("menu_reports_other", findMenu(menus, "menu_reports_routine").getParent().getElementId());
        assertEquals(status.getActionURL(), findMenu(menus, "menu_reports_status_patient").getActionURL());
        assertEquals("menu_reports_routine", status.getParent().getElementId());
        assertEquals("menu_reports", routine.getParent().getElementId());
        assertEquals(4, menus.size());
    }

    @Test
    public void reportingInstanceProfileGroupsWorkflowsAndKeepsLegacyReportsReachable() throws Exception {
        Menu reports = menu("70", "menu_reports", 14);
        Menu routine = menu("71", "menu_reports_routine", 1);
        routine.setParent(reports);
        Menu legacyReport = menu("72", "menu_legacy_report", 1);
        legacyReport.setParent(routine);
        legacyReport.setActionURL("/Report?report=legacy");
        List<Menu> menus = new ArrayList<>(List.of(reports, routine, legacyReport));
        MenuConfigurationLoader.loadConfiguredMenus(new File("projects/reporting-uat/menu/menu_config.json"), menus);

        assertEquals(4, menus.stream().filter(menu -> menu.getParent() == null).count());
        assertEquals("section", findMenu(menus, "menu_section_patient_orders").getPresentationStyle());
        assertEquals("reports", findMenu(menus, "menu_reports").getIcon());
        assertEquals("menu_section_patient_orders", findMenu(menus, "menu_sample_add").getParent().getElementId());
        assertEquals("menu_reports_other", findMenu(menus, "menu_reports_routine").getParent().getElementId());
        assertEquals("menu_reports_routine", findMenu(menus, "menu_legacy_report").getParent().getElementId());
        assertEquals("/Report?report=legacy", legacyReport.getActionURL());
        assertEquals("/reports/custom-data-export?view=queue", findMenu(menus, "menu_reports_queue").getActionURL());
        assertTrue(findMenu(menus, "menu_reports_patient_print_queue").getActionURL() == null);
        assertEquals(menus.size(), menus.stream().map(Menu::getElementId).distinct().count());
        String serialized = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(findMenu(menus, "menu_section_patient_orders"));
        assertTrue(serialized.contains("\"presentationStyle\":\"section\""));
        assertEquals("menu_reports", routine.getParent().getElementId());
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
