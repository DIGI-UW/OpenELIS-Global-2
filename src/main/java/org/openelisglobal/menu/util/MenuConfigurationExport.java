package org.openelisglobal.menu.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.openelisglobal.menu.valueholder.Menu;

/**
 * The current menu in the menu configuration file's format, so a distribution
 * can ship it: seeding a fresh install with it reproduces this navigation.
 * Menus the application adds at runtime are left out.
 */
public final class MenuConfigurationExport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MenuConfigurationExport() {
    }

    public static ObjectNode of(List<MenuItem> menuTree) {
        ObjectNode configuration = OBJECT_MAPPER.createObjectNode();
        configuration.set("menus", items(menuTree));
        return configuration;
    }

    private static ArrayNode items(List<MenuItem> items) {
        ArrayNode array = OBJECT_MAPPER.createArrayNode();
        for (MenuItem item : items) {
            Menu menu = item.getMenu();
            if (menu.getId() == null) {
                continue;
            }
            ObjectNode node = array.addObject();
            node.put("elementId", menu.getElementId());
            node.put("displayKey", menu.getDisplayKey());
            node.put("toolTipKey", menu.getToolTipKey());
            node.put("actionURL", menu.getActionURL());
            node.put("presentationOrder", menu.getPresentationOrder());
            node.put("isActive", menu.getIsActive());
            node.put("openInNewWindow", menu.isOpenInNewWindow());
            node.put("hideInOldUI", menu.isHideInOldUI());
            node.put("presentationStyle", menu.getPresentationStyle());
            node.put("icon", menu.getIcon());
            node.set("childMenus", items(item.getChildMenus()));
        }
        return array;
    }
}
