package org.openelisglobal.storage.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageRoom;

public interface StorageRoomDAO extends BaseDAO<StorageRoom, Integer> {
    StorageRoom findByCode(String code);

    StorageRoom findByName(String name);

    /**
     * Find a room by name within a single owning department ({@code test_section}).
     * When {@code departmentTestSectionId} is null, matches only legacy rooms with
     * no department assigned.
     */
    StorageRoom findByNameAndDepartmentTestSectionId(String name, Integer departmentTestSectionId);
}
