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
package org.openelisglobal.sampleorganization.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;

/**
 * $Header$
 *
 * @author Hung Nguyen
 * @date created 08/04/2006
 * @version $Revision$
 */
public interface SampleOrganizationDAO extends BaseDAO<SampleOrganization, String> {

    // public boolean insertData(SampleOrganization sampleOrg) throws
    // LIMSRuntimeException;

    // public void deleteData(List sampleOrgs) throws LIMSRuntimeException;

    public void getData(SampleOrganization sampleOrg) throws LIMSRuntimeException;

    // public void updateData(SampleOrganization sampleOrg) throws
    // LIMSRuntimeException;

    public void getDataBySample(SampleOrganization sampleOrg) throws LIMSRuntimeException;

    public SampleOrganization getDataBySample(Sample sample) throws LIMSRuntimeException;
}
