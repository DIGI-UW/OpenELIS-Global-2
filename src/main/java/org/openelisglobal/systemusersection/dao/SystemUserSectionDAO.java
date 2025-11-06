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
package org.openelisglobal.systemusersection.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.systemusersection.valueholder.SystemUserSection;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
public interface SystemUserSectionDAO extends BaseDAO<SystemUserSection, String> {

    // public boolean insertData(SystemUserSection systemUserSection) throws
    // LIMSRuntimeException;

    // public void deleteData(List systemUserSection) throws LIMSRuntimeException;

    List<SystemUserSection> getAllSystemUserSections() throws LIMSRuntimeException;

    List<SystemUserSection> getPageOfSystemUserSections(int startingRecNo) throws LIMSRuntimeException;

    void getData(SystemUserSection systemUserSection) throws LIMSRuntimeException;

    // public void updateData(SystemUserSection systemUserSection) throws
    // LIMSRuntimeException;
    //

    Integer getTotalSystemUserSectionCount() throws LIMSRuntimeException;

    List<SystemUserSection> getAllSystemUserSectionsBySystemUserId(int systemUserId) throws LIMSRuntimeException;

    boolean duplicateSystemUserSectionExists(SystemUserSection systemUserSection) throws LIMSRuntimeException;
}
