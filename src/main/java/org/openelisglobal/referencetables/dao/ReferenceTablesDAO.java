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
package org.openelisglobal.referencetables.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;

/**
 * @author Yi Chen
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface ReferenceTablesDAO extends BaseDAO<ReferenceTables, String> {

    // public boolean insertData(ReferenceTables referenceTables) throws
    // LIMSRuntimeException;

    // public void deleteData(List referenceTableses) throws LIMSRuntimeException;

    List<ReferenceTables> getAllReferenceTables() throws LIMSRuntimeException;

    List<ReferenceTables> getPageOfReferenceTables(int startingRecNo) throws LIMSRuntimeException;

    void getData(ReferenceTables referenceTables) throws LIMSRuntimeException;

    // public void updateData(ReferenceTables referenceTables) throws
    // LIMSRuntimeException;

    Integer getTotalReferenceTablesCount() throws LIMSRuntimeException;

    List<ReferenceTables> getAllReferenceTablesForHl7Encoding() throws LIMSRuntimeException;

    ReferenceTables getReferenceTableByName(ReferenceTables referenceTables) throws LIMSRuntimeException;

    ReferenceTables getReferenceTableByName(String tableName) throws LIMSRuntimeException;

    Integer getTotalReferenceTableCount() throws LIMSRuntimeException;

    boolean duplicateReferenceTablesExists(ReferenceTables referenceTables, boolean isNew) throws LIMSRuntimeException;
}
