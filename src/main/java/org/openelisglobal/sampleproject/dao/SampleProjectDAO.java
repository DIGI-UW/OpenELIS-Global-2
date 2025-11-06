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
package org.openelisglobal.sampleproject.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sampleproject.valueholder.SampleProject;

/**
 * $Header$
 *
 * @author Hung Nguyen
 * @date created 08/04/2006
 * @version $Revision$ // AIS - bugzilla 1851 //bugzilla 1920 standards
 */
public interface SampleProjectDAO extends BaseDAO<SampleProject, String> {

    // public boolean insertData(SampleProject sampleProj) throws
    // LIMSRuntimeException;

    // public void deleteData(List sampleProjs) throws LIMSRuntimeException;

    void getData(SampleProject sampleProj) throws LIMSRuntimeException;

    // public void updateData(SampleProject sampleProj) throws LIMSRuntimeException;

    List<SampleProject> getSampleProjectsByProjId(String projId) throws LIMSRuntimeException;

    SampleProject getSampleProjectBySampleId(String id) throws LIMSRuntimeException;

    /**
     * @param locationId
     * @param projectName
     * @param lowDate
     * @param highDate
     * @return
     */
    List<SampleProject> getByOrganizationProjectAndReceivedOnRange(String organizationId, String projectName,
            Date lowDate, Date highDate);
}
