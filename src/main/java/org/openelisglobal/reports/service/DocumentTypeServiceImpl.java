package org.openelisglobal.reports.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.reports.dao.DocumentTypeDAO;
import org.openelisglobal.reports.valueholder.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentTypeServiceImpl extends AuditableBaseObjectServiceImpl<DocumentType, String>
        implements DocumentTypeService {
    @Autowired
    protected DocumentTypeDAO baseObjectDAO;

    DocumentTypeServiceImpl() {
        super(DocumentType.class);
    }

    @Override
    protected DocumentTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentType getDocumentTypeByName(String name) {
        return getMatch("name", name).orElse(null);
    }
}
