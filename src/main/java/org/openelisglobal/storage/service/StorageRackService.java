package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.security.access.prepost.PreAuthorize;

public interface StorageRackService extends BaseObjectService<StorageRack, Integer> {
    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    List<StorageRack> findByParentShelfId(Integer shelfId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageRack findByLabel(String label);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageRack findByLabelAndParentShelf(String label, StorageShelf parentShelf);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    int countByShelfId(Integer shelfId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageRack findByLabelAndParentShelfId(String label, Integer parentShelfId);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    StorageRack findByCode(String code);

    @PreAuthorize("hasAuthority('PRIV_STORAGE_VIEW')")
    Optional<StorageRack> getRack(int id);

}
