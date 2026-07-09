package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.CodeGenerator;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryItem;
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
            throw new LIMSRuntimeException("Inventory item code already exists: " + finalCode);
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
