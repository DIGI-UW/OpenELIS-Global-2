package org.openelisglobal.menu.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Menu Management cannot edit a module's in-memory entries: the save is refused
 * with a message naming Role Management, while ordinary entries save as before.
 */
public class MenuControllerModuleEntriesTest {

    private static final String CONTRIBUTED = "menu_mod_alpha";

    private static MenuItem item(String id, String elementId, MenuItem... children) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setElementId(elementId);
        menu.setIsActive(false);
        MenuItem item = new MenuItem();
        item.setMenu(menu);
        item.setChildMenus(List.of(children));
        return item;
    }

    @After
    public void dropTheContributedEntry() {
        MenuUtil.removeMenu(CONTRIBUTED);
        assertFalse(MenuUtil.isModuleEntry(CONTRIBUTED));
    }

    @Test
    public void aModuleEntryIsRefusedEvenWhenNested() {
        Menu parent = new Menu();
        parent.setId("2");
        parent.setElementId("menu_sample");
        Menu contributed = new Menu();
        contributed.setId("module:" + CONTRIBUTED);
        contributed.setElementId(CONTRIBUTED);
        contributed.setParent(parent);
        contributed.setIsActive(true);
        MenuUtil.addMenu(contributed);
        assertTrue(MenuUtil.isModuleEntry(CONTRIBUTED));

        try {
            MenuController.refuseModuleEntries(List.of(item("2", "menu_sample", item(null, CONTRIBUTED))));
            fail("expected the module entry to be refused");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            assertTrue(e.getReason(), e.getReason().contains(CONTRIBUTED));
            assertTrue(e.getReason(), e.getReason().contains("Role Management"));
        }
    }

    @Test
    public void ordinaryEntriesPass() {
        MenuController.refuseModuleEntries(List.of(item("2", "menu_sample", item("3", "menu_sample_add"))));
        MenuController.refuseModuleEntries(null);
    }
}
