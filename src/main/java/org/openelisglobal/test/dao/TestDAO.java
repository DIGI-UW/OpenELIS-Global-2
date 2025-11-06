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
package org.openelisglobal.test.dao;

import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.valueholder.Test;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface TestDAO extends BaseDAO<Test, String> {

    // public boolean insertData(Test test) throws LIMSRuntimeException;

    // public void deleteData(List tests) throws LIMSRuntimeException;

    List<Test> getAllTests(boolean onlyTestsFullySetup) throws LIMSRuntimeException;

    List<Test> getAllActiveTests(boolean onlyTestsFullySetup) throws LIMSRuntimeException;

    List<Test> getAllActiveOrderableTests() throws LIMSRuntimeException;

    List<Test> getAllOrderBy(String columnName) throws LIMSRuntimeException;

    // public List getAllTestsBySysUserId(int sysUserId, boolean
    // onlyTestsFullySetup) throws
    // LIMSRuntimeException;

    List<Test> getPageOfTests(int startingRecNo) throws LIMSRuntimeException;

    // public List getPageOfTestsBySysUserId(int startingRecNo, int sysUserId)
    // throws
    // LIMSRuntimeException;

    void getData(Test test) throws LIMSRuntimeException;

    // public void updateData(Test test) throws LIMSRuntimeException;

    List<Test> getTests(String filter, boolean onlyTestsFullySetup) throws LIMSRuntimeException;

    Test getTestById(Test test) throws LIMSRuntimeException;

    Test getActiveTestById(Integer id) throws LIMSRuntimeException;

    List<Method> getMethodsByTestSection(String filter) throws LIMSRuntimeException;

    List<Test> getTestsByTestSection(String filter) throws LIMSRuntimeException;

    List<Test> getTestsByMethod(String filter) throws LIMSRuntimeException;

    List<Test> getTestsByTestSectionAndMethod(String filter, String filter2) throws LIMSRuntimeException;

    Integer getTotalTestCount() throws LIMSRuntimeException;

    // public boolean isTestFullySetup(Test test) throws LIMSRuntimeException;

    List<Test> getPageOfSearchedTests(int startingRecNo, String searchString) throws LIMSRuntimeException;

    // public Integer getAllSearchedTotalTestCount(HttpServletRequest request,
    // String searchString)
    // throws LIMSRuntimeException;

    // public Integer getTotalSearchedTestCountBySysUserId(int sysUserId, String
    // searchString) throws
    // LIMSRuntimeException;

    Integer getTotalSearchedTestCount(String searchString) throws LIMSRuntimeException;

    // public List<Test> getPageOfSearchedTestsBySysUserId(int startingRecNo, int
    // sysUserId, String
    // searchString)
    // throws LIMSRuntimeException;

    Integer getNextAvailableSortOrderByTestSection(Test test) throws LIMSRuntimeException;

    Test getTestById(String testId) throws LIMSRuntimeException;

    Test getTestByDescription(String description) throws LIMSRuntimeException;

    List<Test> getTestsByTestSectionId(String id) throws LIMSRuntimeException;

    List<Test> getTestsByTestSectionIds(List<Integer> ids) throws LIMSRuntimeException;

    Test getTestByGUID(String guid) throws LIMSRuntimeException;

    List<Test> getTestsByLoincCode(String loincCode);

    List<Test> getActiveTestsByLoinc(String loincCode);

    List<Test> getActiveTestsByLoinc(String[] loincCodes);

    boolean duplicateTestExists(Test test) throws LIMSRuntimeException;

    List<Test> getAllTestsByDictionaryResult();

    Test getActiveTestByLocalizedName(String testName, Locale locale) throws LIMSRuntimeException;

    Test getTestByLocalizedName(String testName, Locale locale) throws LIMSRuntimeException;

    List<Test> getTestsByName(String testName) throws LIMSRuntimeException;

    List<Test> getActiveTestsByName(String testName) throws LIMSRuntimeException;

    List<Test> getActiveTestsByPanelName(String panelName) throws LIMSRuntimeException;

    List<Test> getTbTestByMethod(String method) throws LIMSRuntimeException;

    List<Test> getTbTest() throws LIMSRuntimeException;

    List<Panel> getTbPanelsByMethod(String method) throws LIMSRuntimeException;
}
