package org.openelisglobal.storage.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.security.access.prepost.PreAuthorize;

public interface StorageRoomService extends BaseObjectService<StorageRoom, Integer> {
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageRoom findByCode(String code);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageRoom findByName(String name);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    Optional<StorageRoom> getRoom(int id);
}
