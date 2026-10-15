package org.openelisglobal.inventory.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
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

    /** Superseded by tags; still NOT NULL, so insert fills it in. */
    @Column(name = "item_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    /** Eager: items go straight to the browser from several endpoints. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "inventory_item_tag", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "tag", nullable = false, length = 255)
    @BatchSize(size = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "catalog_number", length = 100)
    private String catalogNumber;

    @Column(name = "upc", length = 64)
    @Size(max = 64)
    private String upc;

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

    /** Order-to-arrival days, set per lab, never shared; null when unset. */
    @Column(name = "lead_time_days")
    @Min(value = 0, message = "Lead time cannot be negative")
    private Integer leadTimeDays;

    @Column(name = "ordered_at")
    private Timestamp orderedAt;

    @Column(name = "order_note")
    private String orderNote;

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

    @Column(name = "track_lots", length = 1)
    private String trackLots = "N";

    @JsonIgnore
    public boolean tracksLots() {
        return "Y".equals(trackLots);
    }

    // HIV_KIT/SYPHILIS_KIT-specific fields
    @Column(name = "source_organization", length = 255)
    private String sourceOrganization;

    @Column(name = "kit_test_type", length = 50)
    private String kitTestType; // HIV, SYPHILIS, etc.

    @Column(name = "last_counted_at")
    private Timestamp lastCountedAt;

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
