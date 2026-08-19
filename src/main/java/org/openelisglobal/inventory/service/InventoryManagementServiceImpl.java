package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.util.CodeGenerator;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ReferenceType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryManagementServiceImpl implements InventoryManagementService {

    private static final int LOT_NUMBER_MAX_LENGTH = 100;

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
    public List<ConsumptionRecord> consumeInventoryFEFO(Long itemId, Double quantityNeeded, Long testResultId,
            Long analysisId, String sysUserId) {

        if (quantityNeeded <= 0) {
            throw new IllegalArgumentException("Quantity needed must be greater than 0");
        }

        // Get available lots sorted by FEFO
        List<InventoryLot> availableLots = inventoryLotService.getAvailableLotsByItemFEFO(itemId);

        if (availableLots == null || availableLots.isEmpty()) {
            throw new IllegalStateException(describeWhyNoLotsAvailable(itemId));
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
            String referenceTypeStr = testResultId != null ? ReferenceType.TEST_RESULT.name()
                    : ReferenceType.MANUAL.name();
            String notes = testResultId != null ? "Consumed for test result" : "Manual consumption";
            transactionService.recordTransaction(lot.getId(), TransactionType.CONSUMPTION, -quantityFromThisLot,
                    newQuantity, testResultId, referenceTypeStr, notes, sysUserId);

            // Record usage (always, even if no test result)
            usageService.recordUsage(lot.getId(), itemId, quantityFromThisLot, testResultId, analysisId, sysUserId);

            // Add to consumption records
            consumptionRecords
                    .add(new ConsumptionRecord(lot.getId(), lot.getLotNumber(), quantityFromThisLot, newQuantity));

            remainingToConsume -= quantityFromThisLot;
        }

        return consumptionRecords;
    }

    private String generateLotNumber(InventoryItem item) {
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Timestamp(System.currentTimeMillis()));
        Set<String> existingLotNumbers = inventoryLotService.getByInventoryItemId(item.getId()).stream()
                .map(InventoryLot::getLotNumber).collect(Collectors.toSet());
        return CodeGenerator.generateFromName(item.getCode() + "-" + datePart, LOT_NUMBER_MAX_LENGTH, "LOT",
                existingLotNumbers::contains);
    }

    private String describeWhyNoLotsAvailable(Long itemId) {
        List<InventoryLot> allLots = inventoryLotService.getByInventoryItemId(itemId);
        long pendingQc = allLots.stream().filter(lot -> lot.getQcStatus() == QCStatus.PENDING)
                .filter(lot -> lot.getCurrentQuantity() != null && lot.getCurrentQuantity() > 0).count();
        if (pendingQc > 0) {
            return "No QC-passed lots available for item: " + itemId + " (" + pendingQc
                    + " lot(s) with stock are still awaiting QC, mark QC as passed to use them)";
        }
        return "No available lots for item: " + itemId;
    }

    @Override
    @Transactional
    public InventoryLot receiveInventory(InventoryLot lotData, String sysUserId) {
        if (lotData == null) {
            throw new IllegalArgumentException("Lot data cannot be null");
        }

        if (lotData.getInventoryItem() == null || lotData.getInventoryItem().getId() == null) {
            throw new IllegalArgumentException("Inventory item ID must be specified");
        }

        // Fetch managed InventoryItem entity to avoid transient instance error
        Long itemId = lotData.getInventoryItem().getId();
        InventoryItem managedItem = inventoryItemService.get(itemId);
        if (managedItem == null) {
            throw new IllegalArgumentException("Inventory item not found: " + itemId);
        }
        lotData.setInventoryItem(managedItem);

        if (lotData.getLotNumber() == null || lotData.getLotNumber().trim().isEmpty()) {
            lotData.setLotNumber(generateLotNumber(managedItem));
        }

        // Set initial values
        lotData.setSysUserId(sysUserId);
        lotData.setReceiptDate(new Timestamp(System.currentTimeMillis()));

        // Generate FHIR UUID if not provided
        if (lotData.getFhirUuid() == null) {
            lotData.setFhirUuid(java.util.UUID.randomUUID());
        }

        // Save the lot
        InventoryLot savedLot = inventoryLotService.save(lotData);

        // Record receipt transaction
        transactionService.recordTransaction(savedLot.getId(), TransactionType.RECEIPT, savedLot.getCurrentQuantity(),
                savedLot.getCurrentQuantity(), null, ReferenceType.RECEIPT.name(), "New inventory received", sysUserId);

        return savedLot;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSufficientInventoryAvailable(Long itemId, Double quantityNeeded) {
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
