package org.openelisglobal.storage.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageRoom;

public interface StorageRoomService extends BaseObjectService<StorageRoom, Integer> {
    StorageRoom findByCode(String code);

    StorageRoom findByName(String name);
}
