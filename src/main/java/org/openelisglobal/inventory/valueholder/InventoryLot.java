package org.openelisglobal.inventory.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_lot")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryLot extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_lot_generator")
    @SequenceGenerator(name = "inventory_lot_generator", sequenceName = "inventory_lot_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @NotNull
    private InventoryItem inventoryItem;

    // Unified storage hierarchy fields (same as SampleStorageAssignment)
    // Supports assignment at any level: room, device, shelf, rack, box
    @Column(name = "location_id")
    private Integer locationId;

    @Column(name = "location_type", length = 20)
    private String locationType;

    @Column(name = "position_coordinate", length = 50)
    private String positionCoordinate;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

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

    @Column(name = "unit_size", length = 100)
    private String unitSize;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

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
}
