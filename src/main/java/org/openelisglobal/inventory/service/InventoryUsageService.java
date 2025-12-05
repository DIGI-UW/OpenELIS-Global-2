package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryUsage;

public interface InventoryUsageService extends BaseObjectService<InventoryUsage, String> {

    List<InventoryUsage> findByLotId(String lotId);

    List<InventoryUsage> findByTestResultId(String testResultId);
}
