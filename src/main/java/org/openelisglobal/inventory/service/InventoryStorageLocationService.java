package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service interface for InventoryStorageLocation operations
 */
public interface InventoryStorageLocationService extends BaseObjectService<InventoryStorageLocation, Long> {

    /**
     * Get all active storage locations
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryStorageLocation> getAllActive();

    /**
     * Get storage locations by type
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryStorageLocation> getByLocationType(LocationType locationType);

    /**
     * Get child locations of a parent location
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryStorageLocation> getChildLocations(Long parentLocationId);

    /**
     * Get top-level locations (no parent)
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryStorageLocation> getTopLevelLocations();

    /**
     * Get location by location code
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    InventoryStorageLocation getByLocationCode(String locationCode);

    /**
     * Get location by FHIR UUID
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    InventoryStorageLocation getByFhirUuid(String fhirUuid);

    /**
     * Deactivate a storage location (soft delete)
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
    void deactivateLocation(Long locationId, String sysUserId);

    /**
     * Check if a location has any active lots stored
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    boolean hasActiveLots(Long locationId);

    /**
     * Get full location path (e.g., "Main Lab > Refrigerator > Shelf A")
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    String getLocationPath(Long locationId);
}
