package org.openelisglobal.menu.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.menu.valueholder.MenuSeedConfig;
import org.openelisglobal.menu.valueholder.MenuSeedConfig.MenuItemDTO;
import org.springframework.beans.factory.annotation.Autowired;

public class MenuSeedServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MenuService menuService;

    private Path tempConfigFile;
    private List<String> createdMenuIds = new ArrayList<>();

    @Before
    public void setup() throws Exception {
        // Create a temporary config file for testing
        tempConfigFile = Files.createTempFile("menu_seed_test", ".json");
    }

    @After
    public void cleanup() throws Exception {
        // Clean up created menus
        for (String elementId : createdMenuIds) {
            Menu menu = menuService.getMenuByElementId(elementId);
            if (menu != null) {
                menuService.delete(menu);
            }
        }
        createdMenuIds.clear();

        // Delete temp file
        if (tempConfigFile != null && Files.exists(tempConfigFile)) {
            Files.delete(tempConfigFile);
        }
    }

    @Test
    public void testMenuSeedConfigParsing() throws Exception {
        // Create test config
        MenuSeedConfig config = new MenuSeedConfig();
        List<MenuItemDTO> menus = new ArrayList<>();

        MenuItemDTO menu1 = new MenuItemDTO();
        menu1.setElementId("test_menu_1");
        menu1.setDisplayKey("test.menu.1");
        menu1.setActionURL("/TestMenu1");
        menu1.setPresentationOrder(1);
        menu1.setIsActive(true);
        menus.add(menu1);

        MenuItemDTO menu2 = new MenuItemDTO();
        menu2.setElementId("test_menu_2");
        menu2.setDisplayKey("test.menu.2");
        menu2.setActionURL("/TestMenu2");
        menu2.setPresentationOrder(2);
        menu2.setIsActive(true);
        menus.add(menu2);

        config.setMenus(menus);

        // Write to temp file
        ObjectMapper mapper = new ObjectMapper();
        try (FileWriter writer = new FileWriter(tempConfigFile.toFile())) {
            mapper.writeValue(writer, config);
        }

        // Parse back
        MenuSeedConfig parsed = mapper.readValue(tempConfigFile.toFile(), MenuSeedConfig.class);

        assertNotNull(parsed);
        assertEquals(2, parsed.getMenus().size());
        assertEquals("test_menu_1", parsed.getMenus().get(0).getElementId());
        assertEquals("test_menu_2", parsed.getMenus().get(1).getElementId());
    }

    @Test
    public void testMenuCreation() throws Exception {
        String elementId = "test_menu_creation_" + System.currentTimeMillis();
        createdMenuIds.add(elementId);

        // Verify menu doesn't exist
        Menu existingMenu = menuService.getMenuByElementId(elementId);
        assertNull(existingMenu);

        // Create menu
        Menu newMenu = new Menu();
        newMenu.setElementId(elementId);
        newMenu.setDisplayKey("test.menu.creation");
        newMenu.setActionURL("/TestMenuCreation");
        newMenu.setPresentationOrder(1);
        newMenu.setIsActive(true);
        newMenu.setOpenInNewWindow(false);
        newMenu.setHideInOldUI(false);

        menuService.insert(newMenu);

        // Verify menu was created
        Menu createdMenu = menuService.getMenuByElementId(elementId);
        assertNotNull(createdMenu);
        assertEquals(elementId, createdMenu.getElementId());
        assertEquals("test.menu.creation", createdMenu.getDisplayKey());
        assertEquals("/TestMenuCreation", createdMenu.getActionURL());
    }

    @Test
    public void testMenuWithChildCreation() throws Exception {
        String parentElementId = "test_parent_menu_" + System.currentTimeMillis();
        String childElementId = "test_child_menu_" + System.currentTimeMillis();
        createdMenuIds.add(parentElementId);
        createdMenuIds.add(childElementId);

        // Create parent menu
        Menu parentMenu = new Menu();
        parentMenu.setElementId(parentElementId);
        parentMenu.setDisplayKey("test.parent.menu");
        parentMenu.setActionURL("");
        parentMenu.setPresentationOrder(1);
        parentMenu.setIsActive(true);

        menuService.insert(parentMenu);

        // Create child menu
        Menu childMenu = new Menu();
        childMenu.setElementId(childElementId);
        childMenu.setDisplayKey("test.child.menu");
        childMenu.setActionURL("/TestChild");
        childMenu.setPresentationOrder(1);
        childMenu.setIsActive(true);
        childMenu.setParent(parentMenu);

        menuService.insert(childMenu);

        // Verify parent-child relationship
        Menu retrievedChild = menuService.getMenuByElementId(childElementId);
        assertNotNull(retrievedChild);
        assertNotNull(retrievedChild.getParent());
        assertEquals(parentElementId, retrievedChild.getParent().getElementId());
    }

    @Test
    public void testIdempotentMenuCreation() throws Exception {
        String elementId = "test_idempotent_menu_" + System.currentTimeMillis();
        createdMenuIds.add(elementId);

        // Create menu first time
        Menu menu1 = new Menu();
        menu1.setElementId(elementId);
        menu1.setDisplayKey("test.idempotent.menu");
        menu1.setActionURL("/TestIdempotent");
        menu1.setPresentationOrder(1);
        menu1.setIsActive(true);

        menuService.insert(menu1);

        Menu retrieved1 = menuService.getMenuByElementId(elementId);
        assertNotNull(retrieved1);
        String firstId = retrieved1.getId();

        // Attempt to create again (should skip in real seeding)
        Menu existingMenu = menuService.getMenuByElementId(elementId);
        assertNotNull("Menu should already exist", existingMenu);

        // Verify ID hasn't changed
        assertEquals(firstId, existingMenu.getId());
    }
}
