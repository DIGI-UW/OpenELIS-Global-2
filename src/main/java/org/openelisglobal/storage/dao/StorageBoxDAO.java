package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageRack;

public interface StorageBoxDAO extends BaseDAO<StorageBox, Integer> {
    List<StorageBox> findByParentRackId(Integer rackId);

    StorageBox findByCoordinates(String coordinates);

    StorageBox findByCoordinatesAndParentRack(String coordinates, StorageRack parentRack);

    int countOccupied(Integer rackId);

    int countOccupiedInShelf(Integer shelfId);

    int countOccupiedInDevice(Integer deviceId);
}
