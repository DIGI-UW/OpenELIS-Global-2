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
 */
package org.openelisglobal.systemmodule.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.systemmodule.valueholder.SystemModule;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
public interface SystemModuleDAO extends BaseDAO<SystemModule, String> {

    // public boolean insertData(SystemModule systemModule) throws
    // LIMSRuntimeException;

    // public void deleteData(List systemModule) throws LIMSRuntimeException;

    List<SystemModule> getAllSystemModules() throws LIMSRuntimeException;

    List<SystemModule> getPageOfSystemModules(int startingRecNo) throws LIMSRuntimeException;

    void getData(SystemModule systemModule) throws LIMSRuntimeException;

    // public void updateData(SystemModule systemModule) throws
    // LIMSRuntimeException;

    Integer getTotalSystemModuleCount() throws LIMSRuntimeException;

    SystemModule getSystemModuleByName(String name) throws LIMSRuntimeException;

    boolean duplicateSystemModuleExists(SystemModule systemModule);
}
