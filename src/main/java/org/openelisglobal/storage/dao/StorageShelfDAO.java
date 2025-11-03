package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageShelf;

public interface StorageShelfDAO extends BaseDAO<StorageShelf, String> {
    List<StorageShelf> findByParentDeviceId(String deviceId);
}
