package org.openelisglobal.menu;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.beans.factory.annotation.Autowired;

public class GeneratedMenuServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MenuService menuService;

    @Before
    public void loadMenuFixture() throws Exception {
        executeDataSetWithStateManagement("testdata/menu.xml");
    }

    @Test
    public void getAllActiveMenus_shouldReturnExactActiveFixtureRows() {
        List<Menu> activeMenus = menuService.getAllActiveMenus();
        Set<String> activeElementIds = activeMenus.stream().map(Menu::getElementId).collect(Collectors.toSet());

        Assert.assertEquals(6, activeMenus.size());
        Assert.assertEquals(
                Set.of(
                        "testElement1",
                        "testElement3",
                        "testElement4",
                        "testElement5",
                        "testElement7",
                        "testElement8"),
                activeElementIds);
        Assert.assertTrue(activeMenus.stream().allMatch(Menu::getIsActive));
    }
}
