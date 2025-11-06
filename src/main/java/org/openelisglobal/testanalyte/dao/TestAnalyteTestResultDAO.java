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
package org.openelisglobal.testanalyte.dao;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.test.valueholder.Test;

/**
 * @author diane benz
 *     <p>To change this generated comment edit the template variable "typecomment":
 *     Window>Preferences>Java>Templates. To enable and disable the creation of type comments go to
 *     Window>Preferences>Java>Code Generation.
 */
/**
 * @author Benzd1
 */
public interface TestAnalyteTestResultDAO {

    /**
     * @param test
     * @param testAnalytes
     * @param testResults
     * @return
     * @throws LIMSRuntimeException
     */
    public boolean insertData(Test test, List testAnalytes, List testResults) throws LIMSRuntimeException;

    /**
     * @param startingRecNo
     * @param test
     * @return
     * @throws LIMSRuntimeException
     */
    public List getPageOfTestAnalyteTestResults(int startingRecNo, Test test) throws LIMSRuntimeException;

    /**
     * @param test
     * @return
     * @throws LIMSRuntimeException
     */
    public List getAllTestAnalyteTestResultsPerTest(Test test) throws LIMSRuntimeException;

    /**
     * @param test
     * @param oldTestAnalytes
     * @param oldTestResults
     * @param testAnalytes
     * @param testResults
     * @throws LIMSRuntimeException
     */
    public void updateData(Test test, List oldTestAnalytes, List oldTestResults, List testAnalytes, List testResults)
            throws LIMSRuntimeException;

    /**
     * @param id
     * @return
     * @throws LIMSRuntimeException
     */

    /**
     * @param id
     * @return
     * @throws LIMSRuntimeException
     */
}
