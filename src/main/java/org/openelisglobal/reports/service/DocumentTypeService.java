package org.openelisglobal.reports.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.reports.valueholder.DocumentType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
public interface DocumentTypeService extends BaseObjectService<DocumentType, String> {
    DocumentType getDocumentTypeByName(String name);
}
