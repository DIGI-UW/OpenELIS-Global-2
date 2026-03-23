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
