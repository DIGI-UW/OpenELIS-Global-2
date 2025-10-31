package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageRack;

public interface StorageRackDAO extends BaseDAO<StorageRack, String> {
    List<StorageRack> findByParentShelfId(String shelfId);
}

