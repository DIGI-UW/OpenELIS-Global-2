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
package org.openelisglobal.userrole.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.openelisglobal.userrole.valueholder.UserRolePK;

public interface UserRoleDAO extends BaseDAO<UserRole, UserRolePK> {

    public boolean userInRole(String userId, String roleName) throws LIMSRuntimeException;

    public boolean userInRole(String userId, Collection<String> roleNames) throws LIMSRuntimeException;

    List<String> getRoleIdsForUser(String userId) throws LIMSRuntimeException;

    void deleteLabUnitRoleMap(LabUnitRoleMap roleMap);

    List<String> getUserIdsForRole(String roleName);
}
