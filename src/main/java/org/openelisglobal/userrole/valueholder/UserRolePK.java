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
package org.openelisglobal.userrole.valueholder;

import java.io.Serializable;
import java.util.Objects;

/*
 * System_user_role has a primary key and this is it
 */
public class UserRolePK implements Serializable {

    private static final long serialVersionUID = 1L;

    private String systemUserId;
    private String roleId;

    public String getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(String systemUserId) {
        this.systemUserId = systemUserId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String toString() {
        return systemUserId + roleId;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        UserRolePK that = (UserRolePK) o;

        return Objects.equals(this.systemUserId, that.systemUserId) && Objects.equals(this.roleId, that.roleId);
    }

    public int hashCode() {
        return Objects.hash(systemUserId, roleId);
    }
}
