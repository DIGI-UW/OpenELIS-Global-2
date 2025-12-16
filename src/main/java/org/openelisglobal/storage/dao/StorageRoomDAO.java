package org.openelisglobal.storage.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageRoom;

public interface StorageRoomDAO extends BaseDAO<StorageRoom, Integer> {
    StorageRoom findByCode(String code);
}
