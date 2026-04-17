package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.security.access.prepost.PreAuthorize;

public interface StorageLocationService {
    // Room methods
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageRoom> getRooms();

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageRoom getRoom(Integer id);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    StorageRoom createRoom(StorageRoom room);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    StorageRoom updateRoom(Integer id, StorageRoom room);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    void deleteRoom(Integer id);

    // Device methods
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageDevice> getDevicesByRoom(Integer roomId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageDevice> getAllDevices();

    // Shelf methods
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageShelf> getShelvesByDevice(Integer deviceId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageShelf> getAllShelves();

    // Rack methods
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageRack> getRacksByShelf(Integer shelfId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageRack> getAllRacks();

    // Box methods
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageBox> getBoxesByRack(Integer rackId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageBox> getAllBoxes();

    // REST API methods - return fully prepared Maps with all relationship data
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<Map<String, Object>> getRoomsForAPI();

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<Map<String, Object>> getDevicesForAPI(Integer roomId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<Map<String, Object>> getShelvesForAPI(Integer deviceId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<Map<String, Object>> getRacksForAPI(Integer shelfId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<Map<String, Object>> getBoxesForAPI(Integer rackId);

    // Count methods
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countOccupiedInDevice(Integer deviceId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countOccupied(Integer rackId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countOccupiedInShelf(Integer shelfId);

    // Generic CRUD methods
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    Integer insert(Object entity);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    Integer update(Object entity);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    void delete(Object entity);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    Object get(Integer id, Class<?> entityClass);

    // Validation methods
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    boolean validateLocationActive(StorageBox box);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    String buildHierarchicalPath(StorageBox box);

    // Search methods
    /**
     * Search locations across all hierarchy levels (Room, Device, Shelf, Rack)
     * Returns locations matching search term with full hierarchical paths
     * 
     * @param searchTerm Search term (case-insensitive partial match)
     * @return List of matching locations as Maps with hierarchicalPath field
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
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
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    boolean validateDeleteConstraints(Object locationEntity);

    /**
     * Check if a location can be deleted
     * 
     * @param locationEntity Location entity to check
     * @return true if location can be deleted, false if constraints exist
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    boolean canDeleteLocation(Object locationEntity);

    /**
     * Get user-friendly error message explaining why a location cannot be deleted
     * 
     * @param locationEntity Location entity that cannot be deleted
     * @return Error message explaining the constraint (e.g., "Cannot delete Room
     *         'Main Laboratory' because it contains 8 devices")
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    String getDeleteConstraintMessage(Object locationEntity);

    /**
     * OGC-75: Get summary of what will be deleted in a cascade delete operation
     * 
     * @param locationEntity Location entity to get cascade delete summary for
     * @return Map containing: childLocations (Map with counts by type), sampleCount
     *         (int), childLocationType (String - type of child locations),
     *         childLocationCount (int)
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
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
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    Map<String, Object> canMoveLocation(Object locationEntity, Integer newParentId);

    /**
     * OGC-75: Delete location with cascade deletion of all child locations and
     * unassignment of all samples
     * 
     * @param id            Location ID
     * @param locationClass Location entity class (StorageRoom, StorageDevice,
     *                      StorageShelf, or StorageRack)
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    void deleteLocationWithCascade(Integer id, Class<?> locationClass);

    // Deletion Validation Methods

    /**
     * Check if a Room can be deleted (no child devices).
     *
     * @param roomId Room ID to check
     * @return DeletionValidationResult with success/error details
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    DeletionValidationResult canDeleteRoom(Integer roomId);

    /**
     * Check if a Device can be deleted (no child shelves).
     *
     * @param deviceId Device ID to check
     * @return DeletionValidationResult with success/error details
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    DeletionValidationResult canDeleteDevice(Integer deviceId);

    /**
     * Check if a Shelf can be deleted (no child racks).
     *
     * @param shelfId Shelf ID to check
     * @return DeletionValidationResult with success/error details
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
    DeletionValidationResult canDeleteShelf(Integer shelfId);

    /**
     * Check if a Rack can be deleted (no assigned samples).
     *
     * @param rackId Rack ID to check
     * @return DeletionValidationResult with success/error details
     */
    @PreAuthorize("hasAuthority('PRIV_STORAGE_MANAGE')")
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
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    boolean isNameUniqueWithinParent(String name, Integer parentId, String locationType, Integer excludeId);

    // Code uniqueness validation methods (added per spec FR-037l1)
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    boolean isCodeUniqueForRoom(String code, Integer excludeId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    boolean isCodeUniqueForDevice(String code, Integer excludeId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    boolean isCodeUniqueForShelf(String code, Integer excludeId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    boolean isCodeUniqueForRack(String code, Integer excludeId);
}
