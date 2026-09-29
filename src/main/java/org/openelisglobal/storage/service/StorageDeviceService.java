package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.security.access.prepost.PreAuthorize;

public interface StorageDeviceService extends BaseObjectService<StorageDevice, Integer> {
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageDevice> findByParentRoomId(Integer roomId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageDevice findByParentRoomIdAndCode(Integer roomId, String code);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageDevice findByCode(String code);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageDevice findByCodeAndParentRoom(String code, StorageRoom parentRoom);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countByRoomId(Integer roomId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageDevice findByNameAndParentRoomId(String name, Integer parentRoomId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    Optional<StorageDevice> getDevice(int id);
}
