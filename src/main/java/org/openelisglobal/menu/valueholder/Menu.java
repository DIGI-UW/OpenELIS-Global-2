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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.internationalization.MessageUtil;

@Entity
@Table(name = "menu")
@DynamicUpdate
@JsonIgnoreProperties({ "serialVersionUID", "id", "parent", "click_action", "localizedTitle", "localizedTooltip" })
public class Menu extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "menu_seq_gen")
    @GenericGenerator(name = "menu_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "menu_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Menu parent;

    @Column(name = "presentation_order")
    private int presentationOrder;

    @Column(name = "element_id")
    private String elementId;

    @Column(name = "action_url")
    private String actionURL;

    @Column(name = "click_action")
    private String clickAction;

    @Column(name = "display_key")
    private String displayKey;

    @Column(name = "tool_tip_key")
    private String toolTipKey;

    @Column(name = "new_window")
    private boolean openInNewWindow;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "hide_in_old_ui")
    private boolean hideInOldUI;

    @Column(name = "presentation_style")
    private String presentationStyle;

    @Column(name = "icon")
    private String icon;

    @Transient
    private boolean presentationStyleSpecified;
    @Transient
    private boolean iconSpecified;
    @Transient
    private Set<String> configurationFields = Set.of();
    @Transient
    private boolean configurationOnly;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Set<String> getConfigurationFields() {
        return configurationFields;
    }

    public void setConfigurationFields(Set<String> fields) {
        configurationFields = Set.copyOf(fields);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean isConfigurationOnly() {
        return configurationOnly;
    }

    public void setConfigurationOnly(boolean value) {
        configurationOnly = value;
    }

    @JsonIgnore
    public boolean isPresentationStyleSpecified() {
        return presentationStyleSpecified;
    }

    @JsonIgnore
    public boolean isIconSpecified() {
        return iconSpecified;
    }

    public String getPresentationStyle() {
        return presentationStyle;
    }

    public void setPresentationStyle(String presentationStyle) {
        this.presentationStyle = presentationStyle;
        this.presentationStyleSpecified = true;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
        this.iconSpecified = true;
    }

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
        this.parent = parent;
    }

    public Menu getParent() {
        return parent;
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
