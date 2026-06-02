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
    default Map<String, Object> buildStorageOverview(String freezerFilter, String shelfFilter, String rackFilter,
            String boxFilter, boolean includeAllQcVisits, Integer notebookId) {
        return buildStorageOverview(freezerFilter, shelfFilter, rackFilter, boxFilter, includeAllQcVisits, notebookId,
                false, null, null);
    }

    /**
     * Storage overview for QC Inspection. When {@code summaryOnly} is true, counts and
     * diagnostics are returned without building the full {@code eligibleSamples} list.
     */
    Map<String, Object> buildStorageOverview(String freezerFilter, String shelfFilter, String rackFilter,
            String boxFilter, boolean includeAllQcVisits, Integer notebookId, boolean summaryOnly,
            Integer eligibleLimit, Integer eligibleOffset);

    /**
     * Sample rows for the QC table (same shape as legacy {@code GET /samples}).
     */
    List<Map<String, Object>> listSamplesForQcTable(Integer notebookId);
}
