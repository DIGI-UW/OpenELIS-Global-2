package org.openelisglobal.notebook.service;

import java.util.Set;

/**
 * Resolves test-section department IDs for notebook-scoped storage and QC APIs.
 * Centralizes fallbacks for child notebook instances (e.g. production instance 117)
 * so behavior matches Storage Management department filtering.
 */
public interface NotebookDepartmentScopeService {

    /**
     * Resolves department test-section IDs linked to a notebook template or instance.
     */
    Set<Integer> resolveNotebookDepartmentIds(Integer notebookId);

    /**
     * Same as {@link #resolveNotebookDepartmentIds(Integer)} but when {@code biorepositoryOnly}
     * is true and resolution is empty, falls back to Biorepository Laboratory.
     */
    Set<Integer> resolveNotebookDepartmentIds(Integer notebookId, boolean biorepositoryOnly);

    /**
     * Department display names for the given IDs (used for path/name matching).
     */
    Set<String> resolveNotebookDepartmentNames(Set<Integer> departmentIds);

    /**
     * Whether the notebook uses the biorepository workflow type.
     */
    boolean isBiorepositoryNotebook(Integer notebookId);
}
