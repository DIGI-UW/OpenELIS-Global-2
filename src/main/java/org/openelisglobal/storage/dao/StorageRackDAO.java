package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageRack;

public interface StorageRackDAO extends BaseDAO<StorageRack, Integer> {
    List<StorageRack> findByParentShelfId(Integer shelfId);

    /**
     * Count racks by parent shelf ID (for constraint validation)
     * 
     * @param shelfId Parent shelf ID
     * @return Count of racks in the shelf
     */
    int countByShelfId(Integer shelfId);
}
