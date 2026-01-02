/**
 * EquipmentUsageService.js
 *
 * Wrapper facade around existing Inventory APIs for cartridge (equipment) usage tracking.
 * This service provides a simplified interface to the inventory system for tracking
 * consumable cartridge usage in the MNTD laboratory context.
 *
 * It leverages:
 * - InventoryItemAPI: Get cartridges from inventory
 * - InventoryLotAPI: Get available lots with FEFO ordering
 * - InventoryManagementAPI: Record consumption via FEFO logic
 * - InventoryUsageAPI: Query usage history
 * - InventoryTransactionAPI: Get audit trail
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../utils/Utils";

/**
 * CartridgeUsageAPI - Simplified interface for equipment usage tracking
 * All methods use existing inventory endpoints, no new backend code required
 */
export const CartridgeUsageAPI = {
  /**
   * Get all active cartridge items from inventory
   * Filters InventoryItems to only CARTRIDGE type
   */
  getCartridges: (callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      "/rest/inventory/items/type/CARTRIDGE",
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get all cartridges (both active and inactive)
   * Useful for admin views or full inventory status
   */
  getAllCartridges: (callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      "/rest/inventory/items/all?itemType=CARTRIDGE",
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get a single cartridge item by ID
   * @param {number} itemId - The inventory item ID
   */
  getCartridgeById: (itemId, callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      `/rest/inventory/items/${itemId}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get total stock level for a cartridge item
   * @param {number} itemId - The inventory item ID
   */
  getStockLevel: (itemId, callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      `/rest/inventory/items/${itemId}/stock`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get available lots for a cartridge in FEFO order (First Expired First Out)
   * Automatically filters for: ACTIVE/IN_USE status, PASSED QC, quantity > 0
   * Sorts by earliest expiration date first
   * @param {number} itemId - The cartridge item ID
   */
  getAvailableLots: (itemId, callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/item/${itemId}/available`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get all lots for a cartridge (regardless of status/availability)
   * @param {number} itemId - The cartridge item ID
   */
  getAllLots: (itemId, callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/item/${itemId}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get total quantity available for a cartridge item
   * @param {number} itemId - The inventory item ID
   */
  getTotalQuantity: (itemId, callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/item/${itemId}/total-quantity`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Record cartridge consumption (FEFO logic handled by backend)
   * Automatically selects lots with earliest expiration date first
   * Creates InventoryUsage and InventoryTransaction records
   * @param {object} consumeRequest - {
   *   itemId: number,
   *   quantity: number,
   *   testResultId?: string,
   *   analysisId?: string
   * }
   */
  recordUsage: (consumeRequest, callback, errorCallback = null) => {
    const payload = JSON.stringify(consumeRequest);
    postToOpenElisServerFullResponse(
      "/rest/inventory/management/consume",
      payload,
      callback,
      null
    );
  },

  /**
   * Get usage history for a specific cartridge item
   * @param {number} itemId - The cartridge item ID
   */
  getUsageHistory: (itemId, callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      `/rest/inventory/usage/item/${itemId}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get usage records linked to a specific test result
   * @param {string|number} testResultId - The test result ID
   */
  getUsageByTestResult: (
    testResultId,
    callback,
    errorCallback = null,
    signal = null
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/usage/test-result/${testResultId}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get usage records linked to a specific analysis
   * @param {string|number} analysisId - The analysis ID
   */
  getUsageByAnalysis: (
    analysisId,
    callback,
    errorCallback = null,
    signal = null
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/usage/analysis/${analysisId}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get transaction history for a specific lot
   * Provides complete audit trail of lot modifications
   * @param {number} lotId - The inventory lot ID
   */
  getLotTransactionHistory: (
    lotId,
    callback,
    errorCallback = null,
    signal = null
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/transactions/lot/${lotId}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Check if sufficient quantity is available
   * @param {number} itemId - The cartridge item ID
   * @param {number} quantity - Requested quantity
   */
  checkAvailability: (
    itemId,
    quantity,
    callback,
    errorCallback = null,
    signal = null
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/management/check-availability?itemId=${itemId}&quantity=${quantity}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get inventory alerts (low stock, expiring, expired)
   * @param {number} expirationWarningDays - Days before expiration to warn (optional, default 30)
   */
  getAlerts: (
    callback,
    errorCallback = null,
    signal = null,
    expirationWarningDays = 30
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/management/alerts?expirationWarningDays=${expirationWarningDays}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get low stock items (cartridges only)
   * Useful for inventory monitoring dashboard
   */
  getLowStockItems: (callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      "/rest/inventory/items/low-stock",
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get lots expiring within a specified timeframe
   * @param {number} daysBeforeExpiration - Number of days from today (default 30)
   */
  getExpiringLots: (
    callback,
    errorCallback = null,
    signal = null,
    daysBeforeExpiration = 30
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/expiring?days=${daysBeforeExpiration}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get expired lots that are still active
   * Should be disposed/quarantined
   */
  getExpiredLots: (callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      "/rest/inventory/lots/expired",
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get a specific lot by ID
   * @param {number} lotId - The inventory lot ID
   */
  getLotById: (lotId, callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/${lotId}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get a lot by lot number
   * @param {string} lotNumber - The lot number/identifier
   */
  getLotByNumber: (lotNumber, callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/lot-number/${lotNumber}`,
      callback,
      errorCallback,
      signal
    );
  },

  /**
   * Get all items of CARTRIDGE type with available lots (enhanced DTO)
   * Similar to getCartridges but with aggregated lot data
   */
  getCartridgesWithLots: (callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      "/rest/inventory/instruments",
      callback,
      errorCallback,
      signal
    );
  },
};

export default CartridgeUsageAPI;
