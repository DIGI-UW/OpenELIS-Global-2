package org.openelisglobal.module.runtime.contributions;

import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.module.api.manifest.MenuEntry;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleStartupException;

/**
 * Merges a module's manifest menu entries into the in-memory menu tree the same
 * way the core materialises configured entries that have no database row
 * ({@code MenuUtil.addMenu}): nothing is written to the {@code menu} table and
 * the entries vanish when the module is removed. Menu Management refuses to
 * edit them, because an edit would not survive a restart; who sees an entry is
 * decided by its permission, in Role Management. The parent is an existing
 * element id (for example {@code menu_sample}); an entry may also hang under
 * another entry of the same module, so a module can contribute a sub-menu with
 * its own children.
 */
public class ModuleMenuContributor {

    private static final String LOG = "ModuleMenuContributor";

    private final MenuService menuService;

    public ModuleMenuContributor(MenuService menuService) {
        this.menuService = menuService;
    }

    public void contribute(ModuleRecord module) {
        ModuleManifest manifest = module.manifest();
        if (manifest.getMenu().isEmpty()) {
            return;
        }
        Map<String, Menu> ownEntries = new HashMap<>();
        for (MenuEntry entry : manifest.getMenu()) {
            Menu parent = ownEntries.get(entry.getParent());
            if (parent == null) {
                parent = menuService.getMenuByElementId(entry.getParent());
            }
            if (parent == null) {
                throw new ModuleStartupException(
                        "menu entry " + entry.getElementId() + " names parent '" + entry.getParent()
                                + "', which is neither a core menu element nor an earlier entry of" + " this module");
            }
            Menu menu = new Menu();
            menu.setId("module:" + entry.getElementId());
            menu.setElementId(entry.getElementId());
            menu.setParent(parent);
            menu.setDisplayKey(entry.getDisplayKey());
            menu.setToolTipKey(entry.getToolTipKey() == null ? entry.getDisplayKey() : entry.getToolTipKey());
            menu.setActionURL(entry.getActionURL() == null ? "" : entry.getActionURL());
            menu.setPresentationOrder(entry.getPresentationOrder());
            if (entry.getIcon() != null) {
                menu.setIcon(entry.getIcon());
            }
            menu.setOpenInNewWindow(false);
            menu.setHideInOldUI(true);
            MenuUtil.addMenu(menu);
            ownEntries.put(entry.getElementId(), menu);
            LogEvent.logInfo(LOG, "contribute",
                    "[module:" + module.id() + "] menu " + entry.getElementId() + " under " + entry.getParent());
        }
        MenuUtil.forceRebuild();
    }
}
