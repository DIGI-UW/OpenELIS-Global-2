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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.requester.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.requester.valueholder.SampleRequester;

public interface SampleRequesterDAO extends BaseDAO<SampleRequester, String> {

    // public boolean insertData(SampleRequester sampleRequester) throws
    // LIMSRuntimeException;

    public List<SampleRequester> getRequestersForSampleId(String sampleId) throws LIMSRuntimeException;

    List<SampleRequester> getRequestersForRequesterId(String requesterId, String requesterTypeId);

    // void updateData(SampleRequester sampleRequester) throws LIMSRuntimeException;

    // void insertOrUpdateData(SampleRequester sampleRequester) throws
    // LIMSRuntimeException;

}
