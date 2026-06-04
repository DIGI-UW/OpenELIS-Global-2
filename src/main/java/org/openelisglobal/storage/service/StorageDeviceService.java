package org.openelisglobal.storage.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;

public interface StorageDeviceService extends BaseObjectService<StorageDevice, Integer> {
    List<StorageDevice> findByParentRoomId(Integer roomId);

    StorageDevice findByParentRoomIdAndCode(Integer roomId, String code);

    StorageDevice findByCode(String code);

    StorageDevice findByCodeAndParentRoom(String code, StorageRoom parentRoom);

    int countByRoomId(Integer roomId);

    StorageDevice findByNameAndParentRoomId(String name, Integer parentRoomId);
}
