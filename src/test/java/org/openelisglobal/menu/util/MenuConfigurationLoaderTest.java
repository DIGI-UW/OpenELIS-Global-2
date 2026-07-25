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

    private Menu menu(String id, String elementId, int presentationOrder) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setElementId(elementId);
        menu.setPresentationOrder(presentationOrder);
        menu.setIsActive(true);
        return menu;
    }

    private File writeConfig(String contents) throws Exception {
        File file = temporaryFolder.newFile("menu_config.json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(contents);
        }
        return file;
    }
}
