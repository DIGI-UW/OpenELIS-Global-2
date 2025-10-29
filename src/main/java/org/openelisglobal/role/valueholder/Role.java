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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.role.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemusermodule.valueholder.PermissionAgent;

@Entity
@Table(name = "system_role")
public class Role extends BaseObject<String> implements PermissionAgent {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_role_seq_gen")
    @SequenceGenerator(name = "system_role_seq_gen", sequenceName = "system_role_seq", allocationSize = 1)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_grouping_role")
    private boolean groupingRole;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "grouping_parent")
    private String groupingParent;

    @Column(name = "display_key")
    private String displayKey;

    @Column(name = "active")
    private boolean active;

    @Column(name = "editable")
    private boolean editable;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return getName();
    }

    public String getShortNameForDisplay() {
        return getName();
    }

    public boolean getGroupingRole() {
        return groupingRole;
    }

    public void setGroupingRole(boolean groupingRole) {
        this.groupingRole = groupingRole;
    }

    public String getGroupingParent() {
        return groupingParent;
    }

    public void setGroupingParent(String groupingParent) {
        this.groupingParent = groupingParent;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    public void setDisplayKey(String displayKey) {
        this.displayKey = displayKey;
    }

    protected String getDefaultLocalizedName() {
        return getName();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}