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
package org.openelisglobal.reports.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.reports.dao.DocumentTrackDAO;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DocumentTrackDAOImpl extends BaseDAOImpl<DocumentTrack, String> implements DocumentTrackDAO {

    public DocumentTrackDAOImpl() {
        super(DocumentTrack.class);
    }

    //
    // @Override
    // public List<DocumentTrack> getByTypeRecordAndTable(String typeId, String
    // tableId, String
    // recordId)
    // throws LIMSRuntimeException {
    // String sql = "From DocumentTrack dt where dt.documentTypeId = :typeId and
    // dt.tableId =
    // :tableId and dt.recordId = :recordId order by dt.reportTime";
    //
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setInteger("typeId", Integer.parseInt(typeId));
    // query.setInteger("tableId", Integer.parseInt(tableId));
    // query.setInteger("recordId", Integer.parseInt(recordId));
    // List<DocumentTrack> documents = query.list();
    // // closeSession(); // CSL remove old
    // return documents;
    //
    // } catch (HibernateException e) {
    // handleException(e, "getByTypeRecordAndTable");
    // }
    //
    // return null;
    // }

    // @Override
    // public List<DocumentTrack> getByTypeRecordAndTableAndName(String
    // reportTypeId, String tableId,
    // String recordId,
    // String name) throws LIMSRuntimeException {
    // String sql = "From DocumentTrack dt where dt.documentTypeId = :typeId and
    // dt.tableId =
    // :tableId and dt.recordId = :recordId and dt.documentName = :name order by
    // dt.reportTime";
    //
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setInteger("typeId", Integer.parseInt(reportTypeId));
    // query.setInteger("tableId", Integer.parseInt(tableId));
    // query.setInteger("recordId", Integer.parseInt(recordId));
    // query.setString("name", name);
    // List<DocumentTrack> documents = query.list();
    // // closeSession(); // CSL remove old
    // return documents;
    //
    // } catch (HibernateException e) {
    // handleException(e, "getByTypeRecordAndTableAndName");
    // }
    //
    // return null;
    // }

    // @Override
    // public void insertData(DocumentTrack docTrack) {
    // insert(docTrack);
    // }

    // @Override
    // public DocumentTrack readEntity(String id) {
    // return get(id).get();
    // }

}
