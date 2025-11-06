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
package org.openelisglobal.analyte.dao;

import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface AnalyteDAO extends BaseDAO<Analyte, String> {

    // public boolean insertData(Analyte analyte) throws LIMSRuntimeException;

    // public void deleteData(List analytes) throws LIMSRuntimeException;

    // public List getAllAnalytes() throws LIMSRuntimeException;

    // public List getPageOfAnalytes(int startingRecNo) throws LIMSRuntimeException;

    // public void getData(Analyte analyte) throws LIMSRuntimeException;

    // public void updateData(Analyte analyte) throws LIMSRuntimeException;

    // public List getAnalytes(String filter) throws LIMSRuntimeException;

    //

    // bugzilla 1367 added boolean param
    public Analyte getAnalyteByName(Analyte analyte, boolean ignoreCase) throws LIMSRuntimeException;

    boolean duplicateAnalyteExists(Analyte analyte);

    // bugzilla 1411
    // public Integer getTotalAnalyteCount() throws LIMSRuntimeException;

    // bugzilla 2370
    // public List getPagesOfSearchedAnalytes(int startRecNo, String searchString)
    // throws
    // LIMSRuntimeException;

    // bugzilla 2370
    // public Integer getTotalSearchedAnalyteCount(String searchString) throws
    // LIMSRuntimeException;

}
