package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;

/**
 * Service interface for Storage Dashboard filtering operations. Provides
 * tab-specific filter methods with AND logic combination per FR-066.
 */
public interface StorageDashboardService {

    /**
     * Filter samples by location and status (AND logic).
     * 
     * @param location Location filter (hierarchical path substring)
     * @param status   Status filter (active, disposed, etc.)
     * @return Filtered list of samples matching both criteria
     */
    List<Map<String, Object>> filterSamples(String location, String status);

    /**
     * Filter rooms by status.
     * 
     * @param activeStatus true for active, false for inactive
     * @return Filtered list of rooms matching status
     */
    List<StorageRoom> filterRooms(Boolean activeStatus);

    /**
     * Filter rooms by status and return as Maps (API format).
     * 
     * @param activeStatus true for active, false for inactive
     * @return Filtered list of rooms as Maps with all data resolved
     */
    List<Map<String, Object>> filterRoomsForAPI(Boolean activeStatus);

    /**
     * Filter devices by type, roomId, and status (AND logic).
     * 
     * @param deviceType   Device type filter (FREEZER, REFRIGERATOR, etc.)
     * @param roomId       Room ID filter
     * @param activeStatus true for active, false for inactive
     * @return Filtered list of devices matching all three criteria
     */
    List<StorageDevice> filterDevices(StorageDevice.DeviceType deviceType, Integer roomId, Boolean activeStatus);

    /**
     * Filter devices by type, roomId, and status and return as Maps (API format).
     * 
     * @param deviceType   Device type filter (FREEZER, REFRIGERATOR, etc.)
     * @param roomId       Room ID filter
     * @param activeStatus true for active, false for inactive
     * @return Filtered list of devices as Maps with all data resolved
     */
    List<Map<String, Object>> filterDevicesForAPI(StorageDevice.DeviceType deviceType, Integer roomId,
            Boolean activeStatus);

    /**
     * Filter shelves by deviceId, roomId, and status (AND logic).
     * 
     * @param deviceId     Device ID filter
     * @param roomId       Room ID filter
     * @param activeStatus true for active, false for inactive
     * @return Filtered list of shelves matching all three criteria
     */
    List<StorageShelf> filterShelves(Integer deviceId, Integer roomId, Boolean activeStatus);

    /**
     * Filter shelves by deviceId, roomId, and status and return as Maps (API
     * format).
     * 
     * @param deviceId     Device ID filter
     * @param roomId       Room ID filter
     * @param activeStatus true for active, false for inactive
     * @return Filtered list of shelves as Maps with all data resolved
     */
    List<Map<String, Object>> filterShelvesForAPI(Integer deviceId, Integer roomId, Boolean activeStatus);

    /**
     * Filter racks by roomId, shelfId, deviceId, and status (AND logic).
     * 
     * @param roomId       Room ID filter
     * @param shelfId      Shelf ID filter
     * @param deviceId     Device ID filter
     * @param activeStatus true for active, false for inactive
     * @return Filtered list of racks matching all four criteria
     */
    List<StorageRack> filterRacks(Integer roomId, Integer shelfId, Integer deviceId, Boolean activeStatus);

    /**
     * Get racks for API with filters and room column included (FR-065a).
     * 
     * @param roomId       Optional room ID filter
     * @param shelfId      Optional shelf ID filter
     * @param deviceId     Optional device ID filter
     * @param activeStatus Optional active status filter
     * @return List of racks as Maps with roomId column included
     */
    List<Map<String, Object>> getRacksForAPI(Integer roomId, Integer shelfId, Integer deviceId, Boolean activeStatus);

    /**
     * Get location counts by type for active locations only (FR-057, FR-057a).
     * Returns counts for Room, Device, Shelf, and Rack levels (Position excluded).
     * Only counts active (non-decommissioned) locations.
     * 
     * @return Map with keys: "rooms", "devices", "shelves", "racks" and integer
     *         count values
     */
    Map<String, Integer> getLocationCountsByType();
}
