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
package org.openelisglobal.document.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.document.dao.IDDocumentDAO;
import org.openelisglobal.document.valueholder.IDDocument;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class IDDocumentDAOImpl extends BaseDAOImpl<IDDocument, String> implements IDDocumentDAO {

    public IDDocumentDAOImpl() {
        super(IDDocument.class);
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public IDDocument save(IDDocument doc) {
        if (doc.getId() == null) {
            // Generate ID if not set
            doc.setId(java.util.UUID.randomUUID().toString());
        }
        String id = insert(doc);
        return get(id).orElse(doc);
    }

    @Override
    public IDDocument getById(String id) {
        return get(id).orElse(null);
    }

    @Override
    public List<IDDocument> listByPatientId(String patientId) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("patientId", patientId);
        propertyValues.put("isDeleted", false);
        return getAllMatching(propertyValues);
    }

    @Override
    public void softDelete(String id) {
        IDDocument doc = getById(id);
        if (doc != null) {
            doc.setIsDeleted(true);
            update(doc);
        }
    }

    @Override
    public void restore(String id) {
        IDDocument doc = getById(id);
        if (doc != null) {
            doc.setIsDeleted(false);
            update(doc);
        }
    }

    public IDDocument findByPatientIdAndDocumentType(String patientId, String documentType) {
        String hql = "FROM IDDocument d WHERE d.patientId = :patientId " +
                     "AND d.documentType = :documentType AND d.isDeleted = false";
        TypedQuery<IDDocument> query = entityManager.createQuery(hql, IDDocument.class);
        query.setParameter("patientId", patientId);
        query.setParameter("documentType", documentType);
        List<IDDocument> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}

