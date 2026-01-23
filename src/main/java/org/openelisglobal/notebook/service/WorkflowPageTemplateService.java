package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.notebook.valueholder.WorkflowPageTemplate;

/**
 * Service interface for WorkflowPageTemplate operations. This is a simple
 * read-only service for static configuration data.
 */
public interface WorkflowPageTemplateService {

    /** Get all active workflow page templates. */
    List<WorkflowPageTemplate> getAllActive();

    /** Get templates by workflow category (e.g., "IMMUNOLOGY"). */
    List<WorkflowPageTemplate> getByCategory(String category);
}
