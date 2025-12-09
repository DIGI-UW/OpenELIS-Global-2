package org.openelisglobal.inventory.service;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.inventory.valueholder.InventoryLot;

public interface InventoryManagementService {

    /**
     * Consume inventory using FEFO (First Expired, First Out) logic Automatically
     * selects lots based on earliest expiration date
     *
     * @param itemId         The inventory item ID
     * @param quantityNeeded The quantity to consume
     * @param testResultId   Optional test result ID for traceability
     * @param analysisId     Optional analysis ID for traceability
     * @param sysUserId      The user performing the action
     * @return List of lots consumed with quantities
     */
    List<ConsumptionRecord> consumeInventoryFEFO(Long itemId, Double quantityNeeded, Long testResultId, Long analysisId,
            String sysUserId);

    /**
     * Receive new inventory (creates new lot and records transaction)
     *
     * @param lotData   The lot data to create
     * @param sysUserId The user performing the action
     * @return The created lot
     */
    InventoryLot receiveInventory(InventoryLot lotData, String sysUserId);

    /**
     * Check if sufficient inventory is available for a given item and quantity
     *
     * @param itemId         The inventory item ID
     * @param quantityNeeded The quantity needed
     * @return true if sufficient inventory is available
     */
    boolean isSufficientInventoryAvailable(Long itemId, Double quantityNeeded);

    /**
     * Get inventory alerts (low stock, expiring soon, expired)
     *
     * @param daysForExpirationWarning Number of days to look ahead for expiring
     *                                 items
     * @return Inventory alerts
     */
    InventoryAlerts getInventoryAlerts(int daysForExpirationWarning);

    @Setter
    @Getter
    class ConsumptionRecord {
        private Long lotId;
        private String lotNumber;
        private Double quantityConsumed;
        private Double remainingQuantity;

        public ConsumptionRecord(Long lotId, String lotNumber, Double quantityConsumed, Double remainingQuantity) {
            this.lotId = lotId;
            this.lotNumber = lotNumber;
            this.quantityConsumed = quantityConsumed;
            this.remainingQuantity = remainingQuantity;
        }

    }

    @Setter
    @Getter
    class InventoryAlerts {
        private List<org.openelisglobal.inventory.valueholder.InventoryItem> lowStockItems;
        private List<InventoryLot> expiringLots;
        private List<InventoryLot> expiredLots;

    }
}
