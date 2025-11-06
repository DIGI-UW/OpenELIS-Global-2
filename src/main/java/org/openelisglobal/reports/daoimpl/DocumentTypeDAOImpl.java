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
import org.openelisglobal.reports.dao.DocumentTypeDAO;
import org.openelisglobal.reports.valueholder.DocumentType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DocumentTypeDAOImpl extends BaseDAOImpl<DocumentType, String> implements DocumentTypeDAO {

    public DocumentTypeDAOImpl() {
        super(DocumentType.class);
    }

    // @Override
    // public DocumentType getDocumentTypeByName(String name) throws
    // LIMSRuntimeException {
    // String sql = "Select from DocumentType dt where name = :name";
    //
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setString("name", name);
    // DocumentType docType = (DocumentType) query.uniqueResult();
    // // closeSession(); // CSL remove old
    // return docType;
    // } catch (HibernateException e) {
    // handleException(e, "getDocumentTypeByName");
    // }
    // return null;
    // }

    // public DocumentType getByName(String name) {
    // String sql = "From DocumentType dt where dt.name = :name";
    //
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setString("name", name);
    // DocumentType document = (DocumentType) query.setMaxResults(1).uniqueResult();
    // // closeSession(); // CSL remove old
    // return document;
    // } catch (HibernateException e) {
    // handleException(e, "getByName");
    // }
    //
    // return null;
    // }

}
