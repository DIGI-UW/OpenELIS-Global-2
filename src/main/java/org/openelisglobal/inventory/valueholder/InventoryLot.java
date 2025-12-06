package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;

@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_lot")
public class InventoryLot extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @NotNull
    private InventoryItem inventoryItem;

    @ManyToOne
    @JoinColumn(name = "storage_location_id")
    private InventoryStorageLocation storageLocation;

    @Column(name = "lot_number", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String lotNumber;

    @Column(name = "expiration_date")
    private Timestamp expirationDate;

    @Column(name = "date_opened")
    private Timestamp dateOpened;

    @Column(name = "calculated_expiry_after_opening")
    private Timestamp calculatedExpiryAfterOpening;

    @Column(name = "receipt_date")
    private Timestamp receiptDate;

    @Column(name = "initial_quantity", nullable = false, precision = 10, scale = 2)
    @NotNull
    @Min(1)
    private Double initialQuantity;

    @Column(name = "current_quantity", nullable = false, precision = 10, scale = 2)
    @NotNull
    @Min(0)
    private Double currentQuantity;

    @Column(name = "qc_status", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private QCStatus qcStatus = QCStatus.PENDING;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private LotStatus status = LotStatus.ACTIVE;

    @Column(name = "barcode", length = 100, unique = true)
    private String barcode;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Business logic helper methods

    /**
     * Get the effective expiration date (earlier of manufacturer expiration or
     * expiry after opening)
     */
    public Timestamp getEffectiveExpirationDate() {
        if (calculatedExpiryAfterOpening != null && expirationDate != null) {
            return calculatedExpiryAfterOpening.before(expirationDate) ? calculatedExpiryAfterOpening : expirationDate;
        }
        if (calculatedExpiryAfterOpening != null) {
            return calculatedExpiryAfterOpening;
        }
        return expirationDate;
    }

    /**
     * Check if lot is expired based on effective expiration date
     */
    public boolean isExpired() {
        Timestamp effectiveExpiration = getEffectiveExpirationDate();
        if (effectiveExpiration == null) {
            return false;
        }
        return effectiveExpiration.before(new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Check if lot is available for use (not expired, not disposed, QC passed, has
     * quantity)
     */
    public boolean isAvailableForUse() {
        return !isExpired() && currentQuantity > 0 && (status == LotStatus.ACTIVE || status == LotStatus.IN_USE)
                && qcStatus == QCStatus.PASSED;
    }

    /**
     * Check if lot has been opened
     */
    public boolean isOpened() {
        return dateOpened != null;
    }

    // Getters and Setters

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public InventoryStorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(InventoryStorageLocation storageLocation) {
        this.storageLocation = storageLocation;
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

    public Timestamp getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(Timestamp dateOpened) {
        this.dateOpened = dateOpened;
    }

    public Timestamp getCalculatedExpiryAfterOpening() {
        return calculatedExpiryAfterOpening;
    }

    public void setCalculatedExpiryAfterOpening(Timestamp calculatedExpiryAfterOpening) {
        this.calculatedExpiryAfterOpening = calculatedExpiryAfterOpening;
    }

    public Timestamp getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(Timestamp receiptDate) {
        this.receiptDate = receiptDate;
    }

    public Double getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Double initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public Double getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(Double currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public QCStatus getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(QCStatus qcStatus) {
        this.qcStatus = qcStatus;
    }

    public LotStatus getStatus() {
        return status;
    }

    public void setStatus(LotStatus status) {
        this.status = status;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
