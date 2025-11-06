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
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface TestSectionDAO extends BaseDAO<TestSection, String> {

    // public boolean insertData(TestSection testSection) throws
    // LIMSRuntimeException;

    // public void deleteData(List testSections) throws LIMSRuntimeException;

    List<TestSection> getAllTestSections() throws LIMSRuntimeException;

    List<TestSection> getPageOfTestSections(int startingRecNo) throws LIMSRuntimeException;

    void getData(TestSection testSection) throws LIMSRuntimeException;

    // public void updateData(TestSection testSection) throws LIMSRuntimeException;

    List<TestSection> getTestSections(String filter) throws LIMSRuntimeException;

    List<TestSection> getTestSectionsBySysUserId(String filter, int sysUserId, List<String> sectionIdList)
            throws LIMSRuntimeException;

    TestSection getTestSectionByName(TestSection testSection) throws LIMSRuntimeException;

    Integer getTotalTestSectionCount() throws LIMSRuntimeException;

    List<TestSection> getAllActiveTestSections() throws LIMSRuntimeException;

    TestSection getTestSectionByName(String testSection) throws LIMSRuntimeException;

    TestSection getTestSectionById(String testSectionId) throws LIMSRuntimeException;

    List<TestSection> getAllInActiveTestSections() throws LIMSRuntimeException;

    boolean duplicateTestSectionExists(TestSection testSection) throws LIMSRuntimeException;

    List<TestSection> getAllTestSectionsBySysUserId(int sysUserId, List<String> sectionIds) throws LIMSRuntimeException;
}
