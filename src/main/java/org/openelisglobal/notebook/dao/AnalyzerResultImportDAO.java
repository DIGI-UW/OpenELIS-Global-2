package org.openelisglobal.notebook.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.AnalyzerResultImport;

/** DAO interface for AnalyzerResultImport entity operations. */
public interface AnalyzerResultImportDAO extends BaseDAO<AnalyzerResultImport, Integer> {

    /**
     * Get all import records for a notebook page.
     *
     * @param notebookPageId notebook page ID
     * @return list of import records ordered by import date descending
     */
    List<AnalyzerResultImport> getByNotebookPageId(Integer notebookPageId);

    /**
     * Get the most recent import for a notebook page.
     *
     * @param notebookPageId notebook page ID
     * @return most recent import record or null
     */
    AnalyzerResultImport getLatestByNotebookPageId(Integer notebookPageId);

    /**
     * Get all imports for a specific analyzer.
     *
     * @param analyzerId analyzer ID
     * @return list of import records
     */
    List<AnalyzerResultImport> getByAnalyzerId(Integer analyzerId);

    /**
     * Get imports by user.
     *
     * @param userId user ID
     * @return list of import records
     */
    List<AnalyzerResultImport> getByImportedBy(String userId);

    /**
     * Get total successful rows imported for a notebook page.
     *
     * @param notebookPageId notebook page ID
     * @return sum of successful rows across all imports
     */
    long getTotalSuccessfulRows(Integer notebookPageId);

    /**
     * Check if there are any failed imports for a notebook page.
     *
     * @param notebookPageId notebook page ID
     * @return true if any import has failed rows
     */
    boolean hasFailedImports(Integer notebookPageId);
}
