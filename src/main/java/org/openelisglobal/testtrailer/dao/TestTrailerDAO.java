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
package org.openelisglobal.testtrailer.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testtrailer.valueholder.TestTrailer;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface TestTrailerDAO extends BaseDAO<TestTrailer, String> {

    // public boolean insertData(TestTrailer testTrailer) throws
    // LIMSRuntimeException;

    // public void deleteData(List testTrailers) throws LIMSRuntimeException;

    List<TestTrailer> getAllTestTrailers() throws LIMSRuntimeException;

    List<TestTrailer> getPageOfTestTrailers(int startingRecNo) throws LIMSRuntimeException;

    void getData(TestTrailer testTrailer) throws LIMSRuntimeException;

    // public void updateData(TestTrailer testTrailer) throws LIMSRuntimeException;

    TestTrailer getTestTrailerByName(TestTrailer testTrailer) throws LIMSRuntimeException;

    List<TestTrailer> getTestTrailers(String filter) throws LIMSRuntimeException;

    // bugzilla 1411
    Integer getTotalTestTrailerCount() throws LIMSRuntimeException;

    boolean duplicateTestTrailerExists(TestTrailer testTrailer) throws LIMSRuntimeException;
}
