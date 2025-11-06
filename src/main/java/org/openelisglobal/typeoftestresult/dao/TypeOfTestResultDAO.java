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
package org.openelisglobal.typeoftestresult.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface TypeOfTestResultDAO extends BaseDAO<TypeOfTestResult, String> {

    // public boolean insertData(TypeOfTestResult typeOfTestResult) throws
    // LIMSRuntimeException;

    // public void deleteData(List typeOfTestResults) throws LIMSRuntimeException;

    // public List getAllTypeOfTestResults() throws LIMSRuntimeException;

    // public List getPageOfTypeOfTestResults(int startingRecNo) throws
    // LIMSRuntimeException;

    // public void getData(TypeOfTestResult typeOfTestResult) throws
    // LIMSRuntimeException;

    // public void updateData(TypeOfTestResult typeOfTestResult) throws
    // LIMSRuntimeException;

    //

    // public Integer getTotalTypeOfTestResultCount() throws LIMSRuntimeException;

    public TypeOfTestResult getTypeOfTestResultByType(TypeOfTestResult typeOfTestResult) throws LIMSRuntimeException;

    // public TypeOfTestResult getTypeOfTestResultByType(String type) throws
    // LIMSRuntimeException;

    boolean duplicateTypeOfTestResultExists(TypeOfTestResult typeOfTestResult) throws LIMSRuntimeException;
}
