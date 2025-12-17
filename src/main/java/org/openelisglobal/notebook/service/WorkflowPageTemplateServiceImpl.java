package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.notebook.dao.WorkflowPageTemplateDAO;
import org.openelisglobal.notebook.valueholder.WorkflowPageTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for WorkflowPageTemplate operations. This is a simple
 * read-only service for static configuration data.
 */
@Service
public class WorkflowPageTemplateServiceImpl implements WorkflowPageTemplateService {

    @Autowired
    private WorkflowPageTemplateDAO workflowPageTemplateDAO;

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowPageTemplate> getAllActive() {
        return workflowPageTemplateDAO.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowPageTemplate> getByCategory(String category) {
        return workflowPageTemplateDAO.findByCategory(category);
    }
}
