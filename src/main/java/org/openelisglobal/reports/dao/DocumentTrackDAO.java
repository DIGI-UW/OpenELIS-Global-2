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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.reports.valueholder.DocumentTrack;

public interface DocumentTrackDAO extends BaseDAO<DocumentTrack, String> {

    // public List<DocumentTrack> getByTypeRecordAndTable(String typeId, String
    // tableId, String
    // recordId)
    // throws LIMSRuntimeException;

    // public List<DocumentTrack> getByTypeRecordAndTableAndName(String
    // reportTypeId, String
    // referenceTable, String id,
    // String name) throws LIMSRuntimeException;

    // public void insertData(DocumentTrack docTrack);

    // public DocumentTrack readEntity(String id);
}
