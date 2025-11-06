package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;

public interface StorageDeviceDAO extends BaseDAO<StorageDevice, Integer> {
    List<StorageDevice> findByParentRoomId(Integer roomId);

    StorageDevice findByParentRoomIdAndCode(Integer roomId, String code);
}
