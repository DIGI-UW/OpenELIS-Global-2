package org.openelisglobal.storage.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageRack;

public interface StorageBoxService extends BaseObjectService<StorageBox, Integer> {
    List<StorageBox> findByParentRackId(Integer rackId);

    StorageBox findByCoordinates(String coordinates);

    StorageBox findByCoordinatesAndParentRack(String coordinates, StorageRack parentRack);

    int countOccupied(Integer rackId);

    int countOccupiedInShelf(Integer shelfId);

    int countOccupiedInDevice(Integer deviceId);
}
