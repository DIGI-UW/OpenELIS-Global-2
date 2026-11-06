package org.openelisglobal.module.api.manifest;

/**
 * One navigation entry merged into the core menu tree. {@code elementId} must
 * start with {@code menu_mod_<id>}; {@code parent} names an existing element
 * id; {@code displayKey} is a module message key ({@code mod.<id>.*});
 * {@code actionURL} is one of the module's routes or empty for a grouping
 * entry; {@code permission} names one of the module's own permission keys,
 * without which the entry is hidden from users who lack it.
 */
public class MenuEntry {

    private String elementId;
    private String parent;
    private String displayKey;
    private String toolTipKey;
    private String actionURL;
    private int presentationOrder = 100;
    private String icon;
    private String permission;

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
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

    public int getPresentationOrder() {
        return presentationOrder;
    }

    public void setPresentationOrder(int presentationOrder) {
        this.presentationOrder = presentationOrder;
    }

    /** An icon name from the shell's icon map; optional. */
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
