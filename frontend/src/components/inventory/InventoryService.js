import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
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
          const error = new Error(
            json.message ||
              json.error ||
              `Request failed with status ${json.status || json.statusCode}`,
          );
          // errorCode/params (LocalizedValidationException, see
          // InventoryItemRestController) let the
          // caller show a translated message instead of this raw English fallback.
          if (json.errorCode) {
            error.errorCode = json.errorCode;
            error.params = json.params;
          }
          reject(error);
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

  // Get item by ID
  getById: (id) => get(`/items/${id}`),

  // Get all item types
  getItemTypes: () => get("/items/types"),

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

  // Update lot
  update: (id, lot) => put(`/lots/${id}`, lot),

  // Update QC status
  updateQCStatus: (id, qcStatus, notes) =>
    put(`/lots/${id}/qc-status`, { qcStatus, notes }),

  // Adjust quantity
  adjust: (id, newQuantity, reason) =>
    post(`/lots/${id}/adjust`, { newQuantity, reason }),

  // Dispose lot
  dispose: (id, reason, notes) =>
    post(`/lots/${id}/dispose`, { reason, notes }),
};

/**
 * Inventory Management API (FEFO consumption, receiving)
 */
export const InventoryManagementAPI = {
  // Consume inventory using FEFO algorithm
  consume: (consumeData) => post("/management/consume", consumeData),

  // Receive new inventory
  receive: (receiveData) => post("/management/receive", receiveData),
};

/**
 * Inventory Lot Storage API (OGC-657)
 * Assigns/moves an InventoryLot's location using the same
 * sample_storage_assignment-backed endpoints and audit trail as sample
 * storage, keyed by inventoryLotId instead of sampleItemId.
 */
const STORAGE_BASE_PATH = "/rest/storage/inventory-lots";

export const InventoryLotStorageAPI = {
  // Get current location for a lot (empty object if unassigned)
  getLocation: (lotId) =>
    promisify(getFromOpenElisServer, `${STORAGE_BASE_PATH}/${lotId}`),

  // Assign a lot to a location for the first time
  assignLocation: (payload) =>
    new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        `${STORAGE_BASE_PATH}/assign`,
        JSON.stringify(payload),
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
    }),

  // Move an already-assigned lot to a new location
  moveLocation: (payload) =>
    new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        `${STORAGE_BASE_PATH}/move`,
        JSON.stringify(payload),
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
    }),
};

/**
 * Transaction API
 */
export const TransactionAPI = {
  // Get transactions for a lot
  getByLot: (lotId) => get(`/transactions/lot/${lotId}`),
};

/**
 * Usage API (test result linkage)
 */
export const UsageAPI = {
  // Get usage by lot ID
  getByLot: (lotId) => get(`/usage/lot/${lotId}`),
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
