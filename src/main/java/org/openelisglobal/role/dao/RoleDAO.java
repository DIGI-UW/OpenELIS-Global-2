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
package org.openelisglobal.role.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.role.valueholder.Role;

public interface RoleDAO extends BaseDAO<Role, String> {

    // public boolean insertData(Role role) throws LIMSRuntimeException;

    // public void deleteData(List<Role> roles) throws LIMSRuntimeException;

    List<Role> getAllRoles() throws LIMSRuntimeException;

    List<Role> getPageOfRoles(int startingRecNo) throws LIMSRuntimeException;

    void getData(Role role) throws LIMSRuntimeException;

    // public void updateData(Role role) throws LIMSRuntimeException;

    List<Role> getReferencingRoles(Role role) throws LIMSRuntimeException;

    List<Role> getAllActiveRoles() throws LIMSRuntimeException;

    Role getRoleByName(String name) throws LIMSRuntimeException;

    Role getRoleById(String roleId) throws LIMSRuntimeException;
}
