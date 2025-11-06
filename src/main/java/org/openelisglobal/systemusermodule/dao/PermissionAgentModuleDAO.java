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
package org.openelisglobal.systemusermodule.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
public interface PermissionAgentModuleDAO extends BaseDAO<PermissionModule, String> {

    String SUPERVISOR = "Supervisor";

    boolean insertData(PermissionModule permissionModule) throws LIMSRuntimeException;

    void deleteData(List<PermissionModule> permissionModules) throws LIMSRuntimeException;

    List<PermissionModule> getAllPermissionModules() throws LIMSRuntimeException;

    List<PermissionModule> getPageOfPermissionModules(int startingRecNo) throws LIMSRuntimeException;

    void getData(PermissionModule permissionModule) throws LIMSRuntimeException;

    void updateData(PermissionModule permissionModule) throws LIMSRuntimeException;

    Integer getTotalPermissionModuleCount() throws LIMSRuntimeException;

    List<PermissionModule> getAllPermissionModulesByAgentId(int systemUserId) throws LIMSRuntimeException;

    boolean isAgentAllowedAccordingToName(String id, String string) throws LIMSRuntimeException;

    boolean doesUserHaveAnyModules(int userId) throws LIMSRuntimeException;
}
