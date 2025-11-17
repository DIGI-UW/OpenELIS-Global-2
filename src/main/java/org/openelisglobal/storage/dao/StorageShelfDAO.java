package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageShelf;

public interface StorageShelfDAO extends BaseDAO<StorageShelf, Integer> {
    List<StorageShelf> findByParentDeviceId(Integer deviceId);

    /**
     * Find shelf by label (anywhere in database) - for existence check
     *
     * @param label Shelf label
     * @return StorageShelf or null if not found
     */
    StorageShelf findByLabel(String label);

    /**
     * Find shelf by label and parent device (for barcode validation)
     *
     * @param label        Shelf label
     * @param parentDevice Parent device entity
     * @return StorageShelf or null if not found
     */
    StorageShelf findByLabelAndParentDevice(String label, StorageDevice parentDevice);

    /**
     * Count shelves by parent device ID (for constraint validation)
     *
     * @param deviceId Parent device ID
     * @return Count of shelves in the device
     */
    int countByDeviceId(Integer deviceId);

    /**
     * Find shelf by short code (for label management) TODO: Add shortCode field to
     * StorageShelf entity in Phase 5.4
     *
     * @param shortCode Short code
     * @return StorageShelf or null if not found
     */
    StorageShelf findByShortCode(String shortCode);
}
