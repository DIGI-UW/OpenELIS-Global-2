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

    StorageShelf findByLabelAndParentDeviceId(String label, Integer parentDeviceId);

    /**
     * Find shelf by code (for code uniqueness validation)
     *
     * @param code Shelf code
     * @return StorageShelf or null if not found
     */
    StorageShelf findByCode(String code);

}
