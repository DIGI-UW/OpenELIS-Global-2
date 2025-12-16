package org.openelisglobal.inventory.action;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.inventory.service.InventoryLocationService;
import org.openelisglobal.inventory.service.InventoryReceiptService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLocation;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class InventoryUtility {

    public static final String HIV = "HIV";
    public static final String SYPHILIS = "SYPHILIS";

    private boolean onlyActiveInventory = false;

    @Autowired
    InventoryReceiptService receiptService;
    @Autowired
    InventoryLocationService locationService;

    public List<InventoryKitItem> getExistingActiveInventory() {
        onlyActiveInventory = true;

        try {
            return getExistingInventory();
        } finally {
            onlyActiveInventory = false;
        }
    }

    public List<InventoryKitItem> getExistingInventory() {
        List<InventoryKitItem> list = new ArrayList<>();

        List<InventoryLocation> inventoryList = locationService.getAll();

        for (InventoryLocation location : inventoryList) {

            InventoryItem inventoryItem = location.getInventoryItem();

            if (!onlyActiveInventory || isActive(inventoryItem)) {
                InventoryReceipt receipt = receiptService.getInventoryReceiptByInventoryItemId(inventoryItem.getId());
                InventoryKitItem item = createInventoryItem(inventoryItem, location, receipt);
                list.add(item);
            }
        }

        return list;
    }

    private InventoryKitItem createInventoryItem(InventoryItem item, InventoryLocation location,
            InventoryReceipt receipt) {

        InventoryKitItem kitItem = new InventoryKitItem();

        kitItem.setType(item.getDescription());
        kitItem.setKitName(item.getName());
        kitItem.setReceiveDate(DateUtil.convertTimestampToStringDate(receipt.getReceivedDate()));
        kitItem.setExpirationDate(DateUtil.convertTimestampToStringDate(location.getExpirationDate()));
        kitItem.setLotNumber(location.getLotNumber());
        kitItem.setSource(receipt.getOrganization().getOrganizationName());
        kitItem.setOrganizationId(receipt.getOrganization().getId());
        kitItem.setInventoryItemId(item.getId());
        kitItem.setInventoryLocationId(location.getId());
        kitItem.setInventoryReceiptId(receipt.getId());
        kitItem.setIsActive(isActive(item));

        return kitItem;
    }

    private boolean isActive(InventoryItem inventoryItem) {
        return "Y".equals(inventoryItem.getIsActive());
    }
}
