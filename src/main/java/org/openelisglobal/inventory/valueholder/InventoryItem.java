package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;

@Entity
@Table(name = "inventory_item")
@Access(AccessType.FIELD)
public class InventoryItem extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

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
    public boolean isReagent() {
        return itemType == ItemType.REAGENT;
    }

    public boolean isCartridge() {
        return itemType == ItemType.CARTRIDGE;
    }

    public boolean isRDT() {
        return itemType == ItemType.RDT;
    }

    public boolean isHIVKit() {
        return itemType == ItemType.HIV_KIT;
    }

    public boolean isSyphilisKit() {
        return itemType == ItemType.SYPHILIS_KIT;
    }

    public boolean isActive() {
        return "Y".equals(isActive);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getCatalogNumber() {
        return catalogNumber;
    }

    public void setCatalogNumber(String catalogNumber) {
        this.catalogNumber = catalogNumber;
    }

    public String getStorageRequirements() {
        return storageRequirements;
    }

    public void setStorageRequirements(String storageRequirements) {
        this.storageRequirements = storageRequirements;
    }

    public Integer getQuantityPerUnit() {
        return quantityPerUnit;
    }

    public void setQuantityPerUnit(Integer quantityPerUnit) {
        this.quantityPerUnit = quantityPerUnit;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Integer getExpirationAlertDays() {
        return expirationAlertDays;
    }

    public void setExpirationAlertDays(Integer expirationAlertDays) {
        this.expirationAlertDays = expirationAlertDays;
    }

    public Integer getStabilityAfterOpening() {
        return stabilityAfterOpening;
    }

    public void setStabilityAfterOpening(Integer stabilityAfterOpening) {
        this.stabilityAfterOpening = stabilityAfterOpening;
    }

    public String getDilutionNotes() {
        return dilutionNotes;
    }

    public void setDilutionNotes(String dilutionNotes) {
        this.dilutionNotes = dilutionNotes;
    }

    public String getCompatibleAnalyzers() {
        return compatibleAnalyzers;
    }

    public void setCompatibleAnalyzers(String compatibleAnalyzers) {
        this.compatibleAnalyzers = compatibleAnalyzers;
    }

    public String getCalibrationRequired() {
        return calibrationRequired;
    }

    public void setCalibrationRequired(String calibrationRequired) {
        this.calibrationRequired = calibrationRequired;
    }

    public Integer getTestsPerKit() {
        return testsPerKit;
    }

    public void setTestsPerKit(Integer testsPerKit) {
        this.testsPerKit = testsPerKit;
    }

    public String getIndividualTracking() {
        return individualTracking;
    }

    public void setIndividualTracking(String individualTracking) {
        this.individualTracking = individualTracking;
    }

    public String getSourceOrganization() {
        return sourceOrganization;
    }

    public void setSourceOrganization(String sourceOrganization) {
        this.sourceOrganization = sourceOrganization;
    }

    public String getKitTestType() {
        return kitTestType;
    }

    public void setKitTestType(String kitTestType) {
        this.kitTestType = kitTestType;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
