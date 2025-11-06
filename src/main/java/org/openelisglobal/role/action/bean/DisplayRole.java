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
package org.openelisglobal.role.action.bean;

public class DisplayRole {
    public static final String GROUPING_ID = "Grouping_ID";
    private String roleName;
    private String roleId;
    private int nestingLevel = 0;
    private String elementID;
    private StringBuilder childrenID;
    private boolean isGroupingRole = false;
    private String parentRole;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public int getNestingLevel() {
        return nestingLevel;
    }

    public void setNestingLevel(int nestingLevel) {
        this.nestingLevel = nestingLevel;
    }

    public String getElementID() {
        return elementID;
    }

    public void setElementID(String elementID) {
        this.elementID = elementID;
    }

    public String getChildrenID() {
        return childrenID == null ? "" : '\'' + childrenID.toString() + '\'';
    }

    public void addChildID(String childId) {
        if (childrenID == null) {
            childrenID = new StringBuilder();
        } else {
            childrenID.append("_");
        }

        childrenID.append(childId);
    }

    public void setGroupingRole(boolean isGroupingRole) {
        this.isGroupingRole = isGroupingRole;
    }

    public boolean isGroupingRole() {
        return isGroupingRole;
    }

    public void setParentRole(String parentRole) {
        this.parentRole = parentRole;
    }

    public String getParentRole() {
        return parentRole;
    }
}
