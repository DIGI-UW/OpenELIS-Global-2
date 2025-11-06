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
package org.openelisglobal.testreflex.dao;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.valueholder.TestResult;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface TestReflexDAO extends BaseDAO<TestReflex, String> {

    /**
     * @param testReflex
     * @return
     * @throws LIMSRuntimeException
     */
    // public boolean insertData(TestReflex testReflex) throws LIMSRuntimeException;

    /**
     * @param testReflexs
     * @throws LIMSRuntimeException
     */
    // public void deleteData(List testReflexs) throws LIMSRuntimeException;

    /**
     * @return
     * @throws LIMSRuntimeException
     */
    List<TestReflex> getAllTestReflexs() throws LIMSRuntimeException;

    /**
     * @param startingRecNo
     * @return
     * @throws LIMSRuntimeException
     */
    List<TestReflex> getPageOfTestReflexs(int startingRecNo) throws LIMSRuntimeException;

    /**
     * @param testReflex
     * @throws LIMSRuntimeException
     */
    void getData(TestReflex testReflex) throws LIMSRuntimeException;

    /**
     * @param testReflex
     * @throws LIMSRuntimeException
     */
    // public void updateData(TestReflex testReflex) throws LIMSRuntimeException;

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

    /**
     * @param testReflex
     * @param testResult
     * @throws LIMSRuntimeException
     */
    List<TestReflex> getTestReflexesByTestResult(TestResult testResult) throws LIMSRuntimeException;

    /**
     * @param testReflex
     * @param testResult
     * @param testAnalyte
     * @throws LIMSRuntimeException
     */
    List<TestReflex> getTestReflexesByTestResultAndTestAnalyte(TestResult testResult, TestAnalyte testAnalyte)
            throws LIMSRuntimeException;

    // bugzilla 1411
    /**
     * @return
     * @throws LIMSRuntimeException
     */
    Integer getTotalTestReflexCount() throws LIMSRuntimeException;

    /**
     * bugzilla 1798
     *
     * @param analysis
     * @throws LIMSRuntimeException
     */
    boolean isReflexedTest(Analysis analysis) throws LIMSRuntimeException;

    /**
     * Gets the ReflexTest for the analysis if there is one, otherwise returns null.
     *
     * @param analysis
     * @throws LIMSRuntimeException
     */
    List<TestReflex> getTestReflexsByTestResultAnalyteTest(String testResultId, String analyteId, String testId)
            throws LIMSRuntimeException;

    /**
     * Gets the ReflexTests By analyte and test, otherwise returns null.
     *
     * @param analysis
     * @throws LIMSRuntimeException
     */
    List<TestReflex> getTestReflexsByAnalyteAndTest(String analyteId, String testId) throws LIMSRuntimeException;

    /*
     * Gets all test reflexs which may be triggered by this test and have this flag.
     * Intended use was to get testReflexs which the user decides what the action
     * is.
     *
     * @param testId The testId for which we want the reflexes
     *
     * @param flag The value of the flag field. May be null
     */
    List<TestReflex> getTestReflexsByTestAndFlag(String testId, String flag) throws LIMSRuntimeException;

    List<TestReflex> getFlaggedTestReflexesByTestResult(TestResult testResult, String flag) throws LIMSRuntimeException;

    boolean duplicateTestReflexExists(TestReflex testReflex) throws LIMSRuntimeException;

    /**
     * Gets the ReflexTests By TestAnalyte returns null.
     *
     * @param analyte id
     * @throws LIMSRuntimeException
     */
    List<TestReflex> getTestReflexsByTestAnalyteId(String testAnalyteId) throws LIMSRuntimeException;
}
