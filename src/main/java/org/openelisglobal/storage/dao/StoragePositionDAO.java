package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StoragePosition;
import org.openelisglobal.storage.valueholder.StorageRack;

public interface StoragePositionDAO extends BaseDAO<StoragePosition, Integer> {
    List<StoragePosition> findByParentRackId(Integer rackId);

    List<StoragePosition> findByParentDeviceId(Integer deviceId);

    List<StoragePosition> findByParentShelfId(Integer shelfId);

    /**
     * Find position by coordinates (anywhere in database) - for existence check
     *
     * @param coordinates Position coordinates
     * @return StoragePosition or null if not found
     */
    StoragePosition findByCoordinates(String coordinates);

    /**
     * Find position by coordinates and parent rack (for barcode validation)
     *
     * @param coordinates Position coordinates
     * @param parentRack  Parent rack entity
     * @return StoragePosition or null if not found
     */
    StoragePosition findByCoordinatesAndParentRack(String coordinates, StorageRack parentRack);

    List<StoragePosition> findPositionsByHierarchyLevel(int level);

    int countOccupied(Integer rackId);

    int countOccupiedInDevice(Integer deviceId);

    int countOccupiedInShelf(Integer shelfId);

    boolean validateHierarchyIntegrity(Integer positionId);
}
