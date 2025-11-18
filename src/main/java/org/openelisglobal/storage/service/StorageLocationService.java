package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StoragePosition;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;

public interface StorageLocationService {
    // Room methods
    List<StorageRoom> getRooms();

    StorageRoom getRoom(Integer id);

    StorageRoom createRoom(StorageRoom room);

    StorageRoom updateRoom(Integer id, StorageRoom room);

    void deleteRoom(Integer id);

    // Device methods
    List<StorageDevice> getDevicesByRoom(Integer roomId);

    List<StorageDevice> getAllDevices();

    // Shelf methods
    List<StorageShelf> getShelvesByDevice(Integer deviceId);

    List<StorageShelf> getAllShelves();

    // Rack methods
    List<StorageRack> getRacksByShelf(Integer shelfId);

    List<StorageRack> getAllRacks();

    // Position methods
    List<StoragePosition> getPositionsByRack(Integer rackId);

    List<StoragePosition> getAllPositions();

    // REST API methods - return fully prepared Maps with all relationship data
    List<Map<String, Object>> getRoomsForAPI();

    List<Map<String, Object>> getDevicesForAPI(Integer roomId);

    List<Map<String, Object>> getShelvesForAPI(Integer deviceId);

    List<Map<String, Object>> getRacksForAPI(Integer shelfId);

    // Count methods
    int countOccupiedInDevice(Integer deviceId);

    int countOccupied(Integer rackId);

    int countOccupiedInShelf(Integer shelfId);

    // Generic CRUD methods
    Integer insert(Object entity);

    Integer update(Object entity);

    void delete(Object entity);

    Object get(Integer id, Class<?> entityClass);

    // Validation methods
    boolean validateLocationActive(StoragePosition position);

    String buildHierarchicalPath(StoragePosition position);

    // Search methods
    /**
     * Search locations across all hierarchy levels (Room, Device, Shelf, Rack)
     * Returns locations matching search term with full hierarchical paths
     * 
     * @param searchTerm Search term (case-insensitive partial match)
     * @return List of matching locations as Maps with hierarchicalPath field
     */
    List<Map<String, Object>> searchLocations(String searchTerm);

    // Phase 6: Location CRUD Operations - Constraint Validation Methods

    /**
     * Validate if a location entity can be deleted (no child locations, no active
     * samples)
     * 
     * @param locationEntity Location entity to validate (Room, Device, Shelf, or
     *                       Rack)
     * @return true if location can be deleted, false if constraints exist
     */
    boolean validateDeleteConstraints(Object locationEntity);

    /**
     * Check if a location can be deleted
     * 
     * @param locationEntity Location entity to check
     * @return true if location can be deleted, false if constraints exist
     */
    boolean canDeleteLocation(Object locationEntity);

    /**
     * Get user-friendly error message explaining why a location cannot be deleted
     * 
     * @param locationEntity Location entity that cannot be deleted
     * @return Error message explaining the constraint (e.g., "Cannot delete Room
     *         'Main Laboratory' because it contains 8 devices")
     */
    String getDeleteConstraintMessage(Object locationEntity);
}
