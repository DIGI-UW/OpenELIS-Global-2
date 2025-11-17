package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;

public interface StorageDeviceDAO extends BaseDAO<StorageDevice, Integer> {
    List<StorageDevice> findByParentRoomId(Integer roomId);

    StorageDevice findByParentRoomIdAndCode(Integer roomId, String code);

    /**
     * Find device by code (anywhere in database) - for existence check
     *
     * @param code Device code
     * @return StorageDevice or null if not found
     */
    StorageDevice findByCode(String code);

    /**
     * Find device by code and parent room (for barcode validation)
     *
     * @param code       Device code
     * @param parentRoom Parent room entity
     * @return StorageDevice or null if not found
     */
    StorageDevice findByCodeAndParentRoom(String code, StorageRoom parentRoom);

    /**
     * Count devices by parent room ID (for constraint validation)
     *
     * @param roomId Parent room ID
     * @return Count of devices in the room
     */
    int countByRoomId(Integer roomId);

    /**
     * Find device by short code (for label management) TODO: Add shortCode field to
     * StorageDevice entity in Phase 5.4
     *
     * @param shortCode Short code
     * @return StorageDevice or null if not found
     */
    StorageDevice findByShortCode(String shortCode);
}
