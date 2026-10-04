package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.springframework.security.access.prepost.PreAuthorize;

public interface StorageBoxService extends BaseObjectService<StorageBox, Integer> {
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageBox> findByParentRackId(Integer rackId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageBox findByCoordinates(String coordinates);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageBox findByCoordinatesAndParentRack(String coordinates, StorageRack parentRack);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countOccupied(Integer rackId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countOccupiedInShelf(Integer shelfId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countOccupiedInDevice(Integer deviceId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    Optional<StorageBox> getBox(int id);
}
