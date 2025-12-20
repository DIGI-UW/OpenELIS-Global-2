package org.openelisglobal.menu.valueholder;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for parsing menu seed configuration from JSON file. Used by
 * MenuSeedService to read menu_seed.json and create menu entries in the
 * database at application startup.
 */
public class MenuSeedConfig {

    @JsonProperty("menus")
    private List<MenuItemDTO> menus = new ArrayList<>();

    public List<MenuItemDTO> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuItemDTO> menus) {
        this.menus = menus;
    }

    /**
     * DTO representing a single menu item definition from the seed configuration.
     * Supports hierarchical menu structures via the childMenus field.
     */
    public static class MenuItemDTO {

        @JsonProperty("elementId")
        private String elementId;

        @JsonProperty("displayKey")
        private String displayKey;

        @JsonProperty("toolTipKey")
        private String toolTipKey;

        @JsonProperty("actionURL")
        private String actionURL;

        @JsonProperty("clickAction")
        private String clickAction;

        @JsonProperty("presentationOrder")
        private Integer presentationOrder;

        @JsonProperty("isActive")
        private Boolean isActive = true;

        @JsonProperty("openInNewWindow")
        private Boolean openInNewWindow = false;

        @JsonProperty("hideInOldUI")
        private Boolean hideInOldUI = false;

        @JsonProperty("childMenus")
        private List<MenuItemDTO> childMenus = new ArrayList<>();

        // Getters and Setters

        public String getElementId() {
            return elementId;
        }

        public void setElementId(String elementId) {
            this.elementId = elementId;
        }

        public String getDisplayKey() {
            return displayKey;
        }

        public void setDisplayKey(String displayKey) {
            this.displayKey = displayKey;
        }

        public String getToolTipKey() {
            return toolTipKey;
        }

        public void setToolTipKey(String toolTipKey) {
            this.toolTipKey = toolTipKey;
        }

        public String getActionURL() {
            return actionURL;
        }

        public void setActionURL(String actionURL) {
            this.actionURL = actionURL;
        }

        public String getClickAction() {
            return clickAction;
        }

        public void setClickAction(String clickAction) {
            this.clickAction = clickAction;
        }

        public Integer getPresentationOrder() {
            return presentationOrder;
        }

        public void setPresentationOrder(Integer presentationOrder) {
            this.presentationOrder = presentationOrder;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }

        public Boolean getOpenInNewWindow() {
            return openInNewWindow;
        }

        public void setOpenInNewWindow(Boolean openInNewWindow) {
            this.openInNewWindow = openInNewWindow;
        }

        public Boolean getHideInOldUI() {
            return hideInOldUI;
        }

        public void setHideInOldUI(Boolean hideInOldUI) {
            this.hideInOldUI = hideInOldUI;
        }

        public List<MenuItemDTO> getChildMenus() {
            return childMenus;
        }

        public void setChildMenus(List<MenuItemDTO> childMenus) {
            this.childMenus = childMenus;
        }
    }
}
