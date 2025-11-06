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
package org.openelisglobal.project.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.project.valueholder.Project;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface ProjectDAO extends BaseDAO<Project, String> {

    // public boolean insertData(Project project) throws LIMSRuntimeException;

    // public void deleteData(List projects) throws LIMSRuntimeException;

    List<Project> getAllProjects() throws LIMSRuntimeException;

    List<Project> getPageOfProjects(int startingRecNo) throws LIMSRuntimeException;

    void getData(Project project) throws LIMSRuntimeException;

    // public void updateData(Project project) throws LIMSRuntimeException;

    // bugzilla 1978: added param activeOnly
    Project getProjectByName(Project project, boolean ignoreCase, boolean activeOnly) throws LIMSRuntimeException;

    // bugzilla 1978: added param activeOnly
    List<Project> getProjects(String filter, boolean activeOnly) throws LIMSRuntimeException;

    // bugzilla 1411
    Integer getTotalProjectCount() throws LIMSRuntimeException;

    // bugzilla 2438
    Project getProjectByLocalAbbreviation(Project project, boolean activeOnly) throws LIMSRuntimeException;

    Project getProjectById(String id) throws LIMSRuntimeException;

    boolean duplicateProjectExists(Project project) throws LIMSRuntimeException;
}
