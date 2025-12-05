package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.inventory.valueholder.StorageLocation;

/**
 * DAO for StorageLocation entity Manages physical storage locations for
 * inventory (not for samples)
 */
public interface StorageLocationDAO extends BaseDAO<StorageLocation, String> {

    /**
     * Find all active storage locations
     *
     * @return List of active locations
     */
    List<StorageLocation> findAllActive();

    /**
     * Find storage location by location code
     *
     * @param locationCode Unique location code
     * @return StorageLocation or null if not found
     */
    StorageLocation findByLocationCode(String locationCode);

    /**
     * Find child locations by parent location ID
     *
     * @param parentLocationId Parent location ID
     * @return List of child locations
     */
    List<StorageLocation> findByParentLocationId(String parentLocationId);

    /**
     * Find top-level locations (no parent)
     *
     * @return List of root locations
     */
    List<StorageLocation> findRootLocations();
}
