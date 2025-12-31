package org.openelisglobal.notebook.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.valueholder.PathologySop;

/**
 * Service interface for PathologySop entity. Provides business operations for
 * pathology SOP documents.
 */
public interface PathologySopService extends BaseObjectService<PathologySop, Integer> {

    /**
     * Get all SOPs for a notebook entry.
     *
     * @param notebookId the notebook ID (entry ID)
     * @return list of SOPs for the notebook
     */
    List<PathologySop> getByNotebookId(Integer notebookId);

    /**
     * Get all active SOPs.
     *
     * @return list of active SOPs
     */
    List<PathologySop> getAllActive();

    /**
     * Get SOPs by category.
     *
     * @param category the SOP category
     * @return list of SOPs in the category
     */
    List<PathologySop> getByCategory(String category);
}
