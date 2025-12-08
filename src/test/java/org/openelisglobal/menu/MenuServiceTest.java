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

        Assert.assertNotNull(activeMenus);
        Assert.assertFalse(activeMenus.isEmpty());
        Assert.assertEquals(6, activeMenus.size());
        Assert.assertTrue(activeMenus.stream().allMatch(Menu::getIsActive));
    }
}
