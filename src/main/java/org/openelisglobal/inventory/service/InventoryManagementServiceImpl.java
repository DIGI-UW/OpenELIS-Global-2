package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.util.CodeGenerator;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ReferenceType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
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
    public InventoryUsage consumeSelectedLot(Long lotId, Double quantityNeeded, Long testResultId, Long analysisId,
            String sysUserId) {
        if (quantityNeeded == null || quantityNeeded <= 0) {
            throw new IllegalArgumentException("Quantity needed must be greater than 0");
        }

        InventoryLot lot = inventoryLotService.getForUpdate(lotId);
        if (lot == null) {
            throw new IllegalArgumentException("Lot not found: " + lotId);
        }
        validateSelectedLot(lot, quantityNeeded);

        double remaining = lot.getCurrentQuantity() - quantityNeeded;
        lot.setCurrentQuantity(remaining);
        if (remaining == 0) {
            lot.setStatus(LotStatus.CONSUMED);
        }
        lot.setSysUserId(sysUserId);
        lot.setLastupdated(new Timestamp(System.currentTimeMillis()));
        inventoryLotService.update(lot);

        String referenceType = testResultId == null ? ReferenceType.MANUAL.name() : ReferenceType.TEST_RESULT.name();
        String notes = analysisId == null ? "Consumed selected inventory lot"
                : "Consumed selected lot for analysis " + analysisId;
        transactionService.recordTransaction(lot.getId(), TransactionType.CONSUMPTION, -quantityNeeded, remaining,
                testResultId, referenceType, notes, sysUserId);
        return usageService.recordUsage(lot, quantityNeeded, testResultId, analysisId, sysUserId);
    }

    private void validateSelectedLot(InventoryLot lot, Double quantityNeeded) {
        String lotNumber = lot.getLotNumber();
        if (lot.isExpired()) {
            throw new InventoryLotUnavailableException("INVENTORY_LOT_EXPIRED", lotNumber);
        }
        if (lot.getQcStatus() != org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus.PASSED) {
            String code = lot.getQcStatus() == org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus.FAILED
                    ? "INVENTORY_LOT_QC_FAILED"
                    : "INVENTORY_LOT_QC_NOT_PASSED";
            throw new InventoryLotUnavailableException(code, lotNumber);
        }
        if (lot.getStatus() != LotStatus.ACTIVE && lot.getStatus() != LotStatus.IN_USE) {
            String status = lot.getStatus() == null ? "STATUS_UNKNOWN" : lot.getStatus().name();
            throw new InventoryLotUnavailableException("INVENTORY_LOT_" + status, lotNumber);
        }
        if (lot.getCurrentQuantity() == null || lot.getCurrentQuantity() < quantityNeeded) {
            throw new InventoryLotUnavailableException("INVENTORY_LOT_INSUFFICIENT_QUANTITY", lotNumber);
        }
    }

    @Override
    @Transactional
    public List<ConsumptionRecord> consumeInventoryFEFO(Long itemId, Double quantityNeeded, Long testResultId,
            Long analysisId, String sysUserId) {

        if (quantityNeeded <= 0) {
            throw new IllegalArgumentException("Quantity needed must be greater than 0");
        }

        // The FEFO query has no expiry predicate; isAvailableForUse is the rule
        // check-availability answers with, so it decides here too.
        List<InventoryLot> availableLots = inventoryLotService.getAvailableLotsByItemFEFO(itemId).stream()
                .filter(InventoryLot::isAvailableForUse).collect(Collectors.toList());

        if (availableLots == null || availableLots.isEmpty()) {
            throw noAvailableLots(itemId);
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

    /**
     * Names the item and the kind of unexpired stock standing in the way, so the
     * user knows whether to pass QC, release a quarantine, or reorder.
     */
    private LocalizedValidationException noAvailableLots(Long itemId) {
        InventoryItem item;
        try {
            item = inventoryItemService.get(itemId);
        } catch (ObjectNotFoundException e) {
            throw new IllegalArgumentException("Inventory item not found: " + itemId);
        }
        List<InventoryLot> stocked = inventoryLotService.getByInventoryItemId(itemId).stream()
                .filter(lot -> !lot.isExpired() && lot.getCurrentQuantity() != null && lot.getCurrentQuantity() > 0)
                .collect(Collectors.toList());
        String label = item.getName() + " (" + item.getCode() + ")";

        long quarantined = count(stocked,
                lot -> lot.getStatus() == LotStatus.QUARANTINED || lot.getQcStatus() == QCStatus.QUARANTINED);
        if (quarantined > 0) {
            return refusal("inventory.consume.error.noLotsQuarantined",
                    "No usable stock for " + label + ": " + quarantined + " lot(s) with stock are quarantined", item,
                    quarantined);
        }
        long awaitingQc = count(stocked, lot -> lot.getQcStatus() == QCStatus.PENDING);
        if (awaitingQc > 0) {
            return refusal("inventory.consume.error.noLotsAwaitingQc", "No QC-passed stock for " + label + ": "
                    + awaitingQc + " lot(s) with stock are awaiting QC; mark QC as passed to use them", item,
                    awaitingQc);
        }
        long failedQc = count(stocked, lot -> lot.getQcStatus() == QCStatus.FAILED);
        if (failedQc > 0) {
            return refusal("inventory.consume.error.noLotsQcFailed",
                    "No usable stock for " + label + ": " + failedQc + " lot(s) with stock failed QC", item, failedQc);
        }
        return refusal("inventory.consume.error.noLots", "No stock available for " + label, item, 0);
    }

    private long count(List<InventoryLot> lots, Predicate<InventoryLot> predicate) {
        return lots.stream().filter(predicate).count();
    }

    private LocalizedValidationException refusal(String errorCode, String message, InventoryItem item, long count) {
        return new LocalizedValidationException(errorCode, message,
                Map.of("code", item.getCode(), "name", item.getName(), "count", Long.toString(count)));
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

    /**
     * Counts a lot only if {@link InventoryLot#isAvailableForUse()}, the per-lot
     * rule consumption enforces, so a "yes" here cannot turn into a 409 there.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isSufficientInventoryAvailable(Long itemId, Double quantityNeeded) {
        if (quantityNeeded <= 0) {
            return true;
        }

        double available = inventoryLotService.getByInventoryItemId(itemId).stream()
                .filter(InventoryLot::isAvailableForUse).mapToDouble(InventoryLot::getCurrentQuantity).sum();
        return available >= quantityNeeded;
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
