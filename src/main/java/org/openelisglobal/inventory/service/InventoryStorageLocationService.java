package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;

/**
 * Service interface for InventoryStorageLocation operations
 */
public interface InventoryStorageLocationService extends BaseObjectService<InventoryStorageLocation, String> {

    /**
     * Get all active storage locations
     */
    List<InventoryStorageLocation> getAllActive();

    /**
     * Get storage locations by type
     */
    List<InventoryStorageLocation> getByLocationType(LocationType locationType);

    /**
     * Get child locations of a parent location
     */
    List<InventoryStorageLocation> getChildLocations(String parentLocationId);

    /**
     * Get top-level locations (no parent)
     */
    List<InventoryStorageLocation> getTopLevelLocations();

    /**
     * Get location by location code
     */
    InventoryStorageLocation getByLocationCode(String locationCode);

    /**
     * Get location by FHIR UUID
     */
    InventoryStorageLocation getByFhirUuid(String fhirUuid);

    /**
     * Deactivate a storage location (soft delete)
     */
    void deactivateLocation(String locationId, String sysUserId);

    /**
     * Check if a location has any active lots stored
     */
    boolean hasActiveLots(String locationId);

    /**
     * Get full location path (e.g., "Main Lab > Refrigerator > Shelf A")
     */
    String getLocationPath(String locationId);
}
