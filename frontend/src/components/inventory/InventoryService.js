import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
  postToOpenElisServerForBlob,
} from "../utils/Utils";

/**
 * Inventory API Service
 * Handles all API calls for inventory management (items, lots, storage locations, transactions)
 * Uses Utils.js for consistent CSRF protection and session management
 */

const BASE_PATH = "/rest/inventory";

// Helper to convert callback-based functions to promises
const promisify = (fn, ...args) => {
  return new Promise((resolve, reject) => {
    fn(...args, (response) => {
      if (response && response.error) {
        reject(new Error(response.message || response.error));
      } else {
        resolve(response);
      }
    });
  });
};

// Helper for GET requests
const get = (endpoint) => {
  return promisify(getFromOpenElisServer, `${BASE_PATH}${endpoint}`);
};

// Helper for POST requests returning JSON
const post = (endpoint, data) => {
  return promisify(
    postToOpenElisServerJsonResponse,
    `${BASE_PATH}${endpoint}`,
    JSON.stringify(data),
  );
};

// Helper for PUT requests
const put = (endpoint, data) => {
  return new Promise((resolve, reject) => {
    putToOpenElisServer(
      `${BASE_PATH}${endpoint}`,
      data ? JSON.stringify(data) : null,
      (status) => {
        if (status >= 200 && status < 300) {
          resolve({ status });
        } else {
          reject(new Error(`HTTP error! status: ${status}`));
        }
      },
    );
  });
};

/**
 * Inventory Item API
 */
export const InventoryItemAPI = {
  // Get all items with optional filters
  getAll: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.itemType) params.append("itemType", filters.itemType);
    if (filters.isActive !== undefined)
      params.append("isActive", filters.isActive);
    const query = params.toString();
    return get(`/items${query ? `?${query}` : ""}`);
  },

  // Get item by ID
  getById: (id) => get(`/items/${id}`),

  // Get items by type
  getByType: (itemType) => get(`/items/type/${itemType}`),

  // Search items by name
  search: (query) => get(`/items/search?query=${encodeURIComponent(query)}`),

  // Get low stock items
  getLowStock: () => get("/items/low-stock"),

  // Get stock level for an item
  getStockLevel: (itemId) => get(`/items/${itemId}/stock`),

  // Create new item
  create: (item) => post("/items", item),

  // Update item
  update: (id, item) => put(`/items/${id}`, item),

  // Deactivate item (soft delete)
  deactivate: (id) => put(`/items/${id}/deactivate`, {}),
};

/**
 * Inventory Lot API
 */
export const InventoryLotAPI = {
  // Get all lots with optional filters
  getAll: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.status) params.append("status", filters.status);
    if (filters.itemId) params.append("itemId", filters.itemId);
    const query = params.toString();
    return get(`/lots${query ? `?${query}` : ""}`);
  },

  // Get lot by ID
  getById: (id) => get(`/lots/${id}`),

  // Get available lots for an item (FEFO sorted)
  getAvailableByItem: (itemId) => get(`/lots/item/${itemId}/available`),

  // Get all lots for an item
  getByItem: (itemId) => get(`/lots/item/${itemId}`),

  // Get lots by storage location
  getByLocation: (locationId) => get(`/lots/location/${locationId}`),

  // Get expiring lots
  getExpiring: (days = 30) => get(`/lots/expiring?days=${days}`),

  // Get expired lots
  getExpired: () => get("/lots/expired"),

  // Create new lot
  create: (lot) => post("/lots", lot),

  // Update lot
  update: (id, lot) => put(`/lots/${id}`, lot),

  // Open lot (for reagents with stability tracking)
  open: (id, openedDate) =>
    post(`/lots/${id}/open`, { openedDate: openedDate || new Date() }),

  // Update QC status
  updateQCStatus: (id, qcStatus, notes) =>
    put(`/lots/${id}/qc-status`, { qcStatus, notes }),

  // Adjust quantity
  adjust: (id, newQuantity, reason, notes) =>
    post(`/lots/${id}/adjust`, { newQuantity, reason, notes }),

  // Dispose lot
  dispose: (id, reason, notes) =>
    post(`/lots/${id}/dispose`, { reason, notes }),

  // Process expired lots (batch operation)
  processExpired: () => post("/lots/process-expired", {}),
};

/**
 * Inventory Management API (FEFO consumption, receiving)
 */
export const InventoryManagementAPI = {
  // Consume inventory using FEFO algorithm
  consume: (consumeData) => post("/management/consume", consumeData),

  // Receive new inventory
  receive: (receiveData) => post("/management/receive", receiveData),

  // Check availability
  checkAvailability: (itemId, quantity) =>
    get(`/management/check-availability?itemId=${itemId}&quantity=${quantity}`),

  // Get inventory alerts (low stock, expiring, expired)
  getAlerts: (expirationWarningDays = 30) =>
    get(`/management/alerts?expirationWarningDays=${expirationWarningDays}`),
};

/**
 * Storage Location API
 */
export const StorageLocationAPI = {
  // Get all active locations
  getAll: () => get("/storage-locations"),

  // Get location by ID
  getById: (id) => get(`/storage-locations/${id}`),

  // Get top-level locations (no parent)
  getTopLevel: () => get("/storage-locations/top-level"),

  // Get child locations
  getChildren: (parentId) => get(`/storage-locations/${parentId}/children`),

  // Get location path (hierarchical breadcrumb)
  getPath: (id) => get(`/storage-locations/${id}/path`),

  // Check if location has active lots
  hasActiveLots: (id) => get(`/storage-locations/${id}/has-active-lots`),

  // Create location
  create: (location) => post("/storage-locations", location),

  // Update location
  update: (id, location) => put(`/storage-locations/${id}`, location),

  // Deactivate location
  deactivate: (id) => put(`/storage-locations/${id}/deactivate`, {}),
};

/**
 * Transaction API
 */
export const TransactionAPI = {
  // Get transaction by ID
  getById: (id) => get(`/transactions/${id}`),

  // Get transactions for a lot
  getByLot: (lotId) => get(`/transactions/lot/${lotId}`),

  // Get transactions by type
  getByType: (transactionType) => get(`/transactions/type/${transactionType}`),

  // Get transactions by date range
  getByDateRange: (startDate, endDate) =>
    get(`/transactions/date-range?startDate=${startDate}&endDate=${endDate}`),

  // Get transactions by reference (test result, etc.)
  getByReference: (referenceId, referenceType) =>
    get(
      `/transactions/reference?referenceId=${referenceId}&referenceType=${referenceType}`,
    ),
};

/**
 * Usage API (test result linkage)
 */
export const UsageAPI = {
  // Get usage by test result ID
  getByTestResult: (testResultId) => get(`/usage/test-result/${testResultId}`),

  // Get usage by lot ID
  getByLot: (lotId) => get(`/usage/lot/${lotId}`),

  // Get usage by item ID
  getByItem: (itemId) => get(`/usage/item/${itemId}`),

  // Get usage by analysis ID
  getByAnalysis: (analysisId) => get(`/usage/analysis/${analysisId}`),
};

/**
 * Reports API
 */
export const ReportsAPI = {
  // Generate inventory report
  generate: async (params) => {
    const queryParams = new URLSearchParams();
    if (params.reportType) queryParams.append("reportType", params.reportType);
    if (params.exportFormat)
      queryParams.append("exportFormat", params.exportFormat);
    if (params.startDate) queryParams.append("startDate", params.startDate);
    if (params.endDate) queryParams.append("endDate", params.endDate);
    if (params.includeInactive !== undefined)
      queryParams.append("includeInactive", params.includeInactive);
    if (params.includeExpired !== undefined)
      queryParams.append("includeExpired", params.includeExpired);
    if (params.groupByType !== undefined)
      queryParams.append("groupByType", params.groupByType);
    if (params.groupByLocation !== undefined)
      queryParams.append("groupByLocation", params.groupByLocation);

    const query = queryParams.toString();
    const endpoint = `${BASE_PATH}/reports/generate${query ? `?${query}` : ""}`;

    return new Promise((resolve, reject) => {
      postToOpenElisServerForBlob(
        endpoint,
        JSON.stringify({}),
        (blob, response) => {
          const contentType = response.headers.get("Content-Type");
          const contentDisposition = response.headers.get(
            "Content-Disposition",
          );
          let filename = "inventory-report";

          // Extract filename from Content-Disposition header if available
          if (contentDisposition) {
            const filenameMatch =
              contentDisposition.match(/filename="?(.+)"?/i);
            if (filenameMatch) {
              filename = filenameMatch[1];
            }
          }

          resolve({
            data: blob,
            contentType,
            filename,
          });
        },
        (error) => {
          reject(error);
        },
      );
    });
  },
};
