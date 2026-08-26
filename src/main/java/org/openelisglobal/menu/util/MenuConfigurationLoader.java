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
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.menu.valueholder.Menu;

/**
 * Materializes distribution-owned menu definitions without persisting menu
 * rows.
 *
 * <p>
 * The mounted menu configuration remains the source of truth for distribution
 * navigation. Existing database-backed menu entries may be overlaid by element
 * ID; entries that do not exist in the database are supplied in memory for the
 * current application instance.
 */
public final class MenuConfigurationLoader {

    private static final String MENUS_FIELD = "menus";
    private static final String CHILD_MENUS_FIELD = "childMenus";
    private static final String ELEMENT_ID_FIELD = "elementId";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MenuConfigurationLoader() {
    }

    public static List<Menu> loadConfiguredMenus(File configFile, List<Menu> menus) {
        List<Menu> configuredMenus = new ArrayList<>();
        if (configFile == null || !configFile.isFile()) {
            return configuredMenus;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(configFile);
            JsonNode menuDefinitions = root.get(MENUS_FIELD);
            if (menuDefinitions == null || !menuDefinitions.isArray()) {
                return configuredMenus;
            }

            Map<String, Menu> menusByElementId = new HashMap<>();
            for (Menu menu : menus) {
                if (!GenericValidator.isBlankOrNull(menu.getElementId())) {
                    menusByElementId.put(menu.getElementId(), menu);
                }
            }

            for (JsonNode menuDefinition : menuDefinitions) {
                materializeMenu(menuDefinition, null, menus, menusByElementId, configuredMenus);
            }
        } catch (IOException e) {
            LogEvent.logError("Error reading menu config file: " + configFile.getAbsolutePath(), e);
        }
        return configuredMenus;
    }

    private static Menu materializeMenu(JsonNode definition, Menu parent, List<Menu> menus,
            Map<String, Menu> menusByElementId, List<Menu> configuredMenus) {
        String elementId = text(definition, ELEMENT_ID_FIELD);
        if (GenericValidator.isBlankOrNull(elementId)) {
            LogEvent.logWarn("MenuConfigurationLoader", "materializeMenu",
                    "Ignoring a configured menu without an elementId");
            return null;
        }

        Menu menu = menusByElementId.get(elementId);
        if (menu == null) {
            menu = new Menu();
            menu.setId("configuration:" + elementId);
            menu.setElementId(elementId);
            menu.setIsActive(true);
            if (parent != null) {
                menu.setParent(parent);
            }
            menus.add(menu);
            configuredMenus.add(menu);
            menusByElementId.put(elementId, menu);
        } else if (hasConfiguredFields(definition)) {
            menu = replaceWithConfiguredMenu(menu, menus, menusByElementId);
        }

        applyConfiguredFields(menu, definition);
        JsonNode children = definition.get(CHILD_MENUS_FIELD);
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                materializeMenu(child, menu, menus, menusByElementId, configuredMenus);
            }
        }
        return menu;
    }

    private static Menu replaceWithConfiguredMenu(Menu existingMenu, List<Menu> menus,
            Map<String, Menu> menusByElementId) {
        Menu replacement = new Menu();
        replacement.setId(existingMenu.getId());
        replacement.setElementId(existingMenu.getElementId());
        replacement.setPresentationOrder(existingMenu.getPresentationOrder());
        replacement.setActionURL(existingMenu.getActionURL());
        replacement.setClickAction(existingMenu.getClickAction());
        replacement.setDisplayKey(existingMenu.getDisplayKey());
        replacement.setToolTipKey(existingMenu.getToolTipKey());
        replacement.setOpenInNewWindow(existingMenu.isOpenInNewWindow());
        replacement.setIsActive(existingMenu.getIsActive());
        replacement.setHideInOldUI(existingMenu.isHideInOldUI());
        if (existingMenu.getParent() != null) {
            replacement.setParent(existingMenu.getParent());
        }

        int menuIndex = menus.indexOf(existingMenu);
        if (menuIndex >= 0) {
            menus.set(menuIndex, replacement);
        }
        menusByElementId.put(replacement.getElementId(), replacement);
        return replacement;
    }

    private static boolean hasConfiguredFields(JsonNode definition) {
        return definition.has("actionURL") || definition.has("displayKey") || definition.has("toolTipKey")
                || definition.has("presentationOrder") || definition.has("openInNewWindow")
                || definition.has("isActive") || definition.has("hideInOldUI");
    }

    private static void applyConfiguredFields(Menu menu, JsonNode definition) {
        if (definition.has("actionURL")) {
            menu.setActionURL(text(definition, "actionURL"));
        }
        if (definition.has("displayKey")) {
            menu.setDisplayKey(text(definition, "displayKey"));
        }
        if (definition.has("toolTipKey")) {
            menu.setToolTipKey(text(definition, "toolTipKey"));
        }
        if (definition.has("presentationOrder")) {
            menu.setPresentationOrder(definition.get("presentationOrder").asInt());
        }
        if (definition.has("openInNewWindow")) {
            menu.setOpenInNewWindow(definition.get("openInNewWindow").asBoolean());
        }
        if (definition.has("isActive")) {
            menu.setIsActive(definition.get("isActive").asBoolean());
        }
        if (definition.has("hideInOldUI")) {
            menu.setHideInOldUI(definition.get("hideInOldUI").asBoolean());
        }
    }

    private static String text(JsonNode definition, String field) {
        JsonNode value = definition.get(field);
        return value != null && value.isTextual() ? value.asText() : "";
    }
}
