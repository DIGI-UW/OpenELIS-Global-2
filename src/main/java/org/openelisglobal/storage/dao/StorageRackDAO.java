package org.openelisglobal.storage.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;

public interface StorageRackDAO extends BaseDAO<StorageRack, Integer> {
    List<StorageRack> findByParentShelfId(Integer shelfId);

    /**
     * Find rack by label (anywhere in database) - for existence check
     *
     * @param label Rack label
     * @return StorageRack or null if not found
     */
    StorageRack findByLabel(String label);

    /**
     * Find rack by label and parent shelf (for barcode validation)
     *
     * @param label       Rack label
     * @param parentShelf Parent shelf entity
     * @return StorageRack or null if not found
     */
    StorageRack findByLabelAndParentShelf(String label, StorageShelf parentShelf);

    /**
     * Count racks by parent shelf ID (for constraint validation)
     *
     * @param shelfId Parent shelf ID
     * @return Count of racks in the shelf
     */
    int countByShelfId(Integer shelfId);

    StorageRack findByLabelAndParentShelfId(String label, Integer parentShelfId);

    /**
     * Find rack by code (for code uniqueness validation)
     *
     * @param code Rack code
     * @return StorageRack or null if not found
     */
    StorageRack findByCode(String code);

}
