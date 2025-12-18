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
      signal,
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
      signal,
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
      signal,
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
      signal,
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
      signal,
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
      signal,
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
      signal,
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
      null,
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
      signal,
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
    signal = null,
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/usage/test-result/${testResultId}`,
      callback,
      errorCallback,
      signal,
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
    signal = null,
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/usage/analysis/${analysisId}`,
      callback,
      errorCallback,
      signal,
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
    signal = null,
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/transactions/lot/${lotId}`,
      callback,
      errorCallback,
      signal,
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
    signal = null,
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/management/check-availability?itemId=${itemId}&quantity=${quantity}`,
      callback,
      errorCallback,
      signal,
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
    expirationWarningDays = 30,
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/management/alerts?expirationWarningDays=${expirationWarningDays}`,
      callback,
      errorCallback,
      signal,
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
      signal,
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
    daysBeforeExpiration = 30,
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/expiring?days=${daysBeforeExpiration}`,
      callback,
      errorCallback,
      signal,
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
      signal,
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
      signal,
    );
  },

  /**
   * Get a lot by lot number
   * @param {string} lotNumber - The lot number/identifier
   */
  getLotByNumber: (
    lotNumber,
    callback,
    errorCallback = null,
    signal = null,
  ) => {
    getFromOpenElisServer(
      `/rest/inventory/lots/lot-number/${lotNumber}`,
      callback,
      errorCallback,
      signal,
    );
  },

  /**
   * Get all items of CARTRIDGE type with available lots (enhanced DTO)
   * Similar to getCartridges but with aggregated lot data
   * Uses reagent controller endpoint that aggregates cartridge items with lot information
   */
  getCartridgesWithLots: (callback, errorCallback = null, signal = null) => {
    getFromOpenElisServer(
      "/rest/inventory/reagents/instruments",
      callback,
      errorCallback,
      signal,
    );
  },

  /**
   * Record equipment usage WITHOUT reducing inventory quantities
   * This is for equipment usage tracking only, not consumption
   * @param {object} usageRequest - {
   *   itemId: number,
   *   lotId: number,
   *   quantity: number,
   *   labUnitId?: string (optional, will use session lab unit if not provided)
   * }
   */
  recordEquipmentUsage: (usageRequest, callback, errorCallback = null) => {
    const payload = JSON.stringify(usageRequest);
    postToOpenElisServerFullResponse(
      "/rest/equipment/usage/record",
      payload,
      callback,
      null,
    );
  },

  /**
   * Get equipment usage history for a specific item with optional date range
   * @param {number} itemId - The equipment item ID
   * @param {string} startDate - Optional start date (ISO 8601 format)
   * @param {string} endDate - Optional end date (ISO 8601 format)
   */
  getEquipmentUsageHistory: (
    itemId,
    startDate = null,
    endDate = null,
    callback,
    errorCallback = null,
    signal = null,
  ) => {
    let url = `/rest/equipment/usage/item/${itemId}`;
    if (startDate && endDate) {
      url += `?startDate=${startDate}&endDate=${endDate}`;
    }
    getFromOpenElisServer(url, callback, errorCallback, signal);
  },

  /**
   * Get aggregated equipment usage metrics
   * Includes total counts, usage by equipment, and usage by lab unit
   * @param {string} startDate - Optional start date (ISO 8601 format)
   * @param {string} endDate - Optional end date (ISO 8601 format)
   */
  getEquipmentUsageMetrics: (
    startDate = null,
    endDate = null,
    callback,
    errorCallback = null,
    signal = null,
  ) => {
    let url = "/rest/equipment/usage/metrics";
    if (startDate && endDate) {
      url += `?startDate=${startDate}&endDate=${endDate}`;
    }
    getFromOpenElisServer(url, callback, errorCallback, signal);
  },

  /**
   * Get usage records for a specific lab/department
   * @param {string} labUnitId - The lab unit ID
   */
  getUsageByLabUnit: (
    labUnitId,
    callback,
    errorCallback = null,
    signal = null,
  ) => {
    getFromOpenElisServer(
      `/rest/equipment/usage/lab/${labUnitId}`,
      callback,
      errorCallback,
      signal,
    );
  },

  /**
   * Submit complete equipment usage entry with all form fields
   * Records equipment usage with operator info, activities, login/logout times, approval info, etc.
   * @param {object} entryRequest - {
   *   itemId: number,
   *   lotId: number,
   *   quantity: number,
   *   operatorName: string,
   *   date: string,
   *   loginTime: string,
   *   activities: string,
   *   equipmentStatus: string,
   *   logoutTime: string,
   *   approvedBy: string,
   *   approvalDate: string
   * }
   * @returns {EquipmentUsageEntryDTO} Response with all submitted data plus database-generated fields
   */
  submitEquipmentUsageEntry: (entryRequest, callback, errorCallback = null) => {
    const payload = JSON.stringify(entryRequest);
    postToOpenElisServerFullResponse(
      "/rest/equipment/usage/submit",
      payload,
      callback,
      null,
    );
  },

  /**
   * Fetch all equipment usage submissions from the database (source of truth)
   * Returns complete entries with all form fields persisted to database
   * Used by dashboard on page load to fetch submissions
   * @param {string} startDate - Optional start date (ISO 8601 format)
   * @param {string} endDate - Optional end date (ISO 8601 format)
   */
  getEquipmentUsageSubmissions: (
    startDate = null,
    endDate = null,
    callback,
    errorCallback = null,
    signal = null,
  ) => {
    let url = "/rest/equipment/usage/submissions";
    if (startDate && endDate) {
      url += `?startDate=${startDate}&endDate=${endDate}`;
    }
    getFromOpenElisServer(url, callback, errorCallback, signal);
  },
};

export default CartridgeUsageAPI;
