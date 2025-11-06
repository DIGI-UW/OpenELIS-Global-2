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
package org.openelisglobal.statusofsample.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;

/**
 * @author bill mcgough
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface StatusOfSampleDAO extends BaseDAO<StatusOfSample, String> {

    // bugzilla 1942
    StatusOfSample getDataByStatusTypeAndStatusCode(StatusOfSample statusofsample) throws LIMSRuntimeException;

    // public boolean insertData(StatusOfSample sourceOfSample) throws
    // LIMSRuntimeException;

    List<StatusOfSample> getAllStatusOfSamples() throws LIMSRuntimeException;

    List<StatusOfSample> getPageOfStatusOfSamples(int startingRecNo) throws LIMSRuntimeException;

    void getData(StatusOfSample sourceOfSample) throws LIMSRuntimeException;

    // public void updateData(StatusOfSample sourceOfSample) throws
    // LIMSRuntimeException;

    // bugzilla 1411
    Integer getTotalStatusOfSampleCount() throws LIMSRuntimeException;

    boolean duplicateStatusOfSampleExists(StatusOfSample statusOfSample) throws LIMSRuntimeException;
}
