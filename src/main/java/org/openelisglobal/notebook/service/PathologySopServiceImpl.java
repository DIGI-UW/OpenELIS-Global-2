package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.PathologySopDAO;
import org.openelisglobal.notebook.valueholder.PathologySop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for PathologySop entity. Inherits standard CRUD
 * operations from AuditableBaseObjectServiceImpl.
 */
@Service
public class PathologySopServiceImpl extends AuditableBaseObjectServiceImpl<PathologySop, Integer>
        implements PathologySopService {

    @Autowired
    private PathologySopDAO baseObjectDAO;

    public PathologySopServiceImpl() {
        super(PathologySop.class);
        this.auditTrailLog = false;
    }

    @Override
    protected BaseDAO<PathologySop, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySop> getByNotebookId(Integer notebookId) {
        return baseObjectDAO.getByNotebookId(notebookId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySop> getAllActive() {
        return baseObjectDAO.getAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySop> getByCategory(String category) {
        return baseObjectDAO.getByCategory(category);
    }
}
