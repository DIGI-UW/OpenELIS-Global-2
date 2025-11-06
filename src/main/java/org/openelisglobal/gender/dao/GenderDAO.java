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
package org.openelisglobal.gender.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.gender.valueholder.Gender;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface GenderDAO extends BaseDAO<Gender, Integer> {

    // public boolean insertData(Gender gender) throws LIMSRuntimeException;

    // public List getAllGenders() throws LIMSRuntimeException;

    // public List getPageOfGenders(int startingRecNo) throws LIMSRuntimeException;

    // public void getData(Gender gender) throws LIMSRuntimeException;

    // public void updateData(Gender gender) throws LIMSRuntimeException;

    public Gender getGenderByType(String type) throws LIMSRuntimeException;

    // public Integer getTotalGenderCount() throws LIMSRuntimeException;

    boolean duplicateGenderExists(Gender gender) throws LIMSRuntimeException;
}
