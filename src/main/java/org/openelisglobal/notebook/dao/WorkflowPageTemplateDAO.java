package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.notebook.valueholder.WorkflowPageTemplate;

/**
 * DAO interface for WorkflowPageTemplate operations. This is a simple read-only
 * DAO for static configuration data.
 */
public interface WorkflowPageTemplateDAO {

    /**
     * Find all active templates ordered by display order.
     */
    List<WorkflowPageTemplate> findAllActive();

    /**
     * Find templates by workflow category.
     */
    List<WorkflowPageTemplate> findByCategory(String category);
}
