import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
  postToOpenElisServerForBlob,
} from "../utils/Utils";
import config from "../../config.json";

const BASE_PATH = "/rest/inventory";

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

const get = (endpoint) => {
  return promisify(getFromOpenElisServer, `${BASE_PATH}${endpoint}`);
};

const post = (endpoint, data) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      `${BASE_PATH}${endpoint}`,
      JSON.stringify(data),
      (json) => {
        if (json && (json.status >= 400 || json.statusCode >= 400)) {
          if (json.errors && typeof json.errors === "object") {
            const errorMessages = Object.entries(json.errors)
              .map(([field, message]) => `${field}: ${message}`)
              .join(", ");
            reject(new Error(errorMessages));
            return;
          }
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
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
          return response.json();
        }
        return {};
      })
      .then((json) => resolve(json))
      .catch((error) => reject(error));
  });
};

export const InventoryItemAPI = {
  getAll: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.itemType) params.append("itemType", filters.itemType);
    if (filters.isActive !== undefined)
      params.append("isActive", filters.isActive);
    const query = params.toString();
    return get(`/items/all${query ? `?${query}` : ""}`);
  },

  getAllActive: () => get("/items"),

  getById: (id) => get(`/items/${id}`),

  getItemTypes: () => get("/items/types"),

  getByType: (itemType) => get(`/items/type/${itemType}`),

  search: (query) => get(`/items/search?query=${encodeURIComponent(query)}`),

  getLowStock: () => get("/items/low-stock"),

  getStockLevel: (itemId) => get(`/items/${itemId}/stock`),

  create: (item) => post("/items", item),

  update: (id, item) => put(`/items/${id}`, item),

  deactivate: (id) => put(`/items/${id}/deactivate`, {}),

  activate: (id) => put(`/items/${id}/activate`, {}),

  getUnitOptions: () => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer("/rest/UomCreate", (response) => {
        if (response && response.existingUomList) {
          const formattedUnits = response.existingUomList.map((unit) => ({
            id: unit.id || unit.unitOfMeasureName || unit.value,
            text:
              unit.value || unit.unitOfMeasureName || unit.text || String(unit),
          }));
          resolve(formattedUnits);
        } else {
          resolve([]);
        }
      });
    });
  },

  createUnitOfMeasure: (unitName) => {
    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        "/rest/UomCreate",
        JSON.stringify({ uomEnglishName: unitName }),
        (response) => {
          if (response && response.error) {
            reject(new Error(response.message || response.error));
          } else {
            resolve(response);
          }
        },
        (error) => {
          reject(error);
        },
      );
    });
  },
};

export const InventoryLotAPI = {
  getAll: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.status) params.append("status", filters.status);
    if (filters.itemId) params.append("itemId", filters.itemId);
    const query = params.toString();
    return get(`/lots${query ? `?${query}` : ""}`);
  },

  getById: (id) => get(`/lots/${id}`),

  getAvailableByItem: (itemId) => get(`/lots/item/${itemId}/available`),

  getByItem: (itemId) => get(`/lots/item/${itemId}`),

  getByLocation: (locationId) => get(`/lots/location/${locationId}`),

  getExpiring: (days = 30) => get(`/lots/expiring?days=${days}`),

  getExpired: () => get("/lots/expired"),

  create: (lot) => post("/lots", lot),

  update: (id, lot) => put(`/lots/${id}`, lot),

  open: (id, openedDate) =>
    post(`/lots/${id}/open`, { openedDate: openedDate || new Date() }),

  updateQCStatus: (id, qcStatus, notes) =>
    put(`/lots/${id}/qc-status`, { qcStatus, notes }),

  adjust: (id, newQuantity, reason) =>
    post(`/lots/${id}/adjust`, { newQuantity, reason }),

  dispose: (id, reason, notes) =>
    post(`/lots/${id}/dispose`, { reason, notes }),

  batchDispose: (lotIds, reason, notes) =>
    post("/lots/batch-dispose", { lotIds, reason, notes }),

  processExpired: () => post("/lots/process-expired", {}),
};

export const InventoryManagementAPI = {
  consume: (consumeData) => post("/management/consume", consumeData),

  receive: (receiveData) => post("/management/receive", receiveData),

  checkAvailability: (itemId, quantity) =>
    get(`/management/check-availability?itemId=${itemId}&quantity=${quantity}`),

  getAlerts: (expirationWarningDays = 30) =>
    get(`/management/alerts?expirationWarningDays=${expirationWarningDays}`),
};

export const InventoryAuditLogAPI = {
  getItemAuditTrail: (itemId) => get(`/audit-logs/item/${itemId}`),

  getLotAuditTrail: (lotId) => get(`/audit-logs/lot/${lotId}`),

  getLocationAuditTrail: (locationId) =>
    get(`/audit-logs/location/${locationId}`),

  /**
   * Get unified audit logs across all inventory tables
   * @param {Object} filters - Filter options
   * @param {string} filters.startDate - Start date (yyyy-MM-dd)
   * @param {string} filters.endDate - End date (yyyy-MM-dd)
   * @param {string} filters.entityType - Entity type (ITEM, LOT, LOCATION, USAGE, TRANSACTION)
   * @param {string} filters.userId - User ID filter
   * @param {string} filters.activity - Activity type (I, U, D)
   * @param {number} filters.limit - Max records (default: 100, max: 1000)
   * @param {number} filters.offset - Pagination offset
   * @returns {Promise} Response with logs, totalRecords, limit, offset, hasMore
   */
  getAllAuditLogs: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.startDate) params.append("startDate", filters.startDate);
    if (filters.endDate) params.append("endDate", filters.endDate);
    if (filters.entityType) params.append("entityType", filters.entityType);
    if (filters.userId) params.append("userId", filters.userId);
    if (filters.activity) params.append("activity", filters.activity);
    if (filters.limit) params.append("limit", filters.limit);
    if (filters.offset) params.append("offset", filters.offset);

    const queryString = params.toString();
    return get(`/audit-logs/all${queryString ? `?${queryString}` : ""}`);
  },

  /**
   * Get audit log statistics
   * @returns {Promise} Statistics object with totalLogs, countByTable, countByActivity
   */
  getStatistics: () => get(`/audit-logs/statistics`),
};

export const StorageLocationAPI = {
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

export const TransactionAPI = {
  getById: (id) => get(`/transactions/${id}`),
  getByLot: (lotId) => get(`/transactions/lot/${lotId}`),
  getByType: (transactionType) => get(`/transactions/type/${transactionType}`),
  getByDateRange: (startDate, endDate) =>
    get(`/transactions/date-range?startDate=${startDate}&endDate=${endDate}`),
  getByReference: (referenceId, referenceType) =>
    get(
      `/transactions/reference?referenceId=${referenceId}&referenceType=${referenceType}`,
    ),
};

export const UsageAPI = {
  getByTestResult: (testResultId) => get(`/usage/test-result/${testResultId}`),
  getByLot: (lotId) => get(`/usage/lot/${lotId}`),
  getByItem: (itemId) => get(`/usage/item/${itemId}`),
  getByAnalysis: (analysisId) => get(`/usage/analysis/${analysisId}`),
};

export const ReportsAPI = {
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

export const NotebookDataAPI = {
  getNotebooks: () => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        "/rest/notebook/dashboard/notebooks",
        (response) => {
          if (response && response.error) {
            reject(new Error(response.message || response.error));
          } else {
            resolve(response || []);
          }
        },
      );
    });
  },

  getOrganizations: () => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer("/rest/notebook/organizations", (response) => {
        if (response && response.error) {
          reject(new Error(response.message || response.error));
        } else {
          resolve(response || []);
        }
      });
    });
  },

  getDepartments: () => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer("/rest/notebook/departments", (response) => {
        if (response && response.error) {
          reject(new Error(response.message || response.error));
        } else {
          resolve(response || []);
        }
      });
    });
  },
};
