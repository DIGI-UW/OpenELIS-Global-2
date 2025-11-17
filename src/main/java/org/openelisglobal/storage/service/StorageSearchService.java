package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Storage Search operations. Provides tab-specific search
 * functionality per FR-064 and FR-064a (Phase 3.1 in plan.md).
 * 
 * All searches use case-insensitive partial/substring matching with OR logic
 * (matches any of the specified fields).
 */
public interface StorageSearchService {

    /**
     * Search SampleItems by SampleItem ID, SampleItem External ID, parent Sample
     * accession number, and assigned location (full hierarchical path). Matches ANY
     * of these fields (OR logic).
     * 
     * @param query Search term (case-insensitive partial match)
     * @return List of matching SampleItems with id, sampleItemId,
     *         sampleItemExternalId, sampleAccessionNumber, type, status, location,
     *         assignedBy, date
     */
    List<Map<String, Object>> searchSamples(String query);

    /**
     * Search rooms by name and code. Matches name OR code (OR logic).
     * 
     * @param query Search term (case-insensitive partial match)
     * @return List of matching rooms as Maps with all data resolved (API format)
     */
    List<Map<String, Object>> searchRooms(String query);

    /**
     * Search devices by name, code, and type. Matches name OR code OR type (OR
     * logic).
     * 
     * @param query Search term (case-insensitive partial match)
     * @return List of matching devices as Maps with all data resolved (API format)
     */
    List<Map<String, Object>> searchDevices(String query);

    /**
     * Search shelves by label (name).
     * 
     * @param query Search term (case-insensitive partial match)
     * @return List of matching shelves as Maps with all data resolved (API format)
     */
    List<Map<String, Object>> searchShelves(String query);

    /**
     * Search racks by label (name).
     * 
     * @param query Search term (case-insensitive partial match)
     * @return List of matching racks as Maps with all data resolved (API format)
     */
    List<Map<String, Object>> searchRacks(String query);
}
