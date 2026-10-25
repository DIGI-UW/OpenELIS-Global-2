package org.openelisglobal.menu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.menu.util.MenuConfigurationLoader;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies the menu configuration file to the menu rows. The file's
 * {@code menus} seed rows that are missing and set the fields they name on rows
 * nobody has edited in Admin > Menu; a row edited there keeps its edits. When
 * {@code org.openelisglobal.menu.configuration.autocreate} is on, the file's
 * {@code includes} or {@code excludes} deactivate the rows they leave out, so
 * the navigation is filtered once here rather than on every request.
 */
@Service
public class MenuConfigurationSeeder {

    public static final String FROM_DISTRIBUTION = "distribution";
    public static final String FROM_ADMIN = "admin";
    private static final String SYSTEM_USER_ID = "1";

    private final MenuService menuService;
    private final String configurationFile;
    private final boolean applySelection;

    public MenuConfigurationSeeder(MenuService menuService,
            @Value("${org.openelisglobal.menu.configuration.file:/var/lib/openelis-global/menu/menu_config.json}") String configurationFile,
            @Value("${org.openelisglobal.menu.configuration.autocreate:false}") boolean applySelection) {
        this.menuService = menuService;
        this.configurationFile = configurationFile;
        this.applySelection = applySelection;
    }

    @Transactional
    public void seed() {
        seed(new File(configurationFile), applySelection);
    }

    @Transactional
    public void seed(File configFile, boolean applySelection) {
        if (configFile == null || !configFile.isFile()) {
            return;
        }
        List<Menu> rows = menuService.getAll();
        Map<String, Menu> byElementId = new HashMap<>();
        for (Menu row : rows) {
            byElementId.put(row.getElementId(), row);
        }

        List<Menu> effective = new ArrayList<>(rows);
        MenuConfigurationLoader.loadConfiguredMenus(configFile, effective);
        effective.replaceAll(menu -> {
            Menu stored = byElementId.get(menu.getElementId());
            return stored != null && FROM_ADMIN.equals(stored.getConfigSource()) ? stored : menu;
        });
        if (applySelection) {
            applySelection(configFile, effective);
        }
        effective.stream().filter(menu -> menu.isConfigurationOnly() || !menu.getConfigurationFields().isEmpty())
                .sorted(Comparator.comparingInt(MenuConfigurationSeeder::depth))
                .forEach(menu -> apply(menu, byElementId));
    }

    private void apply(Menu configured, Map<String, Menu> byElementId) {
        Menu row = byElementId.get(configured.getElementId());
        Menu parent = configured.getParent() == null ? null : byElementId.get(configured.getParent().getElementId());
        if (row == null) {
            row = new Menu();
            row.setElementId(configured.getElementId());
            copy(configured, row, null);
            row.setParent(parent);
            row.setConfigSource(FROM_DISTRIBUTION);
            row.setSysUserId(SYSTEM_USER_ID);
            menuService.insert(row);
            byElementId.put(row.getElementId(), row);
            return;
        }
        if (FROM_ADMIN.equals(row.getConfigSource())) {
            return;
        }
        Menu storedState = snapshot(row);
        Set<String> fields = configured.getConfigurationFields();
        boolean changed = copy(configured, row, fields);
        if (fields.contains("parent") && !sameMenu(row.getParent(), parent)) {
            row.setParent(parent);
            changed = true;
        }
        if (changed) {
            row.setConfigSource(FROM_DISTRIBUTION);
            row.setSysUserId(SYSTEM_USER_ID);
            menuService.updateAgainst(row, storedState);
        }
    }

    /**
     * Copies the named fields, or all of them when {@code fields} is null; true
     * when one changed.
     */
    private static boolean copy(Menu from, Menu to, Set<String> fields) {
        boolean changed = false;
        if (named(fields, "actionURL") && !Objects.equals(to.getActionURL(), from.getActionURL())) {
            to.setActionURL(from.getActionURL());
            changed = true;
        }
        if (named(fields, "displayKey") && !Objects.equals(to.getDisplayKey(), from.getDisplayKey())) {
            to.setDisplayKey(from.getDisplayKey());
            changed = true;
        }
        if (named(fields, "toolTipKey") && !Objects.equals(to.getToolTipKey(), from.getToolTipKey())) {
            to.setToolTipKey(from.getToolTipKey());
            changed = true;
        }
        if (named(fields, "presentationOrder") && to.getPresentationOrder() != from.getPresentationOrder()) {
            to.setPresentationOrder(from.getPresentationOrder());
            changed = true;
        }
        if (named(fields, "openInNewWindow") && to.isOpenInNewWindow() != from.isOpenInNewWindow()) {
            to.setOpenInNewWindow(from.isOpenInNewWindow());
            changed = true;
        }
        if (named(fields, "isActive") && to.getIsActive() != from.getIsActive()) {
            to.setIsActive(from.getIsActive());
            changed = true;
        }
        if (named(fields, "hideInOldUI") && to.isHideInOldUI() != from.isHideInOldUI()) {
            to.setHideInOldUI(from.isHideInOldUI());
            changed = true;
        }
        if (named(fields, "presentationStyle")
                && !Objects.equals(to.getPresentationStyle(), from.getPresentationStyle())) {
            to.setPresentationStyle(from.getPresentationStyle());
            changed = true;
        }
        if (named(fields, "icon") && !Objects.equals(to.getIcon(), from.getIcon())) {
            to.setIcon(from.getIcon());
            changed = true;
        }
        return changed;
    }

    private static boolean named(Set<String> fields, String field) {
        return fields == null || fields.contains(field);
    }

    private static boolean sameMenu(Menu a, Menu b) {
        return a == null ? b == null : b != null && Objects.equals(a.getElementId(), b.getElementId());
    }

    private static int depth(Menu menu) {
        int depth = 0;
        for (Menu parent = menu.getParent(); parent != null && depth < 64; parent = parent.getParent()) {
            depth++;
        }
        return depth;
    }

    /**
     * Entries omitted by the file's selection, including entries added at runtime
     * rather than stored as rows. Empty when filtering is off or no selection rule
     * exists.
     */
    public Set<String> leftOut(List<MenuItem> tree) {
        return applySelection ? leftOut(new File(configurationFile), tree) : Set.of();
    }

    public Set<String> leftOut(File configFile, List<MenuItem> tree) {
        if (!configFile.isFile()) {
            return Set.of();
        }
        JsonNode config;
        try {
            config = new ObjectMapper().readTree(configFile);
        } catch (IOException e) {
            LogEvent.logError("Error reading menu config file: " + configFile.getAbsolutePath(), e);
            return Set.of();
        }
        List<MenuItem> selected = MenuUtil.filterByConfiguration(tree, config);
        if (selected == null) {
            return Set.of();
        }
        Set<String> kept = new HashSet<>();
        collect(selected, kept);
        Set<String> all = new HashSet<>();
        collect(tree, all);
        all.removeAll(kept);
        return all;
    }

    private void applySelection(File configFile, List<Menu> effective) {
        Set<String> leftOut = leftOut(configFile, tree(effective));
        effective.replaceAll(menu -> {
            if (!leftOut.contains(menu.getElementId()) || FROM_ADMIN.equals(menu.getConfigSource())) {
                return menu;
            }
            Menu selectedState = snapshot(menu);
            selectedState.setIsActive(false);
            Set<String> fields = new HashSet<>(menu.getConfigurationFields());
            fields.add("isActive");
            selectedState.setConfigurationFields(fields);
            return selectedState;
        });
    }

    private static Menu snapshot(Menu row) {
        Menu stored = new Menu();
        BeanUtils.copyProperties(row, stored);
        return stored;
    }

    private static List<MenuItem> tree(Iterable<Menu> rows) {
        Map<String, MenuItem> items = new HashMap<>();
        for (Menu row : rows) {
            MenuItem item = new MenuItem();
            item.setMenu(row);
            items.put(row.getElementId(), item);
        }
        List<MenuItem> roots = new ArrayList<>();
        for (MenuItem item : items.values()) {
            Menu parent = item.getMenu().getParent();
            MenuItem parentItem = parent == null ? null : items.get(parent.getElementId());
            if (parentItem == null) {
                roots.add(item);
            } else {
                parentItem.getChildMenus().add(item);
            }
        }
        return roots;
    }

    private static void collect(List<MenuItem> items, Set<String> elementIds) {
        for (MenuItem item : items) {
            elementIds.add(item.getMenu().getElementId());
            collect(item.getChildMenus(), elementIds);
        }
    }
}
