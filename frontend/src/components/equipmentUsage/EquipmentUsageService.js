/**
 * Equipment Usage REST API Service
 * Handles all API calls for Equipment and Equipment Usage Entry operations
 * Uses Utils.js helper functions for consistent API handling (following Inventory pattern)
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const BASE_PATH = "/rest/equipment-usage";

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

// ==================== Equipment API ====================

export const EquipmentAPI = {
  /**
   * Get all active equipment
   */
  getAll: () => get("/equipment"),

  /**
   * Get equipment for dropdown (active only)
   */
  getForDropdown: () => get("/equipment/dropdown"),

  /**
   * Get equipment by ID
   */
  getById: (id) => get(`/equipment/${id}`),

  /**
   * Search equipment by name
   */
  search: (query) => get(`/equipment/search?q=${encodeURIComponent(query)}`),

  /**
   * Create new equipment
   */
  create: (equipment) => post("/equipment", equipment),

  /**
   * Update equipment
   */
  update: (id, equipment) => post(`/equipment/${id}`, equipment),
};

// ==================== Equipment Usage Entry API ====================

export const EquipmentUsageEntryAPI = {
  /**
   * Get all usage entries
   */
  getAll: () => get(""),

  /**
   * Get usage entry by ID
   */
  getById: (id) => get(`/${id}`),

  /**
   * Get usage entries by equipment
   */
  getByEquipmentId: (equipmentId) => get(`/equipment/${equipmentId}`),

  /**
   * Get entries pending approval
   */
  getPendingApproval: () => get("/pending-approval"),

  /**
   * Get approved entries
   */
  getApproved: () => get("/approved"),

  /**
   * Search usage entries with filters
   */
  search: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.equipmentId) params.append("equipmentId", filters.equipmentId);
    if (filters.operatorId) params.append("operatorId", filters.operatorId);
    if (filters.startDate) params.append("startDate", filters.startDate);
    if (filters.endDate) params.append("endDate", filters.endDate);
    if (filters.department) params.append("department", filters.department);
    if (filters.status) params.append("status", filters.status);
    const query = params.toString();
    return get(`/search${query ? `?${query}` : ""}`);
  },

  /**
   * Create new usage entry (as DRAFT)
   */
  create: (entry) => post("", entry),

  /**
   * Save usage entry as draft
   */
  saveDraft: (id, entry) => post(`/${id}/draft`, entry),

  /**
   * Submit entry for approval
   */
  submitForApproval: (id) => post(`/${id}/submit`, {}),

  /**
   * Approve entry
   */
  approve: (id, approverId) =>
    post(`/${id}/approve?approverId=${approverId}`, {}),

  /**
   * Reject entry
   */
  reject: (id) => post(`/${id}/reject`, {}),

  /**
   * Check if entry can be edited
   */
  canEdit: (id) => get(`/${id}/can-edit`),

  /**
   * Check if entry can be approved
   */
  canApprove: (id) => get(`/${id}/can-approve`),
};

// ==================== Backwards compatibility wrapper ====================

class EquipmentUsageService {
  // Equipment methods
  static getAllEquipment() {
    return EquipmentAPI.getAll();
  }

  static getEquipmentForDropdown() {
    return EquipmentAPI.getForDropdown();
  }

  static getEquipmentById(id) {
    return EquipmentAPI.getById(id);
  }

  static searchEquipment(query) {
    return EquipmentAPI.search(query);
  }

  static createEquipment(equipment) {
    return EquipmentAPI.create(equipment);
  }

  static updateEquipment(id, equipment) {
    return EquipmentAPI.update(id, equipment);
  }

  // Equipment Usage Entry methods
  static getAllUsageEntries() {
    return EquipmentUsageEntryAPI.getAll();
  }

  static getUsageEntryById(id) {
    return EquipmentUsageEntryAPI.getById(id);
  }

  static getByEquipmentId(equipmentId) {
    return EquipmentUsageEntryAPI.getByEquipmentId(equipmentId);
  }

  static getPendingApproval() {
    return EquipmentUsageEntryAPI.getPendingApproval();
  }

  static getApprovedEntries() {
    return EquipmentUsageEntryAPI.getApproved();
  }

  static searchUsageEntries(filters) {
    return EquipmentUsageEntryAPI.search(filters);
  }

  static createUsageEntry(entry) {
    return EquipmentUsageEntryAPI.create(entry);
  }

  static saveDraft(id, entry) {
    return EquipmentUsageEntryAPI.saveDraft(id, entry);
  }

  static submitForApproval(id) {
    return EquipmentUsageEntryAPI.submitForApproval(id);
  }

  static approveEntry(id, approverId) {
    return EquipmentUsageEntryAPI.approve(id, approverId);
  }

  static rejectEntry(id) {
    return EquipmentUsageEntryAPI.reject(id);
  }

  static canEditEntry(id) {
    return EquipmentUsageEntryAPI.canEdit(id);
  }

  static canApproveEntry(id) {
    return EquipmentUsageEntryAPI.canApprove(id);
  }
}

export default EquipmentUsageService;
