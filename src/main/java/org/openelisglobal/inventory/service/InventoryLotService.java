package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryLot;

public interface InventoryLotService extends BaseObjectService<InventoryLot, String> {

    List<InventoryLot> findByInventoryItemId(String inventoryItemId);

    List<InventoryLot> findAvailableLotsByItemFEFO(String inventoryItemId);

    List<InventoryLot> findExpiringSoon(int daysAhead);

    List<InventoryLot> findExpiredActiveLots();
}
