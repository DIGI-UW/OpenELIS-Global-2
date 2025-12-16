package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageDevice;
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

    // Box methods
    List<StorageBox> getBoxesByRack(Integer rackId);

    List<StorageBox> getAllBoxes();

    // REST API methods - return fully prepared Maps with all relationship data
    List<Map<String, Object>> getRoomsForAPI();

    List<Map<String, Object>> getDevicesForAPI(Integer roomId);

    List<Map<String, Object>> getShelvesForAPI(Integer deviceId);

    List<Map<String, Object>> getRacksForAPI(Integer shelfId);

    List<Map<String, Object>> getBoxesForAPI(Integer rackId);

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
    boolean validateLocationActive(StorageBox box);

    String buildHierarchicalPath(StorageBox box);

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

    /**
     * OGC-75: Get summary of what will be deleted in a cascade delete operation
     * 
     * @param locationEntity Location entity to get cascade delete summary for
     * @return Map containing: childLocations (Map with counts by type), sampleCount
     *         (int), childLocationType (String - type of child locations),
     *         childLocationCount (int)
     */
    Map<String, Object> getCascadeDeleteSummary(Object locationEntity);

    /**
     * Check if a location can be moved to a new parent, and if samples exist
     * downstream
     * 
     * @param locationEntity Location entity to check (Device, Shelf, or Rack)
     * @param newParentId    ID of the new parent location
     * @return Map containing: canMove (boolean), hasDownstreamSamples (boolean),
     *         sampleCount (int), warning (String - optional warning message)
     */
    Map<String, Object> canMoveLocation(Object locationEntity, Integer newParentId);

    /**
     * OGC-75: Delete location with cascade deletion of all child locations and
     * unassignment of all samples
     * 
     * @param id            Location ID
     * @param locationClass Location entity class (StorageRoom, StorageDevice,
     *                      StorageShelf, or StorageRack)
     */
    void deleteLocationWithCascade(Integer id, Class<?> locationClass);

    // Deletion Validation Methods

    /**
     * Check if a Room can be deleted (no child devices).
     *
     * @param roomId Room ID to check
     * @return DeletionValidationResult with success/error details
     */
    DeletionValidationResult canDeleteRoom(Integer roomId);

    /**
     * Check if a Device can be deleted (no child shelves).
     *
     * @param deviceId Device ID to check
     * @return DeletionValidationResult with success/error details
     */
    DeletionValidationResult canDeleteDevice(Integer deviceId);

    /**
     * Check if a Shelf can be deleted (no child racks).
     *
     * @param shelfId Shelf ID to check
     * @return DeletionValidationResult with success/error details
     */
    DeletionValidationResult canDeleteShelf(Integer shelfId);

    /**
     * Check if a Rack can be deleted (no assigned samples).
     *
     * @param rackId Rack ID to check
     * @return DeletionValidationResult with success/error details
     */
    DeletionValidationResult canDeleteRack(Integer rackId);

    /**
     * Validate location name uniqueness within parent scope
     *
     * @param name         Location name to validate
     * @param parentId     Parent ID (null for rooms)
     * @param locationType One of: "room", "device", "shelf", "rack"
     * @param excludeId    Existing ID to exclude (for updates)
     * @return true if unique within scope, false otherwise
     */
    boolean isNameUniqueWithinParent(String name, Integer parentId, String locationType, Integer excludeId);

    // Code uniqueness validation methods (added per spec FR-037l1)
    boolean isCodeUniqueForRoom(String code, Integer excludeId);

    boolean isCodeUniqueForDevice(String code, Integer excludeId);

    boolean isCodeUniqueForShelf(String code, Integer excludeId);

    boolean isCodeUniqueForRack(String code, Integer excludeId);
}
