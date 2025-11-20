package org.openelisglobal.inventory.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidDate;

/**
 * Form DTO for inventory item operations via REST API
 */
public class InventoryKitItemForm {

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String inventoryItemId;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String inventoryLocationId;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String inventoryReceiptId;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String organizationId;

    @Pattern(regexp = "^$|^HIV$|^SYPHILIS$")
    private String type;

    @NotBlank
    @SafeHtml
    private String kitName;

    @ValidDate(relative = DateRelation.ANY)
    private String receiveDate;

    @ValidDate(relative = DateRelation.ANY)
    private String expirationDate;

    @Pattern(regexp = "^[0-9]*$")
    private String lotNumber;

    @SafeHtml
    private String source;

    private Boolean isActive;

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

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
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

    public void setKitName(String kitName) {
        this.kitName = kitName;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
