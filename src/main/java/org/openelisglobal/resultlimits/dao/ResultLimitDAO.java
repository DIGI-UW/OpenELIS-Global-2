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
package org.openelisglobal.resultlimits.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;

public interface ResultLimitDAO extends BaseDAO<ResultLimit, String> {

    // boolean insertData(ResultLimit resultLimit) throws LIMSRuntimeException;

    // void deleteData(List resultLimits) throws LIMSRuntimeException;

    List<ResultLimit> getAllResultLimits() throws LIMSRuntimeException;

    List<ResultLimit> getPageOfResultLimits(int startingRecNo) throws LIMSRuntimeException;

    void getData(ResultLimit resultLimit) throws LIMSRuntimeException;

    // void updateData(ResultLimit resultLimit) throws LIMSRuntimeException;

    List<ResultLimit> getAllResultLimitsForTest(String testId) throws LIMSRuntimeException;

    ResultLimit getResultLimitById(String resultLimitId) throws LIMSRuntimeException;
}
