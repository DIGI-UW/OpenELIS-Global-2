package org.openelisglobal.storage.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Storage Search operations. Implements tab-specific
 * search functionality per FR-064 and FR-064a (Phase 3.1 in plan.md).
 * 
 * All searches use case-insensitive partial/substring matching with OR logic
 * (matches any of the specified fields).
 */
@Service
public class StorageSearchServiceImpl implements StorageSearchService {

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchSamples(String query) {
        List<Map<String, Object>> allSamples = sampleStorageService.getAllSamplesWithAssignments();

        // Empty or null query returns all samples
        if (query == null || query.trim().isEmpty()) {
            return allSamples;
        }

        String normalizedQuery = query.trim().toLowerCase();
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> sample : allSamples) {
            // Search by sample ID (exact match or substring)
            // Handle both Integer and String IDs
            Object idObj = sample.get("id");
            boolean matchesId = false;
            if (idObj != null) {
                String idStr = idObj instanceof Integer ? String.valueOf(idObj) : String.valueOf(idObj);
                matchesId = idStr.toLowerCase().contains(normalizedQuery);
            }

            // Search by accession prefix (type field)
            String type = (String) sample.get("type");
            boolean matchesType = type != null && type.toLowerCase().contains(normalizedQuery);

            // Search by location path (full hierarchical path)
            String location = (String) sample.get("location");
            boolean matchesLocation = location != null && location.toLowerCase().contains(normalizedQuery);

            // OR logic: matches if ANY field matches
            if (matchesId || matchesType || matchesLocation) {
                filtered.add(sample);
            }
        }

        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchRooms(String query) {
        // Get all rooms as fully populated Maps (with all data resolved within
        // transaction)
        List<Map<String, Object>> allRooms = storageLocationService.getRoomsForAPI();

        // Empty or null query returns all rooms
        if (query == null || query.trim().isEmpty()) {
            return allRooms;
        }

        String normalizedQuery = query.trim().toLowerCase();
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> room : allRooms) {
            // Search by name OR code (OR logic)
            String name = (String) room.get("name");
            String code = (String) room.get("code");
            boolean matchesName = name != null && name.toLowerCase().contains(normalizedQuery);
            boolean matchesCode = code != null && code.toLowerCase().contains(normalizedQuery);

            if (matchesName || matchesCode) {
                filtered.add(room);
            }
        }

        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchDevices(String query) {
        // Get all devices as fully populated Maps (with all data resolved within
        // transaction)
        List<Map<String, Object>> allDevices = storageLocationService.getDevicesForAPI(null);

        // Empty or null query returns all devices
        if (query == null || query.trim().isEmpty()) {
            return allDevices;
        }

        String normalizedQuery = query.trim().toLowerCase();
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> device : allDevices) {
            // Search by name OR code OR type (OR logic)
            String name = (String) device.get("name");
            String code = (String) device.get("code");
            String type = (String) device.get("type");
            boolean matchesName = name != null && name.toLowerCase().contains(normalizedQuery);
            boolean matchesCode = code != null && code.toLowerCase().contains(normalizedQuery);
            boolean matchesType = type != null && type.toLowerCase().contains(normalizedQuery);

            if (matchesName || matchesCode || matchesType) {
                filtered.add(device);
            }
        }

        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchShelves(String query) {
        // Get all shelves as fully populated Maps (with all data resolved within
        // transaction)
        List<Map<String, Object>> allShelves = storageLocationService.getShelvesForAPI(null);

        // Empty or null query returns all shelves
        if (query == null || query.trim().isEmpty()) {
            return allShelves;
        }

        String normalizedQuery = query.trim().toLowerCase();
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> shelf : allShelves) {
            // Search by label (name)
            String label = (String) shelf.get("label");
            if (label != null && label.toLowerCase().contains(normalizedQuery)) {
                filtered.add(shelf);
            }
        }

        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchRacks(String query) {
        // Get all racks as fully populated Maps (with all data resolved within
        // transaction)
        List<Map<String, Object>> allRacks = storageLocationService.getRacksForAPI(null);

        // Empty or null query returns all racks
        if (query == null || query.trim().isEmpty()) {
            return allRacks;
        }

        String normalizedQuery = query.trim().toLowerCase();
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> rack : allRacks) {
            // Search by label (name)
            String label = (String) rack.get("label");
            if (label != null && label.toLowerCase().contains(normalizedQuery)) {
                filtered.add(rack);
            }
        }

        return filtered;
    }
}
