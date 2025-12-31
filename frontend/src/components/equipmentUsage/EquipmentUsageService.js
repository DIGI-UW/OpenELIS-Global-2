/**
 * Equipment Usage REST API Service
 * Handles all API calls for Equipment and Equipment Usage Entry operations
 */

const API_BASE_URL = "/rest";

class EquipmentUsageService {
  // ==================== Equipment API ====================

  /**
   * Get all active equipment
   */
  static getAllEquipment() {
    return fetch(`${API_BASE_URL}/equipment`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to fetch equipment");
      return response.json();
    });
  }

  /**
   * Get equipment for dropdown (active only)
   */
  static getEquipmentForDropdown() {
    return fetch(`${API_BASE_URL}/equipment/dropdown`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to fetch equipment dropdown");
      return response.json();
    });
  }

  /**
   * Get equipment by ID
   */
  static getEquipmentById(id) {
    return fetch(`${API_BASE_URL}/equipment/${id}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to fetch equipment");
      return response.json();
    });
  }

  /**
   * Search equipment by name
   */
  static searchEquipment(query) {
    return fetch(`${API_BASE_URL}/equipment/search?q=${encodeURIComponent(query)}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to search equipment");
      return response.json();
    });
  }

  /**
   * Create new equipment
   */
  static createEquipment(equipment) {
    return fetch(`${API_BASE_URL}/equipment`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(equipment),
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to create equipment");
      return response.json();
    });
  }

  /**
   * Update equipment
   */
  static updateEquipment(id, equipment) {
    return fetch(`${API_BASE_URL}/equipment/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(equipment),
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to update equipment");
      return response.json();
    });
  }

  // ==================== Equipment Usage Entry API ====================

  /**
   * Get all usage entries
   */
  static getAllUsageEntries() {
    return fetch(`${API_BASE_URL}/equipment-usage`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to fetch usage entries");
      return response.json();
    });
  }

  /**
   * Get usage entry by ID
   */
  static getUsageEntryById(id) {
    return fetch(`${API_BASE_URL}/equipment-usage/${id}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to fetch usage entry");
      return response.json();
    });
  }

  /**
   * Get usage entries by equipment
   */
  static getByEquipmentId(equipmentId) {
    return fetch(`${API_BASE_URL}/equipment-usage/equipment/${equipmentId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to fetch usage entries");
      return response.json();
    });
  }

  /**
   * Get entries pending approval
   */
  static getPendingApproval() {
    return fetch(`${API_BASE_URL}/equipment-usage/pending-approval`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to fetch pending entries");
      return response.json();
    });
  }

  /**
   * Get approved entries
   */
  static getApprovedEntries() {
    return fetch(`${API_BASE_URL}/equipment-usage/approved`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to fetch approved entries");
      return response.json();
    });
  }

  /**
   * Search usage entries with filters
   */
  static searchUsageEntries(filters) {
    const params = new URLSearchParams();
    if (filters.equipmentId) params.append("equipmentId", filters.equipmentId);
    if (filters.operatorId) params.append("operatorId", filters.operatorId);
    if (filters.startDate) params.append("startDate", filters.startDate);
    if (filters.endDate) params.append("endDate", filters.endDate);
    if (filters.department) params.append("department", filters.department);
    if (filters.status) params.append("status", filters.status);

    return fetch(`${API_BASE_URL}/equipment-usage/search?${params}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to search usage entries");
      return response.json();
    });
  }

  /**
   * Create new usage entry (as DRAFT)
   */
  static createUsageEntry(entry) {
    return fetch(`${API_BASE_URL}/equipment-usage`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(entry),
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to create usage entry");
      return response.json();
    });
  }

  /**
   * Save usage entry as draft
   */
  static saveDraft(id, entry) {
    return fetch(`${API_BASE_URL}/equipment-usage/${id}/draft`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(entry),
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to save draft");
      return response.json();
    });
  }

  /**
   * Submit entry for approval
   */
  static submitForApproval(id) {
    return fetch(`${API_BASE_URL}/equipment-usage/${id}/submit`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to submit entry");
      return response.json();
    });
  }

  /**
   * Approve entry
   */
  static approveEntry(id, approverId) {
    return fetch(`${API_BASE_URL}/equipment-usage/${id}/approve?approverId=${approverId}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to approve entry");
      return response.json();
    });
  }

  /**
   * Reject entry
   */
  static rejectEntry(id) {
    return fetch(`${API_BASE_URL}/equipment-usage/${id}/reject`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to reject entry");
      return response.json();
    });
  }

  /**
   * Check if entry can be edited
   */
  static canEditEntry(id) {
    return fetch(`${API_BASE_URL}/equipment-usage/${id}/can-edit`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to check edit permission");
      return response.json();
    });
  }

  /**
   * Check if entry can be approved
   */
  static canApproveEntry(id) {
    return fetch(`${API_BASE_URL}/equipment-usage/${id}/can-approve`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }).then((response) => {
      if (!response.ok) throw new Error("Failed to check approve permission");
      return response.json();
    });
  }
}

export default EquipmentUsageService;
