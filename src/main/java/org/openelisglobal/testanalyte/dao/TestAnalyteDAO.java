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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.testanalyte.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface TestAnalyteDAO extends BaseDAO<TestAnalyte, String> {

    // public boolean insertData(TestAnalyte testAnalyte) throws
    // LIMSRuntimeException;

    /**
     * @param testAnalytes
     * @throws LIMSRuntimeException
     */
    // public void deleteData(List testAnalytes) throws LIMSRuntimeException;

    /**
     * @return
     * @throws LIMSRuntimeException
     */
    List<TestAnalyte> getAllTestAnalytes() throws LIMSRuntimeException;

    /**
     * @param startingRecNo
     * @return
     * @throws LIMSRuntimeException
     */
    List<TestAnalyte> getPageOfTestAnalytes(int startingRecNo) throws LIMSRuntimeException;

    /**
     * @param testAnalyte
     * @throws LIMSRuntimeException
     */
    TestAnalyte getData(TestAnalyte testAnalyte) throws LIMSRuntimeException;

    /**
     * @param testAnalyte
     * @throws LIMSRuntimeException
     */
    // public void updateData(TestAnalyte testAnalyte) throws LIMSRuntimeException;

    /**
     * @param filter
     * @return
     * @throws LIMSRuntimeException
     */
    List<TestAnalyte> getTestAnalytes(String filter) throws LIMSRuntimeException;

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
     * @param testAnalyte
     * @return
     * @throws LIMSRuntimeException
     */
    TestAnalyte getTestAnalyteById(TestAnalyte testAnalyte) throws LIMSRuntimeException;

    /**
     * @param test
     * @return
     * @throws LIMSRuntimeException
     */
    List<TestAnalyte> getAllTestAnalytesPerTest(Test test) throws LIMSRuntimeException;
}
