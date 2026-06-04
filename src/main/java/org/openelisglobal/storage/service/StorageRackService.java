package org.openelisglobal.storage.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;

public interface StorageRackService extends BaseObjectService<StorageRack, Integer> {
    List<StorageRack> findByParentShelfId(Integer shelfId);

    StorageRack findByLabel(String label);

    StorageRack findByLabelAndParentShelf(String label, StorageShelf parentShelf);

    int countByShelfId(Integer shelfId);

    StorageRack findByLabelAndParentShelfId(String label, Integer parentShelfId);

    StorageRack findByCode(String code);

}
