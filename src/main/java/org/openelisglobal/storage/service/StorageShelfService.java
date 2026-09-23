package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageShelf;

public interface StorageShelfService extends BaseObjectService<StorageShelf, Integer> {
    List<StorageShelf> findByParentDeviceId(Integer deviceId);

    StorageShelf findByLabel(String label);

    StorageShelf findByLabelAndParentDevice(String label, StorageDevice parentDevice);

    int countByDeviceId(Integer deviceId);

    StorageShelf findByLabelAndParentDeviceId(String label, Integer parentDeviceId);

    StorageShelf findByCode(String code);

    Optional<StorageShelf> getShelf(int id);

}
