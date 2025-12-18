import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../utils/Utils";

const BASE_PATH = "/rest/notebook";

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
    putToOpenElisServer(
      `${BASE_PATH}${endpoint}`,
      JSON.stringify(data),
      (status) => {
        if (status >= 200 && status < 300) {
          resolve({ success: true });
        } else {
          reject(new Error(`Failed to update: HTTP ${status}`));
        }
      },
    );
  });
};

export const NotebookAuditLogAPI = {
  /**
   * Get audit trail for a specific notebook (project/template)
   * @param {string} notebookId - The notebook ID
   * @returns {Promise} Array of audit log records with changes
   */
  getNotebookAuditTrail: (notebookId) =>
    get(`/audit-logs/notebook/${notebookId}`),

  /**
   * Get audit trail for a specific notebook instance (entry)
   * @param {string} instanceId - The notebook instance ID
   * @returns {Promise} Array of audit log records with changes
   */
  getInstanceAuditTrail: (instanceId) =>
    get(`/audit-logs/instance/${instanceId}`),

  /**
   * Get unified audit logs across all notebook tables
   * @param {Object} filters - Filter options
   * @param {string} filters.startDate - Start date (yyyy-MM-dd)
   * @param {string} filters.endDate - End date (yyyy-MM-dd)
   * @param {string} filters.entityType - Entity type (NOTEBOOK, INSTANCE)
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
