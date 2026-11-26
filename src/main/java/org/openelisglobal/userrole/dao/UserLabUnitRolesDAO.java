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

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;

public interface UserLabUnitRolesDAO extends BaseDAO<UserLabUnitRoles, Integer> {

    /**
     * Active users holding the role on the lab unit (by id) or on AllLabUnits, in
     * one query against lab_unit_roles/lab_unit_role_map/lab_roles joined to
     * system_user — not every user's lab-unit roles loaded and checked one at a
     * time.
     */
    List<IdValuePair> getActiveUsersWithRoleOnLabUnit(String roleId, String labUnitId);
}
