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
package org.openelisglobal.dictionarycategory.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface DictionaryCategoryDAO extends BaseDAO<DictionaryCategory, String> {

    // public boolean insertData(DictionaryCategory dictionaryCategory) throws
    // LIMSRuntimeException;

    // public void deleteData(List dictionaryCategorys) throws LIMSRuntimeException;

    // public List getAllDictionaryCategorys() throws LIMSRuntimeException;

    // public List getPageOfDictionaryCategorys(int startingRecNo) throws
    // LIMSRuntimeException;

    // public void getData(DictionaryCategory dictionaryCategory) throws
    // LIMSRuntimeException;

    // public void updateData(DictionaryCategory dictionaryCategory) throws
    // LIMSRuntimeException;

    //

    //
    // // bugzilla 1411
    // public Integer getTotalDictionaryCategoryCount() throws LIMSRuntimeException;

    public DictionaryCategory getDictionaryCategoryByName(String name) throws LIMSRuntimeException;

    boolean duplicateDictionaryCategoryExists(DictionaryCategory dictionaryCategory) throws LIMSRuntimeException;
}
