package org.openelisglobal.biorepository.service;

import java.util.List;
import java.util.Map;

/**
 * Builds the biorepository QC eligible sample pool from the same storage-assignment
 * source used by Storage Management ({@code SampleStorageService#getAllSamplesWithAssignments}).
 */
public interface BiorepositoryQcSamplePoolService {

    /**
     * Full storage-overview payload for the QC Inspection page (counts, filters, eligible list, scopeStats, diagnostics).
     */
    Map<String, Object> buildStorageOverview(String freezerFilter, String shelfFilter, String rackFilter,
            String boxFilter, boolean includeAllQcVisits, Integer notebookId);

    /**
     * Sample rows for the QC table (same shape as legacy {@code GET /samples}).
     */
    List<Map<String, Object>> listSamplesForQcTable(Integer notebookId);
}
