package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StoragePosition;

public interface StoragePositionDAO extends BaseDAO<StoragePosition, String> {
    List<StoragePosition> findByParentRackId(String rackId);
    int countOccupied(String rackId);
    int countOccupiedInDevice(String deviceId);
}

