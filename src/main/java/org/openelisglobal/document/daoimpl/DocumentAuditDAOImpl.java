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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.document.dao.DocumentAuditDAO;
import org.openelisglobal.document.valueholder.DocumentAudit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DocumentAuditDAOImpl extends BaseDAOImpl<DocumentAudit, String> implements DocumentAuditDAO {

    public DocumentAuditDAOImpl() {
        super(DocumentAudit.class);
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DocumentAudit save(DocumentAudit audit) {
        if (audit.getId() == null) {
            audit.setId(java.util.UUID.randomUUID().toString());
        }
        String id = insert(audit);
        return get(id).orElse(audit);
    }

    @Override
    public List<DocumentAudit> listByPatientId(String patientId) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("patientId", patientId);
        return getAllMatchingOrdered(propertyValues, List.of("createdAt"), true);
    }

    @Override
    public List<DocumentAudit> listByDocumentId(String documentId) {
        Map<String, Object> propertyValues = new HashMap<>();
        propertyValues.put("idDocumentId", documentId);
        return getAllMatchingOrdered(propertyValues, List.of("createdAt"), true);
    }
}

