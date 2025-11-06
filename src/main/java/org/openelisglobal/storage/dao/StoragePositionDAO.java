package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StoragePosition;

public interface StoragePositionDAO extends BaseDAO<StoragePosition, Integer> {
    List<StoragePosition> findByParentRackId(Integer rackId);

    List<StoragePosition> findByParentDeviceId(Integer deviceId);

    List<StoragePosition> findByParentShelfId(Integer shelfId);

    List<StoragePosition> findPositionsByHierarchyLevel(int level);

    int countOccupied(Integer rackId);

    int countOccupiedInDevice(Integer deviceId);

    int countOccupiedInShelf(Integer shelfId);

    boolean validateHierarchyIntegrity(Integer positionId);
}
