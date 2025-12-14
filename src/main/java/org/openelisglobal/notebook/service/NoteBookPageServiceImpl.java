package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for NoteBookPage entity. Inherits standard CRUD
 * operations from AuditableBaseObjectServiceImpl.
 */
@Service
public class NoteBookPageServiceImpl extends AuditableBaseObjectServiceImpl<NoteBookPage, Integer>
        implements NoteBookPageService {

    @Autowired
    private NoteBookPageDAO baseObjectDAO;

    public NoteBookPageServiceImpl() {
        super(NoteBookPage.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<NoteBookPage, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteBookPage> getByNotebookId(Integer notebookId) {
        return baseObjectDAO.getByNotebookId(notebookId);
    }
}
