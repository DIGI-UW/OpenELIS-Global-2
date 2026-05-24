package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryItemServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItem, Long>
        implements InventoryItemService {

    @Autowired
    private InventoryItemDAO inventoryItemDAO;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    public InventoryItemServiceImpl() {
        super(InventoryItem.class);
        this.auditTrailLog = true; // Enable generic audit trail
    }

    @Override
    protected InventoryItemDAO getBaseObjectDAO() {
        return inventoryItemDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemType> getAllItemTypes() {
        return inventoryItemDAO.getAllItemTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllActive() {
        return inventoryItemDAO.getAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getByItemType(ItemType itemType) {
        return inventoryItemDAO.getByItemType(itemType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getByCategory(String category) {
        return inventoryItemDAO.getByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> searchByName(String searchTerm) {
        return inventoryItemDAO.searchByName(searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems() {
        return inventoryItemDAO.getLowStockItems();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem getByFhirUuid(String fhirUuid) {
        return inventoryItemDAO.getByFhirUuid(fhirUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalCurrentStock(Long itemId) {
        Integer total = inventoryLotDAO.getTotalCurrentQuantity(itemId);
        return total != null ? total.doubleValue() : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInStock(Long itemId) {
        List<org.openelisglobal.inventory.valueholder.InventoryLot> availableLots = inventoryLotDAO
                .getAvailableLotsByItemFEFO(itemId);
        return availableLots != null && !availableLots.isEmpty();
    }

    @Override
    @Transactional
    public Long insert(InventoryItem item) {
        validateItemTypeSpecificFields(item);

        if (item.getFhirUuid() == null) {
            item.setFhirUuid(UUID.randomUUID());
        }

        // Audit logging is automatic via auditTrailLog = true in constructor
        return super.insert(item);
    }

    @Override
    @Transactional
    public InventoryItem update(InventoryItem item) {
        // Validate type-specific required fields
        validateItemTypeSpecificFields(item);

        // Preserve the FHIR UUID from the existing record if not provided in the update
        if (item.getId() != null && item.getFhirUuid() == null) {
            InventoryItem existing = get(item.getId());
            if (existing != null && existing.getFhirUuid() != null) {
                item.setFhirUuid(existing.getFhirUuid());
            }
        }

        // Audit logging is automatic via auditTrailLog = true in constructor
        return super.update(item);
    }

    @Override
    @Transactional
    public void deactivateItem(Long itemId, String sysUserId) {
        InventoryItem item = get(itemId);
        if (item != null) {
            // Detach from session so audit can compare properly
            inventoryItemDAO.evict(item);

            item.setIsActive("N");
            item.setSysUserId(sysUserId);
            item.setLastupdated(new Timestamp(System.currentTimeMillis()));
            // Audit logging is automatic via update() -> auditTrailLog
            update(item);
        }
    }

    @Override
    @Transactional
    public void activateItem(Long itemId, String sysUserId) {
        InventoryItem item = get(itemId);
        if (item != null) {
            // Detach from session so audit can compare properly
            inventoryItemDAO.evict(item);

            item.setIsActive("Y");
            item.setSysUserId(sysUserId);
            item.setLastupdated(new Timestamp(System.currentTimeMillis()));
            update(item);
            // Activation is logged as an UPDATE through the update() method above
        }
    }

    /**
     * Validate that required fields are populated based on item type
     *
     * @param item The inventory item to validate
     * @throws IllegalArgumentException if required fields are missing
     */
    private void validateItemTypeSpecificFields(InventoryItem item) {
        if (item.getItemType() == null) {
            throw new IllegalArgumentException("Item type is required");
        }

        switch (item.getItemType()) {
        case REAGENT:
            validateReagentFields(item);
            break;
        case CARTRIDGE:
            validateCartridgeFields(item);
            break;
        case EQUIPMENT:
            validateEquipmentFields(item);
            break;
        case CONSUMABLE:
            // Common fields only (name, units, department)
            break;
        case RDT:
            validateRDTFields(item);
            break;
        case HIV_KIT:
        case SYPHILIS_KIT:
            validateKitFields(item);
            break;
        case ENZYME:
            validateEnzymeFields(item);
            break;
        case ANTIBIOTICS:
            // Uses catalog name/category; no extra required columns
            break;
        default:
            break;
        }
    }

    /** Validate required fields for REAGENT item type */
    private void validateReagentFields(InventoryItem item) {
        requireStockCategory(item);
    }

    /** Validate required fields for CARTRIDGE (analyzer supply) item type */
    private void validateCartridgeFields(InventoryItem item) {
        rejectEquipmentOnlyFieldsOnCartridge(item);
        requireStockCategory(item);
        validateCalibrationFlag(item);
    }

    private void validateEquipmentFields(InventoryItem item) {
        if (item.getUnits() == null || item.getUnits().trim().isEmpty()) {
            item.setUnits("each");
        }
        validateEquipmentConditionValue(item.getEquipmentCondition());
        validateCalibrationFlag(item);
    }

    private void validateEnzymeFields(InventoryItem item) {
        if (item.getEnzymeType() == null || item.getEnzymeType().trim().isEmpty()) {
            throw new IllegalArgumentException("Enzyme type is required for enzyme catalog items");
        }
    }

    private void requireStockCategory(InventoryItem item) {
        if (!hasText(item.getCategory())) {
            throw new IllegalArgumentException("Category is required for stock inventory items");
        }
    }

    private void rejectEquipmentOnlyFieldsOnCartridge(InventoryItem item) {
        if (hasText(item.getModelNumber()) || hasText(item.getSerialNumber()) || hasText(item.getEquipmentCondition())
                || hasText(item.getAhriTag())) {
            throw new IllegalArgumentException(
                    "Equipment metadata belongs on item type EQUIPMENT, not CARTRIDGE. Use Equipment for instruments.");
        }
    }

    private void validateCalibrationFlag(InventoryItem item) {
        if (item.getCalibrationRequired() != null && !item.getCalibrationRequired().trim().isEmpty()) {
            if (!item.getCalibrationRequired().equals("Y") && !item.getCalibrationRequired().equals("N")) {
                throw new IllegalArgumentException("Calibration required must be 'Y' or 'N'");
            }
        }
    }

    private void validateEquipmentConditionValue(String condition) {
        if (condition == null) {
            return;
        }
        String normalized = condition.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.equals("functional") && !normalized.equals("non-functional")
                && !normalized.equals("under-repair") && !normalized.equals("decommissioned")) {
            throw new IllegalArgumentException(
                    "Equipment condition must be functional, non-functional, under-repair, or decommissioned");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Validate required fields for RDT item type */
    private void validateRDTFields(InventoryItem item) {
        if (item.getTestsPerKit() == null || item.getTestsPerKit() <= 0) {
            throw new IllegalArgumentException("Tests per kit is required for RDTs and must be greater than 0");
        }
        // individualTracking has default value "N", so just validate if it's Y or N
        if (item.getIndividualTracking() != null && !item.getIndividualTracking().trim().isEmpty()) {
            if (!item.getIndividualTracking().equals("Y") && !item.getIndividualTracking().equals("N")) {
                throw new IllegalArgumentException("Individual tracking must be 'Y' or 'N'");
            }
        }
    }

    /** Validate required fields for HIV_KIT and SYPHILIS_KIT item types */
    private void validateKitFields(InventoryItem item) {
        if (item.getSourceOrganization() == null || item.getSourceOrganization().trim().isEmpty()) {
            throw new IllegalArgumentException("Source organization is required for HIV/Syphilis kits");
        }
        if (item.getKitTestType() == null || item.getKitTestType().trim().isEmpty()) {
            throw new IllegalArgumentException("Kit test type is required for HIV/Syphilis kits");
        }
        // Also apply RDT validation since kits are similar to RDTs
        if (item.getTestsPerKit() == null || item.getTestsPerKit() <= 0) {
            throw new IllegalArgumentException(
                    "Tests per kit is required for HIV/Syphilis kits and must be greater than 0");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getPagedItems(int limit, int offset, String sortBy, String sortOrder, ItemType itemType,
            Boolean isActive, String searchTerm) {
        return getPagedItems(limit, offset, sortBy, sortOrder, itemType, isActive, searchTerm, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getPagedItems(int limit, int offset, String sortBy, String sortOrder, ItemType itemType,
            Boolean isActive, String searchTerm, Set<Integer> departmentScopeIds) {
        // Validate and constrain limit to prevent performance issues
        if (limit > 1000) {
            limit = 1000;
        }
        if (limit < 1) {
            limit = 20; // Default page size
        }

        // Ensure offset is non-negative
        if (offset < 0) {
            offset = 0;
        }

        if (departmentScopeIds != null && departmentScopeIds.isEmpty()) {
            return Collections.emptyList();
        }

        return inventoryItemDAO.getPagedItems(limit, offset, sortBy, sortOrder, itemType, isActive, searchTerm,
                departmentScopeIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPagedItemsCount(ItemType itemType, Boolean isActive, String searchTerm) {
        return getPagedItemsCount(itemType, isActive, searchTerm, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPagedItemsCount(ItemType itemType, Boolean isActive, String searchTerm,
            Set<Integer> departmentScopeIds) {
        if (departmentScopeIds != null && departmentScopeIds.isEmpty()) {
            return 0L;
        }
        return inventoryItemDAO.getPagedItemsCount(itemType, isActive, searchTerm, departmentScopeIds);
    }
}
