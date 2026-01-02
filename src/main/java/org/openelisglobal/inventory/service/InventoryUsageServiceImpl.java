package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.dao.InventoryUsageDAO;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.test.dao.TestSectionDAO;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryUsageServiceImpl extends AuditableBaseObjectServiceImpl<InventoryUsage, Long>
        implements InventoryUsageService {

    @Autowired
    private InventoryUsageDAO inventoryUsageDAO;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryItemDAO inventoryItemDAO;

    @Autowired
    private TestSectionDAO testSectionDAO;

    public InventoryUsageServiceImpl() {
        super(InventoryUsage.class);
        this.auditTrailLog = true; // Enable generic audit trail
    }

    @Override
    protected InventoryUsageDAO getBaseObjectDAO() {
        return inventoryUsageDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByTestResultId(Long testResultId) {
        return inventoryUsageDAO.getByTestResultId(testResultId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByLotId(Long lotId) {
        return inventoryUsageDAO.getByLotId(lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByInventoryItemId(Long itemId) {
        return inventoryUsageDAO.getByInventoryItemId(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByAnalysisId(Long analysisId) {
        return inventoryUsageDAO.getByAnalysisId(analysisId);
    }

    @Override
    @Transactional
    public InventoryUsage recordUsage(Long lotId, Long itemId, Double quantityUsed, Long testResultId, Long analysisId,
            String sysUserId) {
        // Default behavior: deduct quantity from lot
        return recordUsage(lotId, itemId, quantityUsed, testResultId, analysisId, sysUserId, true);
    }

    @Override
    public InventoryUsage recordUsage(Long lotId, Long itemId, Double quantityUsed, Long testResultId, Long analysisId,
            String sysUserId, boolean deductQuantity) {

        InventoryLot lot = inventoryLotDAO.get(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lotId));

        InventoryItem item = inventoryItemDAO.get(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + itemId));

        if (quantityUsed == null || quantityUsed <= 0) {
            throw new IllegalArgumentException("Quantity used must be greater than 0");
        }

        // Check if lot has sufficient quantity (only if we're deducting)
        Double currentQuantity = lot.getCurrentQuantity();
        if (deductQuantity && (currentQuantity == null || currentQuantity < quantityUsed)) {
            throw new IllegalArgumentException(
                    "Insufficient quantity in lot. Available: " + currentQuantity + ", Requested: " + quantityUsed);
        }

        // Create usage record
        InventoryUsage usage = new InventoryUsage();
        usage.setLot(lot);
        usage.setInventoryItem(item);
        usage.setQuantityUsed(quantityUsed);
        usage.setTestResultId(testResultId);
        usage.setAnalysisId(analysisId);
        usage.setUsageDate(new Timestamp(System.currentTimeMillis()));
        usage.setSysUserId(sysUserId);
        usage.setPerformedByUser(Integer.valueOf(sysUserId));

        Long id = insert(usage);

        // Only deduct quantity if requested (avoid double deduction when called from
        // consumeInventoryFEFO)
        if (deductQuantity) {
            // Detach from session so audit can compare properly
            inventoryLotDAO.evict(lot);

            // Deduct quantity from lot
            Double newQuantity = currentQuantity - quantityUsed;
            lot.setCurrentQuantity(newQuantity);
            lot.setSysUserId(sysUserId);

            // Update lot status to CONSUMED if quantity reaches zero
            if (newQuantity <= 0) {
                lot.setStatus(org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus.CONSUMED);
            }

            // Use service to ensure audit trail is captured
            inventoryLotService.update(lot);
        }

        // Audit logging is automatic via auditTrailLog = true in constructor
        return get(id);
    }

    @Override
    @Transactional
    public InventoryUsage recordEquipmentUsage(Long lotId, Long itemId, Double quantityUsed, String sysUserId,
            String labUnitId) {

        InventoryLot lot = inventoryLotDAO.get(lotId)
                .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + lotId));

        InventoryItem item = inventoryItemDAO.get(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found: " + itemId));

        if (quantityUsed == null || quantityUsed <= 0) {
            throw new IllegalArgumentException("Quantity used must be greater than 0");
        }

        // Create usage record without deducting quantity
        InventoryUsage usage = new InventoryUsage();
        usage.setLot(lot);
        usage.setInventoryItem(item);
        usage.setQuantityUsed(quantityUsed);
        usage.setUsageDate(new Timestamp(System.currentTimeMillis()));
        usage.setSysUserId(sysUserId);
        usage.setPerformedByUser(Integer.valueOf(sysUserId));

        // Set lab unit if provided
        if (labUnitId != null && !labUnitId.isEmpty()) {
            TestSection labUnit = testSectionDAO.get(labUnitId).orElse(null);
            usage.setLabUnit(labUnit);
        }

        Long id = insert(usage);

        // Do NOT deduct quantity - equipment usage only tracks usage, not consumption
        // Audit logging is automatic via auditTrailLog = true in constructor
        return get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByLabUnitId(String labUnitId) {
        return inventoryUsageDAO.getByLabUnitId(labUnitId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByDateRange(Timestamp startDate, Timestamp endDate) {
        return inventoryUsageDAO.getByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> getByItemIdAndDateRange(Long itemId, Timestamp startDate, Timestamp endDate) {
        return inventoryUsageDAO.getByItemIdAndDateRange(itemId, startDate, endDate);
    }
}
