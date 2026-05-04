package org.openelisglobal.menu;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.beans.factory.annotation.Autowired;

public class MenuServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MenuService menuService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/menu.xml");
    }

    @Test
    public void saveSingleMenuItem_shouldSaveAndReturnMenuItem() {
        MenuItem menuItem = new MenuItem();
        Menu menu = new Menu();
        menu.setElementId("testElement4");
        menuItem.setMenu(menu);

        MenuItem savedItem = menuService.save(menuItem);

        Assert.assertNotNull(savedItem);
        Assert.assertEquals("testElement4", savedItem.getMenu().getElementId());
    }

    @Test
    public void saveMultipleMenuItems_shouldSaveAndReturnMenuItems() {
        MenuItem menuItem1 = new MenuItem();
        Menu menu1 = new Menu();
        menu1.setElementId("testElement5");
        menuItem1.setMenu(menu1);

        MenuItem menuItem2 = new MenuItem();
        Menu menu2 = new Menu();
        menu2.setElementId("testElement6");
        menuItem2.setMenu(menu2);

        List<MenuItem> menuItems = List.of(menuItem1, menuItem2);
        List<MenuItem> savedItems = menuService.save(menuItems);

        Assert.assertNotNull(savedItems);
        Assert.assertEquals(2, savedItems.size());
    }

    @Test
    public void getAllActiveMenus_shouldReturnOnlyActiveMenus() {
        List<Menu> activeMenus = menuService.getAllActiveMenus();

        // testdata/menu.xml: 6 active lab rows (testElement1,3,4,5,7,8) +
        // 2 active admin rows (testAdminAlpha, testAdminBeta). The service
        // does not filter by nav_scope, so the total is 8.
        Assert.assertNotNull(activeMenus);
        Assert.assertFalse(activeMenus.isEmpty());
        Assert.assertEquals(8, activeMenus.size());
        Assert.assertTrue(activeMenus.stream().allMatch(Menu::getIsActive));
    }

    @Test
    public void saveSingleMenuItem_shouldPersistExistingMenuUpdates() {
        MenuItem menuItem = new MenuItem();
        Menu menu = new Menu();
        menu.setElementId("testElement2");
        menu.setActionURL("/billing");
        menu.setIsActive(true);
        menuItem.setMenu(menu);

        MenuItem savedItem = menuService.save(menuItem);
        Menu reloadedMenu = menuService.getMenuByElementId("testElement2");

        Assert.assertNotNull(savedItem);
        Assert.assertEquals("testElement2", savedItem.getMenu().getElementId());
        Assert.assertTrue(savedItem.getMenu().getIsActive());
        Assert.assertEquals("/billing", savedItem.getMenu().getActionURL());
        Assert.assertNotNull(reloadedMenu);
        Assert.assertTrue(reloadedMenu.getIsActive());
        Assert.assertEquals("/billing", reloadedMenu.getActionURL());
    }
}
