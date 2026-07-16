package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.CodeGenerator;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryItemServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItem, String>
        implements InventoryItemService {

    // inventory_item.code is VARCHAR(64) — see 071-inventory-item-code-pk.xml
    private static final int CODE_MAX_LENGTH = 64;

    @Autowired
    private InventoryItemDAO inventoryItemDAO;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    public InventoryItemServiceImpl() {
        super(InventoryItem.class);
    }

    @Override
    protected InventoryItemDAO getBaseObjectDAO() {
        return inventoryItemDAO;
    }

    /**
     * OGC-658 Part C: inventory_item's PK is a server-generated code, not an
     * auto-increment surrogate. Generate (or normalize/validate an explicit) code
     * here, immediately before delegating to the inherited insert() — this keeps
     * AuditableBaseObjectServiceImpl.save()'s insert-vs-update null-check working
     * unmodified, since the id stays null until this method runs.
     */
    @Override
    @Transactional
    public String insert(InventoryItem item) {
        String explicitCode = item.getId();
        String finalCode = (explicitCode == null || explicitCode.trim().isEmpty())
                ? CodeGenerator.generateFromName(item.getName(), CODE_MAX_LENGTH, "ITEM", this::codeExists)
                : CodeGenerator.normalize(explicitCode, CODE_MAX_LENGTH);
        if (codeExists(finalCode)) {
            throw new LocalizedValidationException("inventory.item.error.duplicateCode",
                    "Inventory item code already exists: " + finalCode, Map.of("code", finalCode));
        }
        item.setId(finalCode);
        return super.insert(item);
    }

    private boolean codeExists(String code) {
        return !getAllMatching("id", code).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllActive() {
        return inventoryItemDAO.getAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getByItemType(String itemType) {
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

    /**
     * "Low stock" is judged against usable quantity
     * ({@link InventoryLot#isAvailableForUse()} — excludes
     * EXPIRED/DISPOSED/QUARANTINED lots and anything that failed QC), not the raw
     * sum of every lot. {@code InventoryItemDAO.getLowStockItems()}'s native-SQL
     * query sums every lot's current_quantity regardless of status, so an item
     * sitting on a pile of expired/disposed stock would never trip the alert
     * despite having nothing actually usable — backwards for an alert meant to
     * answer "what do we need to reorder." Computed here in Java (same approach as
     * {@code InventoryReportServiceImpl.buildLowStockReport()}, which had this
     * exact fix already) rather than in the DAO, since replicating
     * {@code isAvailableForUse()}'s expiration/status/QC logic correctly in SQL is
     * more error-prone than reusing the one Java implementation.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems() {
        return inventoryItemDAO.getAllActive().stream().filter(item -> item.getLowStockThreshold() != null)
                .filter(item -> availableQuantity(item.getId()) <= item.getLowStockThreshold())
                .collect(Collectors.toList());
    }

    private double availableQuantity(String itemId) {
        return inventoryLotDAO.getByInventoryItemId(itemId).stream().filter(InventoryLot::isAvailableForUse)
                .mapToDouble(lot -> lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0).sum();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem getByFhirUuid(String fhirUuid) {
        return inventoryItemDAO.getByFhirUuid(fhirUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalCurrentStock(String itemId) {
        Integer total = inventoryLotDAO.getTotalCurrentQuantity(itemId);
        return total != null ? total.doubleValue() : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInStock(String itemId) {
        List<org.openelisglobal.inventory.valueholder.InventoryLot> availableLots = inventoryLotDAO
                .getAvailableLotsByItemFEFO(itemId);
        return availableLots != null && !availableLots.isEmpty();
    }

    @Override
    @Transactional
    public void deactivateItem(String itemId, String sysUserId) {
        InventoryItem item = get(itemId);
        if (item != null) {
            item.setIsActive("N");
            item.setSysUserId(sysUserId);
            item.setLastupdated(new Timestamp(System.currentTimeMillis()));
            update(item);
        }
    }

    @Override
    @Transactional
    public void activateItem(String itemId, String sysUserId) {
        InventoryItem item = get(itemId);
        if (item != null) {
            item.setIsActive("Y");
            item.setSysUserId(sysUserId);
            item.setLastupdated(new Timestamp(System.currentTimeMillis()));
            update(item);
        }
    }
}
