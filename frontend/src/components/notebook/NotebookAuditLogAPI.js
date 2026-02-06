/**
 * API service for Notebook Audit Log operations.
 *
 * Uses Utils.js methods for consistent API calls with authentication.
 */

import { getFromOpenElisServer } from "../utils/Utils";

const BASE_URL = "/rest/notebook/audit-logs";

export const NotebookAuditLogAPI = {
  /**
   * Get audit logs for a specific notebook template.
   *
   * @param {string} notebookId - The notebook ID
   * @param {function} callback - Callback function to receive the data
   */
  getNotebookAuditTrail: (notebookId, callback) => {
    getFromOpenElisServer(`${BASE_URL}/notebook/${notebookId}`, callback);
  },

  /**
   * Get ALL audit logs for a notebook and its related entities
   * (entries, pages, page samples, etc.).
   *
   * @param {string} notebookId - The notebook ID
   * @param {function} callback - Callback function to receive the data
   */
  getAllNotebookRelatedAuditLogs: (notebookId, callback) => {
    getFromOpenElisServer(`${BASE_URL}/notebook/${notebookId}/all`, callback);
  },

  /**
   * Get audit logs for a specific notebook entry.
   *
   * @param {string} entryId - The entry ID
   * @param {function} callback - Callback function to receive the data
   */
  getEntryAuditTrail: (entryId, callback) => {
    getFromOpenElisServer(`${BASE_URL}/entry/${entryId}`, callback);
  },

  /**
   * Get audit logs for a notebook page sample.
   *
   * @param {string} pageSampleId - The page sample ID
   * @param {function} callback - Callback function to receive the data
   */
  getPageSampleAuditTrail: (pageSampleId, callback) => {
    getFromOpenElisServer(`${BASE_URL}/page-sample/${pageSampleId}`, callback);
  },

  /**
   * Search audit logs with filters and pagination.
   *
   * @param {Object} filters - Filter criteria
   * @param {function} callback - Callback function to receive the data
   */
  searchAuditLogs: (filters, callback) => {
    const params = new URLSearchParams();

    if (filters.startDate) params.append("startDate", filters.startDate);
    if (filters.endDate) params.append("endDate", filters.endDate);
    if (filters.entityType) params.append("entityType", filters.entityType);
    if (filters.userId) params.append("userId", filters.userId);
    if (filters.activity) params.append("activity", filters.activity);
    if (filters.statusNew) params.append("statusNew", filters.statusNew);
    if (filters.referenceId) params.append("referenceId", filters.referenceId);
    if (filters.offset !== undefined) params.append("offset", filters.offset);
    if (filters.limit !== undefined) params.append("limit", filters.limit);

    getFromOpenElisServer(`${BASE_URL}/search?${params}`, callback);
  },

  /**
   * Get audit log statistics.
   *
   * @param {function} callback - Callback function to receive the data
   */
  getStatistics: (callback) => {
    getFromOpenElisServer(`${BASE_URL}/statistics`, callback);
  },

  /**
   * Get recent audit logs.
   *
   * @param {number} limit - Maximum number of logs to retrieve
   * @param {function} callback - Callback function to receive the data
   */
  getRecentAuditLogs: (limit, callback) => {
    getFromOpenElisServer(`${BASE_URL}/recent?limit=${limit}`, callback);
  },
};

export default NotebookAuditLogAPI;
