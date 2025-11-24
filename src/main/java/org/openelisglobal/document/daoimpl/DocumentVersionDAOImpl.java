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
import org.openelisglobal.document.dao.DocumentVersionDAO;
import org.openelisglobal.document.valueholder.DocumentVersion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DocumentVersionDAOImpl extends BaseDAOImpl<DocumentVersion, String> implements DocumentVersionDAO {

    public DocumentVersionDAOImpl() {
        super(DocumentVersion.class);
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DocumentVersion saveVersion(DocumentVersion v) {
        if (v.getVersionId() == null) {
            v.setVersionId(java.util.UUID.randomUUID().toString());
        }
        String id = insert(v);
        return get(id).orElse(v);
    }

    @Override
    public List<DocumentVersion> listByDocumentId(String documentId) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("idDocumentId", documentId);
        propertyValues.put("isDeleted", false);
        return getAllMatchingOrdered(propertyValues, List.of("versionNumber"), false);
    }

    @Override
    public DocumentVersion getLatestVersion(String documentId) {
        String hql = "FROM DocumentVersion v WHERE v.idDocumentId = :documentId " +
                     "AND v.isDeleted = false ORDER BY v.versionNumber DESC";
        TypedQuery<DocumentVersion> query = entityManager.createQuery(hql, DocumentVersion.class);
        query.setParameter("documentId", documentId);
        query.setMaxResults(1);
        List<DocumentVersion> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}

