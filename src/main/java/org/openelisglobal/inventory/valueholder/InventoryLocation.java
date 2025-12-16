package org.openelisglobal.inventory.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;

public class InventoryLocation extends BaseObject<String> {

    private static final long serialVersionUID = 1L;
    private String id;
    private String lotNumber;
    private Timestamp expirationDate;
    private ValueHolderInterface inventoryItem = new ValueHolder();
    private String inventoryItemId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public Timestamp getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Timestamp expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public void setInventoryItem(ValueHolderInterface inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public void setInventoryItemHolder(ValueHolderInterface inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem.setValue(inventoryItem);
    }

    public ValueHolderInterface getInventoryItemHolder() {
        return inventoryItem;
    }

    public InventoryItem getInventoryItem() {
        return (InventoryItem) inventoryItem.getValue();
    }
}
