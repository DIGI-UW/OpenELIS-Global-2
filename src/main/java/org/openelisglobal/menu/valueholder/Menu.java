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
package org.openelisglobal.menu.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.internationalization.MessageUtil;

@JsonIgnoreProperties({ "serialVersionUID", "id", "parent", "click_action", "localizedTitle", "localizedTooltip" })
public class Menu extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private ValueHolderInterface parent = new ValueHolder();

    private int presentationOrder;

    private String elementId;

    private String actionURL;

    private String clickAction;

    private String displayKey;

    private String toolTipKey;

    private boolean openInNewWindow;

    private boolean isActive;

    private boolean hideInOldUI;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPresentationOrder() {
        return presentationOrder;
    }

    public void setPresentationOrder(int presentationOrder) {
        this.presentationOrder = presentationOrder;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
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

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public void setParent(Menu parent) {
        this.parent.setValue(parent);
    }

    public Menu getParent() {
        return (Menu) this.parent.getValue();
    }

    public String getLocalizedTitle() {
        if (GenericValidator.isBlankOrNull(getDisplayKey())) {
            return null;
        } else {
            return MessageUtil.getContextualMessage(getDisplayKey());
        }
    }

    public String getLocalizedTooltip() {
        if (GenericValidator.isBlankOrNull(getToolTipKey())) {
            return null;
        } else {
            return MessageUtil.getContextualMessage(getToolTipKey());
        }
    }

    public void setOpenInNewWindow(boolean openInNewWindow) {
        this.openInNewWindow = openInNewWindow;
    }

    public boolean isOpenInNewWindow() {
        return openInNewWindow;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isHideInOldUI() {
        return hideInOldUI;
    }

    public void setHideInOldUI(boolean hideInOldUI) {
        this.hideInOldUI = hideInOldUI;
    }
}
