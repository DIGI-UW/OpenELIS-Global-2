/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */
package org.openelisglobal.referral.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.valueholder.ReferringTestResult;

public interface ReferringTestResultDAO extends BaseDAO<ReferringTestResult, String> {

    // public boolean insertData(ReferringTestResult referringTestResult) throws
    // LIMSRuntimeException;

    public List<ReferringTestResult> getReferringTestResultsForSampleItem(String sampleItemId)
            throws LIMSRuntimeException;

    public List<ReferringTestResult> getResultsInDateRange(Date lowDate, Date highDate) throws LIMSRuntimeException;
}
