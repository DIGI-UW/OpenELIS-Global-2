import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
  postToOpenElisServerForBlob,
} from "../utils/Utils";
import config from "../../config.json";

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
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      `${BASE_PATH}${endpoint}`,
      JSON.stringify(data),
      (json) => {
        if (json && (json.status >= 400 || json.statusCode >= 400)) {
          // Handle validation errors object (field-level errors)
          if (json.errors && typeof json.errors === "object") {
            const errorMessages = Object.entries(json.errors)
              .map(([field, message]) => `${field}: ${message}`)
              .join(", ");
            reject(new Error(errorMessages));
            return;
          }
          // Handle standard message/error fields
          reject(
            new Error(
              json.message ||
                json.error ||
                `Request failed with status ${json.status || json.statusCode}`,
            ),
          );
        } else {
          resolve(json);
        }
      },
      null,
    );
  });
};

// Helper for PUT requests
const put = (endpoint, data) => {
  return new Promise((resolve, reject) => {
    fetch(`${config.serverBaseUrl}${BASE_PATH}${endpoint}`, {
      credentials: "include",
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: data ? JSON.stringify(data) : null,
    })
      .then((response) => {
        if (!response.ok) {
          return response
            .json()
            .then((errorJson) => {
              // Handle validation errors object
              if (errorJson.errors && typeof errorJson.errors === "object") {
                const errorMessages = Object.entries(errorJson.errors)
                  .map(([field, message]) => `${field}: ${message}`)
                  .join(", ");
                throw new Error(errorMessages);
              }
              throw new Error(
                errorJson.message ||
                  errorJson.error ||
                  `Failed to update: HTTP ${response.status}`,
              );
            })
            .catch((e) => {
              if (e.message && !e.message.includes("HTTP")) {
                throw e;
              }
              throw new Error(`Failed to update: HTTP ${response.status}`);
            });
        }
        // Check if response has content before parsing JSON
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
          return response.json();
        }
        // Return empty object for successful requests with no body
        return {};
      })
      .then((json) => resolve(json))
      .catch((error) => reject(error));
  });
};

/**
 * Inventory Item API
 */
export const InventoryItemAPI = {
  // Get all items (both active and inactive)
  getAll: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.itemType) params.append("itemType", filters.itemType);
    if (filters.isActive !== undefined)
      params.append("isActive", filters.isActive);
    const query = params.toString();
    return get(`/items/all${query ? `?${query}` : ""}`);
  },

  // Get only active items
  getAllActive: () => get("/items"),

  // Get item by ID
  getById: (id) => get(`/items/${id}`),

  // Get all item types
  getItemTypes: () => get("/items/types"),

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

  // Activate item (restore from soft delete)
  activate: (id) => put(`/items/${id}/activate`, {}),
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
  adjust: (id, newQuantity, reason) =>
    post(`/lots/${id}/adjust`, { newQuantity, reason }),

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
 * Uses inventory-specific storage locations (separate from sample storage)
 */
export const StorageLocationAPI = {
  // Get all active locations
  getAll: async () => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer("/rest/inventory-storage-locations", (response) => {
        if (response) {
          resolve(response);
        } else {
          reject(new Error("Failed to fetch storage locations"));
        }
      });
    });
  },

  // Get location by ID
  getById: async (id) => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        `/rest/inventory-storage-locations/${id}`,
        (response) => {
          if (response) {
            resolve(response);
          } else {
            reject(new Error("Failed to fetch storage location"));
          }
        },
      );
    });
  },

  // Get top-level locations (no parent)
  getTopLevel: async () => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        "/rest/inventory-storage-locations/top-level",
        (response) => {
          if (response) {
            resolve(response);
          } else {
            reject(new Error("Failed to fetch top-level locations"));
          }
        },
      );
    });
  },

  // Get child locations
  getChildren: async (parentId) => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        `/rest/inventory-storage-locations/${parentId}/children`,
        (response) => {
          if (response) {
            resolve(response);
          } else {
            reject(new Error("Failed to fetch child locations"));
          }
        },
      );
    });
  },

  // Get location path (hierarchical breadcrumb)
  getPath: async (id) => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        `/rest/inventory-storage-locations/${id}/path`,
        (response) => {
          if (response) {
            resolve(response);
          } else {
            reject(new Error("Failed to fetch location path"));
          }
        },
      );
    });
  },

  // Check if location has active lots
  hasActiveLots: async (id) => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        `/rest/inventory-storage-locations/${id}/has-active-lots`,
        (response) => {
          if (response) {
            resolve(response);
          } else {
            reject(new Error("Failed to check active lots"));
          }
        },
      );
    });
  },

  // Create location
  create: async (location) => {
    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        "/rest/inventory-storage-locations",
        JSON.stringify(location),
        (json) => {
          if (json && (json.status >= 400 || json.statusCode >= 400)) {
            reject(
              new Error(
                json.message ||
                  json.error ||
                  `Request failed with status ${json.status || json.statusCode}`,
              ),
            );
          } else {
            resolve(json);
          }
        },
        null,
      );
    });
  },

  // Update location
  update: async (id, location) => {
    return new Promise((resolve, reject) => {
      putToOpenElisServer(
        `/rest/inventory-storage-locations/${id}`,
        JSON.stringify(location),
        (status) => {
          if (status >= 200 && status < 300) {
            resolve({ success: true });
          } else {
            reject(new Error(`Failed to update location: HTTP ${status}`));
          }
        },
      );
    });
  },

  // Deactivate location
  deactivate: async (id) => {
    return new Promise((resolve, reject) => {
      putToOpenElisServer(
        `/rest/inventory-storage-locations/${id}/deactivate`,
        "{}",
        (status) => {
          if (status >= 200 && status < 300) {
            resolve({ success: true });
          } else {
            reject(new Error(`Failed to deactivate location: HTTP ${status}`));
          }
        },
      );
    });
  },
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
