package org.openelisglobal.inventory.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import java.io.Serializable;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidDate;

public class InventoryKitItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String inventoryItemId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { Default.class, InventoryForm.ManageInventory.class })
    private String inventoryLocationId;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String inventoryReceiptId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { Default.class, InventoryForm.ManageInventory.class })
    private String OrganizationId;

    @Pattern(regexp = "^$|^HIV$|^SYPHILLIS$", groups = { Default.class, InventoryForm.ManageInventory.class })
    private String type;

    @NotBlank(groups = { Default.class, InventoryForm.ManageInventory.class })
    @SafeHtml(groups = { Default.class, InventoryForm.ManageInventory.class })
    private String kitName;

    @ValidDate(relative = DateRelation.ANY, groups = { Default.class, InventoryForm.ManageInventory.class })
    private String receiveDate;

    @ValidDate(relative = DateRelation.ANY, groups = { Default.class, InventoryForm.ManageInventory.class })
    private String expirationDate;

    @Pattern(regexp = "^[0-9]*$", groups = { Default.class, InventoryForm.ManageInventory.class })
    private String lotNumber;

    @SafeHtml(groups = { Default.class, InventoryForm.ManageInventory.class })
    private String source;

    private boolean isActive = false;

    private boolean isModified;

    public String getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public String getInventoryLocationId() {
        return inventoryLocationId;
    }

    public void setInventoryLocationId(String inventoryLocationId) {
        this.inventoryLocationId = inventoryLocationId;
    }

    public String getInventoryReceiptId() {
        return inventoryReceiptId;
    }

    public void setInventoryReceiptId(String inventoryReceiptId) {
        this.inventoryReceiptId = inventoryReceiptId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKitName() {
        return kitName;
    }

    public void setKitName(String name) {
        kitName = name;
    }

    public String getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(String receiveDate) {
        this.receiveDate = receiveDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public boolean getIsModified() {
        return isModified;
    }

    public void setIsModified(boolean isModified) {
        this.isModified = isModified;
    }

    public void setOrganizationId(String organizationId) {
        OrganizationId = organizationId;
    }

    public String getOrganizationId() {
        return OrganizationId;
    }
}
