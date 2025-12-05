package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.StorageLocation;

public interface StorageLocationService extends BaseObjectService<StorageLocation, String> {

    List<StorageLocation> findAllActive();

    StorageLocation findByLocationCode(String locationCode);

    List<StorageLocation> findByParentLocationId(String parentLocationId);

    List<StorageLocation> findRootLocations();
}
