package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ReferenceType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryManagementServiceImpl implements InventoryManagementService {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryTransactionService transactionService;

    @Autowired
    private InventoryUsageService usageService;

    @Override
    @Transactional
    public List<ConsumptionRecord> consumeInventoryFEFO(String itemId, Double quantityNeeded, String testResultId,
            String analysisId, String sysUserId) {

        if (quantityNeeded <= 0) {
            throw new IllegalArgumentException("Quantity needed must be greater than 0");
        }

        // Get available lots sorted by FEFO
        List<InventoryLot> availableLots = inventoryLotService.getAvailableLotsByItemFEFO(itemId);

        if (availableLots == null || availableLots.isEmpty()) {
            throw new IllegalStateException("No available lots for item: " + itemId);
        }

        // Check if sufficient inventory is available
        Double totalAvailable = 0.0;
        for (InventoryLot lot : availableLots) {
            totalAvailable += lot.getCurrentQuantity();
        }

        if (totalAvailable < quantityNeeded) {
            throw new IllegalStateException(String.format("Insufficient inventory. Needed: %.2f, Available: %.2f",
                    quantityNeeded, totalAvailable));
        }

        // Consume from lots using FEFO
        List<ConsumptionRecord> consumptionRecords = new ArrayList<>();
        Double remainingToConsume = quantityNeeded;

        for (InventoryLot lot : availableLots) {
            if (remainingToConsume <= 0) {
                break;
            }

            Double lotQuantity = lot.getCurrentQuantity();
            Double quantityFromThisLot = Math.min(lotQuantity, remainingToConsume);

            // Update lot quantity
            Double newQuantity = lotQuantity - quantityFromThisLot;
            lot.setCurrentQuantity(newQuantity);
            lot.setSysUserId(sysUserId);
            lot.setLastupdated(new Timestamp(System.currentTimeMillis()));

            // Update status if consumed
            if (newQuantity == 0) {
                lot.setStatus(LotStatus.CONSUMED);
            }

            inventoryLotService.update(lot);

            // Record transaction
            transactionService.recordTransaction(lot.getId(), TransactionType.CONSUMPTION, -quantityFromThisLot,
                    newQuantity, testResultId, ReferenceType.TEST_RESULT.name(), "Consumed for test result", sysUserId);

            // Record usage if test result provided
            if (testResultId != null) {
                usageService.recordUsage(lot.getId(), itemId, quantityFromThisLot, testResultId, analysisId, sysUserId);
            }

            // Add to consumption records
            consumptionRecords
                    .add(new ConsumptionRecord(lot.getId(), lot.getLotNumber(), quantityFromThisLot, newQuantity));

            remainingToConsume -= quantityFromThisLot;
        }

        return consumptionRecords;
    }

    @Override
    @Transactional
    public InventoryLot receiveInventory(InventoryLot lotData, String sysUserId) {
        if (lotData == null) {
            throw new IllegalArgumentException("Lot data cannot be null");
        }

        if (lotData.getInventoryItem() == null) {
            throw new IllegalArgumentException("Inventory item must be specified");
        }

        // Set initial values
        lotData.setSysUserId(sysUserId);
        lotData.setReceiptDate(new Timestamp(System.currentTimeMillis()));

        // Save the lot
        InventoryLot savedLot = inventoryLotService.save(lotData);

        // Record receipt transaction
        transactionService.recordTransaction(savedLot.getId(), TransactionType.RECEIPT, savedLot.getCurrentQuantity(),
                savedLot.getCurrentQuantity(), null, ReferenceType.RECEIPT.name(), "New inventory received", sysUserId);

        return savedLot;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSufficientInventoryAvailable(String itemId, Double quantityNeeded) {
        if (quantityNeeded <= 0) {
            return true;
        }

        Double totalAvailable = inventoryLotService.getTotalCurrentQuantity(itemId);
        return totalAvailable != null && totalAvailable >= quantityNeeded;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAlerts getInventoryAlerts(int daysForExpirationWarning) {
        InventoryAlerts alerts = new InventoryAlerts();

        // Get low stock items
        List<InventoryItem> lowStockItems = inventoryItemService.getLowStockItems();
        alerts.setLowStockItems(lowStockItems);

        // Get expiring lots
        List<InventoryLot> expiringLots = inventoryLotService.getExpiringLots(daysForExpirationWarning);
        alerts.setExpiringLots(expiringLots);

        // Get expired lots
        List<InventoryLot> expiredLots = inventoryLotService.getExpiredActiveLots();
        alerts.setExpiredLots(expiredLots);

        return alerts;
    }
}
