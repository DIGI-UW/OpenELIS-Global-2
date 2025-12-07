package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;

public interface InventoryStorageLocationDAO extends BaseDAO<InventoryStorageLocation, Long> {

    /**
     * Get all active storage locations
     */
    List<InventoryStorageLocation> getAllActive() throws LIMSRuntimeException;

    /**
     * Get storage locations by type
     */
    List<InventoryStorageLocation> getByLocationType(LocationType locationType) throws LIMSRuntimeException;

    /**
     * Get child locations of a parent location
     */
    List<InventoryStorageLocation> getChildLocations(Long parentLocationId) throws LIMSRuntimeException;

    /**
     * Get top-level locations (no parent)
     */
    List<InventoryStorageLocation> getTopLevelLocations() throws LIMSRuntimeException;

    /**
     * Get storage location by location code
     */
    InventoryStorageLocation getByLocationCode(String locationCode) throws LIMSRuntimeException;

    /**
     * Get storage location by FHIR UUID
     */
    InventoryStorageLocation getByFhirUuid(String fhirUuid) throws LIMSRuntimeException;
}
