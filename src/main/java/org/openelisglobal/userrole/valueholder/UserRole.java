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

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.valueholder.BaseObject;

public class UserRole extends BaseObject<UserRolePK> {

    private static final long serialVersionUID = 1L;

    private UserRolePK compoundId = new UserRolePK();
    private String userName;
    private String roleName;
    private String uniqueIdentifyer;

    public void setCompoundId(UserRolePK compoundId) {
        uniqueIdentifyer = null;
        this.compoundId = compoundId;
    }

    public UserRolePK getCompoundId() {
        return compoundId;
    }

    public String getStringId() {
        return compoundId == null ? "0" : compoundId.getSystemUserId() + compoundId.getRoleId();
    }

    public void setSystemUserId(String systemUserId) {
        uniqueIdentifyer = null;
        compoundId.setSystemUserId(systemUserId);
    }

    public String getSystemUserId() {
        return compoundId == null ? null : compoundId.getSystemUserId();
    }

    public void setRoleId(String roleId) {
        uniqueIdentifyer = null;
        compoundId.setRoleId(roleId);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleId() {
        return compoundId == null ? null : compoundId.getRoleId();
    }

    public void setUniqueIdentifyer(String uniqueIdentifyer) {
        this.uniqueIdentifyer = uniqueIdentifyer;
    }

    public String getUniqueIdentifyer() {
        if (GenericValidator.isBlankOrNull(uniqueIdentifyer)) {
            uniqueIdentifyer = getSystemUserId() + "-" + getRoleId();
        }
        return uniqueIdentifyer;
    }

    @Override
    public void setId(UserRolePK id) {
        setCompoundId(id);
    }

    @Override
    public UserRolePK getId() {
        return getCompoundId();
    }
}
