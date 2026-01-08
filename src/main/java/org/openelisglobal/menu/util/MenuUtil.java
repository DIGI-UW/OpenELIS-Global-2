/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.menu.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginMenuService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.core.env.Environment;

public class MenuUtil {

    private static List<MenuItem> root;
    private static final List<Menu> insertedMenus = new ArrayList<>();
    private static final PluginMenuService pluginMenuService = PluginMenuService.getInstance();
    private static final MenuService menuService = SpringContext.getBean(MenuService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MENU_CONFIG_PATH = "/var/lib/openelis-global/menu/menu_config.json";
    private static final String MENU_CONFIG_AUTOCREATE_PROPERTY = "org.openelisglobal.menu.configuration.autocreate";

    /**
     * The intent of this method is to allow menu items to be added outside of the
     * database. Typically plugins
     *
     * @param menu The menu item to be added
     */
    public static void addMenu(Menu menu) {
        menu.setIsActive(true);
        insertedMenus.add(menu);
    }

    // Update Menu items added outside the database
    public static void updateMenu(Menu menu) {
        insertedMenus.forEach(insertedMenu -> {
            if (insertedMenu.getElementId().equals(menu.getElementId())) {
                insertedMenu.setActionURL(menu.getActionURL());
                insertedMenu.setIsActive(menu.getIsActive());
            }
        });
    }

    public static void forceRebuild() {
        root = null;
    }

    public static List<MenuItem> getMenuTree() {
        if (root == null) {
            createTree();
        }

        // Apply menu filtering if enabled
        if (isMenuFilteringEnabled()) {
            return filterMenuTree(root);
        }

        return root;
    }

    private static void createTree() {
        List<Menu> menuList = menuService.getAll();

        Map<String, Menu> idToMenuMap = new HashMap<>();

        for (Menu menu : menuList) {
            idToMenuMap.put(menu.getId(), menu);
        }
        for (Menu menu : insertedMenus) {
            if (idToMenuMap.get(menu.getId()) == null) {
                idToMenuMap.put(menu.getId(), menu);
            }
        }

        for (Menu menu : insertedMenus) {
            Menu parent = idToMenuMap.get(menu.getParent().getId());
            if (parent != null) {
                menu.setParent(parent);
            }
        }

        menuList.addAll(insertedMenus);

        Map<Menu, MenuItem> menuToMenuItemMap = new HashMap<>();

        for (Menu menu : menuList) {
            createMenuItems(menuToMenuItemMap, menu);
        }

        MenuItem rootWrapper = new MenuItem();

        for (Menu menu : menuList) {
            if (menu.getParent() == null) {
                rootWrapper.getChildMenus().add(menuToMenuItemMap.get(menu));
            } else {
                MenuItem menuItem = menuToMenuItemMap.get(menu.getParent());
                if (menuItem == null) {
                    LogEvent.logWarn("MenuUtil", "createTree",
                            "parent menu item is not active so can't attach child node " + menu.getElementId()
                                    + ". Continuing without child node");
                } else {
                    menuToMenuItemMap.get(menu.getParent()).getChildMenus().add(menuToMenuItemMap.get(menu));
                }
            }
        }

        sortChildren(rootWrapper);

        root = rootWrapper.getChildMenus();
    }

    private static void createMenuItems(Map<Menu, MenuItem> menuToMenuItemMap, Menu menu) {
        MenuItem menuItem = new MenuItem();
        menuItem.setMenu(menu);
        menuToMenuItemMap.put(menu, menuItem);
    }

    public static String getMenuAsHTML() {
        StringBuffer html = new StringBuffer();
        html.append("<ul class=\"nav-menu\" id=\"main-nav\" >\n");
        addChildMenuItems(html, getMenuTree(), true);
        html.append("</ul>");
        return html.toString();
    }

    private static void addChildMenuItems(StringBuffer html, List<MenuItem> menuTree, boolean topLevel) {
        String locale = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.DEFAULT_LANG_LOCALE);
        int topLevelCount = 0;
        for (MenuItem menuItem : menuTree) {
            Menu menu = menuItem.getMenu();
            if (menu.getIsActive() && !menu.isHideInOldUI()) {
                if (topLevel) {
                    if (topLevelCount == 0) {
                        html.append("\t<li id=\"nav-first\" >\n");
                    } else if (topLevelCount == menuTree.size() - 1) {
                        html.append("\t<li id=\"nav-last\" >\n");
                    } else {
                        html.append("\t<li>\n");
                    }

                    topLevelCount++;
                } else {
                    html.append("\t<li>\n");
                }

                html.append("\t\t<a ");
                html.append("id=\"");
                html.append(menu.getElementId());
                html.append("\" ");

                // tooltips disabled as they were unnecessary and distracting in the menu
                // if (!GenericValidator.isBlankOrNull(menu.getLocalizedTooltip())) {
                // html.append(" title=\"");
                // html.append(getTooltip(menu, locale));
                // html.append("\" ");
                // }

                if (menu.isOpenInNewWindow()) {
                    html.append(" target=\"_blank\" ");
                }

                if (GenericValidator.isBlankOrNull(menu.getActionURL())
                        && GenericValidator.isBlankOrNull(menu.getClickAction())) {
                    html.append(" class=\"no-link\" >");
                } else {
                    html.append(" href=\"");
                    String url = menu.getActionURL().startsWith("/")
                            ? menu.getActionURL().substring(1, menu.getActionURL().length())
                            : menu.getActionURL();
                    html.append(url);
                    html.append("\" >");
                }

                html.append(getLabel(menu, locale));
                html.append("</a>\n");

                if (!menuItem.getChildMenus().isEmpty()) {
                    html.append("<ul>\n");
                    addChildMenuItems(html, menuItem.getChildMenus(), false);
                    html.append("</ul>\n");
                }

                html.append("\t</li>\n");
            }
        }
    }

    @SuppressWarnings("unused")
    private static String getTooltip(Menu menu, String locale) {
        String key = menu.getToolTipKey();
        String value = pluginMenuService.getMenuLabel(locale, key);
        if (key != value) {
            return value;
        }

        return menu.getLocalizedTooltip();
    }

    private static String getLabel(Menu menu, String locale) {
        String key = menu.getDisplayKey();
        String value = pluginMenuService.getMenuLabel(locale, key);
        if (key != value) {
            return value;
        }

        return menu.getLocalizedTitle();
    }

    private static void sortChildren(MenuItem menuItem) {
        menuItem.sortChildren();

        for (MenuItem child : menuItem.getChildMenus()) {
            sortChildren(child);
        }
    }

    /**
     * Checks if menu filtering is enabled via configuration property. Uses Spring's
     * Environment to read the property, similar to @Value annotation.
     *
     * @return true if menu filtering should be applied
     */
    private static boolean isMenuFilteringEnabled() {
        try {
            // Use Spring's Environment to read the property (same way @Value works)
            Environment environment = SpringContext.getBean(Environment.class);
            String autocreateValue = environment.getProperty(MENU_CONFIG_AUTOCREATE_PROPERTY);
            return "true".equalsIgnoreCase(autocreateValue);
        } catch (Exception e) {
            // Fallback to ConfigurationProperties if Environment is not available
            LogEvent.logDebug("MenuUtil", "isMenuFilteringEnabled",
                    "Could not read property via Environment " + e.getMessage());

            return false;
        }
    }

    /**
     * Filters the menu tree based on the menu configuration file. Supports both
     * "includes" (whitelist) and "excludes" (blacklist) modes.
     *
     * @param menuTree The original menu tree
     * @return The filtered menu tree
     */
    private static List<MenuItem> filterMenuTree(List<MenuItem> menuTree) {
        try {
            File configFile = new File(MENU_CONFIG_PATH);
            if (!configFile.exists() || !configFile.isFile()) {
                LogEvent.logWarn("MenuUtil", "filterMenuTree",
                        "Menu config file not found at: " + MENU_CONFIG_PATH + ". Skipping menu filtering.");
                return menuTree;
            }

            JsonNode configNode = objectMapper.readTree(configFile);
            Set<String> elementIds = new HashSet<>();

            // Check for includes or excludes
            if (configNode.has("includes") && configNode.get("includes").isArray()) {
                extractElementIds(configNode.get("includes"), elementIds);
                return filterByIncludes(menuTree, elementIds);
            } else if (configNode.has("excludes") && configNode.get("excludes").isArray()) {
                extractElementIds(configNode.get("excludes"), elementIds);
                return filterByExcludes(menuTree, elementIds);
            } else {
                LogEvent.logWarn("MenuUtil", "filterMenuTree",
                        "Menu config file does not contain 'includes' or 'excludes' array. Skipping menu filtering.");
                return menuTree;
            }
        } catch (IOException e) {
            LogEvent.logError("Error reading menu config file: " + MENU_CONFIG_PATH, e);
            return menuTree;
        }
    }

    /**
     * Recursively extracts element IDs from the JSON config structure.
     *
     * @param nodes      The JSON array of menu items
     * @param elementIds The set to populate with element IDs
     */
    private static void extractElementIds(JsonNode nodes, Set<String> elementIds) {
        if (nodes == null || !nodes.isArray()) {
            return;
        }

        for (JsonNode node : nodes) {
            if (node.has("elementId") && node.get("elementId").isTextual()) {
                String elementId = node.get("elementId").asText();
                if (!GenericValidator.isBlankOrNull(elementId)) {
                    elementIds.add(elementId);
                }
            }

            // Recursively process child menus
            if (node.has("childMenus") && node.get("childMenus").isArray()) {
                extractElementIds(node.get("childMenus"), elementIds);
            }
        }
    }

    /**
     * Filters menu tree to include only specified menu items and their children.
     *
     * @param menuTree The original menu tree
     * @param includes The set of element IDs to include
     * @return The filtered menu tree containing only included items
     */
    private static List<MenuItem> filterByIncludes(List<MenuItem> menuTree, Set<String> includes) {
        List<MenuItem> filtered = new ArrayList<>();

        for (MenuItem menuItem : menuTree) {
            String elementId = menuItem.getMenu().getElementId();
            if (includes.contains(elementId)) {
                // Include this item and recursively filter its children
                MenuItem filteredItem = new MenuItem();
                filteredItem.setMenu(menuItem.getMenu());
                filteredItem.setChildMenus(filterByIncludes(menuItem.getChildMenus(), includes));
                filtered.add(filteredItem);
            } else {
                // Check if any child is included - if so, include this parent but filter
                // children
                List<MenuItem> filteredChildren = filterByIncludes(menuItem.getChildMenus(), includes);
                if (!filteredChildren.isEmpty()) {
                    MenuItem filteredItem = new MenuItem();
                    filteredItem.setMenu(menuItem.getMenu());
                    filteredItem.setChildMenus(filteredChildren);
                    filtered.add(filteredItem);
                }
            }
        }

        return filtered;
    }

    /**
     * Filters menu tree to exclude specified menu items and their children.
     *
     * @param menuTree The original menu tree
     * @param excludes The set of element IDs to exclude
     * @return The filtered menu tree with excluded items removed
     */
    private static List<MenuItem> filterByExcludes(List<MenuItem> menuTree, Set<String> excludes) {
        List<MenuItem> filtered = new ArrayList<>();

        for (MenuItem menuItem : menuTree) {
            String elementId = menuItem.getMenu().getElementId();
            if (!excludes.contains(elementId)) {
                // Not excluded - include this item and recursively filter its children
                MenuItem filteredItem = new MenuItem();
                filteredItem.setMenu(menuItem.getMenu());
                filteredItem.setChildMenus(filterByExcludes(menuItem.getChildMenus(), excludes));
                filtered.add(filteredItem);
            }
            // If elementId is in excludes, skip this item and all its children
        }

        return filtered;
    }
}
