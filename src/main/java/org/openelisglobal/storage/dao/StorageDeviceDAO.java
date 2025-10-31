package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;

public interface StorageDeviceDAO extends BaseDAO<StorageDevice, String> {
    List<StorageDevice> findByParentRoomId(String roomId);
    StorageDevice findByParentRoomIdAndCode(String roomId, String code);
}

