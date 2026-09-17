package org.openelisglobal.inventory.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;

@Getter
@Setter
@Entity
@Table(name = "inventory_item")
@Access(AccessType.FIELD)
public class InventoryItem extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_item_generator")
    @SequenceGenerator(name = "inventory_item_generator", sequenceName = "inventory_item_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    @Size(max = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    @NotNull
    @Size(min = 1, max = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "item_type", nullable = false, length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "catalog_number", length = 100)
    private String catalogNumber;

    @Column(name = "storage_requirements", length = 255)
    private String storageRequirements;

    @Column(name = "quantity_per_unit")
    private Integer quantityPerUnit;

    @Column(name = "units", nullable = false, length = 50)
    @NotNull
    @Size(min = 1, max = 50)
    private String units;

    @Column(name = "low_stock_threshold")
    @Min(0)
    private Integer lowStockThreshold;

    /**
     * Days between placing an order for this item and it arriving. Local to this
     * lab: the same product takes different times to reach different sites, so it
     * is never seeded from a shared catalog. Null means none has been entered and
     * the board falls back to a marked placeholder.
     */
    @Column(name = "lead_time_days")
    @Min(value = 0, message = "Lead time cannot be negative")
    private Integer leadTimeDays;

    /**
     * When someone recorded that this item had been ordered. Null means it has not
     * been, or that the mark was cleared: this is an acknowledgement that an order
     * was placed, not the order itself, so it is reversible in both directions.
     *
     * <p>
     * It is also the anchor for learning the real lead time later, from the gap
     * between this stamp and the next receipt of the item, rather than leaving
     * {@code leadTimeDays} as a number somebody had to guess.
     */
    @Column(name = "ordered_at")
    private Timestamp orderedAt;

    /** Free text kept with the mark, e.g. a requisition reference. */
    @Column(name = "order_note")
    private String orderNote;

    /** When the lab expects the order to arrive, if they know. Advisory only. */
    @Column(name = "order_expected_date")
    private LocalDate orderExpectedDate;

    @Column(name = "expiration_alert_days")
    @Min(1)
    private Integer expirationAlertDays;

    // REAGENT-specific fields
    @Column(name = "stability_after_opening")
    @Min(1)
    private Integer stabilityAfterOpening;

    @Column(name = "dilution_notes", columnDefinition = "TEXT")
    @Size(max = 2000)
    private String dilutionNotes;

    // CARTRIDGE-specific fields
    @Column(name = "compatible_analyzers", length = 500)
    @Size(max = 500)
    private String compatibleAnalyzers;

    @Column(name = "calibration_required", length = 1)
    private String calibrationRequired = "N";

    // RDT-specific fields
    @Column(name = "tests_per_kit")
    @Min(1)
    private Integer testsPerKit;

    @Column(name = "individual_tracking", length = 1)
    private String individualTracking = "N";

    // HIV_KIT/SYPHILIS_KIT-specific fields
    @Column(name = "source_organization", length = 255)
    private String sourceOrganization;

    @Column(name = "kit_test_type", length = 50)
    private String kitTestType; // HIV, SYPHILIS, etc.

    @Column(name = "is_active", length = 1, nullable = false)
    private String isActive = "Y";

    // Business logic helper methods
    @JsonIgnore
    public boolean isReagent() {
        return itemType == ItemType.REAGENT;
    }

    @JsonIgnore
    public boolean isCartridge() {
        return itemType == ItemType.CARTRIDGE;
    }

    @JsonIgnore
    public boolean isRDT() {
        return itemType == ItemType.RDT;
    }

    @JsonIgnore
    public boolean isHIVKit() {
        return itemType == ItemType.HIV_KIT;
    }

    @JsonIgnore
    public boolean isSyphilisKit() {
        return itemType == ItemType.SYPHILIS_KIT;
    }

    @JsonIgnore
    public boolean isActive() {
        return "Y".equals(isActive);
    }
}
