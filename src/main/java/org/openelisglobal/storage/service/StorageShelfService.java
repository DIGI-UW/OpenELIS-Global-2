package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.security.access.prepost.PreAuthorize;

public interface StorageShelfService extends BaseObjectService<StorageShelf, Integer> {
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageShelf> findByParentDeviceId(Integer deviceId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageShelf findByLabel(String label);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageShelf findByLabelAndParentDevice(String label, StorageDevice parentDevice);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countByDeviceId(Integer deviceId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageShelf findByLabelAndParentDeviceId(String label, Integer parentDeviceId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageShelf findByCode(String code);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    Optional<StorageShelf> getShelf(int id);

}
