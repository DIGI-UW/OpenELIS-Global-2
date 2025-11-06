
package org.openelisglobal.menu;

import java.util.List;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.menu.service.AdminMenuItemService;
import org.openelisglobal.menu.valueholder.AdminMenuItem;
import org.springframework.beans.factory.annotation.Autowired;

public class AdminMenuItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AdminMenuItemService adminMenuItemService;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/menu.xml");
    }

    @Test
    public void getActiveItemsSorted_shouldReturnOnlyActiveSortedAdminMenus() {
        List<AdminMenuItem> activeItems = adminMenuItemService.getActiveItemsSorted();

        Assert.assertNotNull("Returned list should not be null", activeItems);
        Assert.assertFalse("Returned list should not be empty", activeItems.isEmpty());

        for (int i = 0; i < activeItems.size() - 1; i++) {
            String currentKey = MessageUtil.getMessage(activeItems.get(i).getMessageKey(), Locale.ENGLISH);
            String nextKey = MessageUtil.getMessage(activeItems.get(i + 1).getMessageKey(), Locale.ENGLISH);

            Assert.assertTrue("Sorting issue: " + currentKey + " should be before " + nextKey,
                    currentKey.compareTo(nextKey) <= 0);
        }
    }
}
