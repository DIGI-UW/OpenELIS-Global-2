import {
  getFromOpenElisServerV2,
  postToOpenElisServerJsonResponse,
  postToOpenElisServerForPDF,
  putToOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";
import config from "../../config.json";

export const fetchFreezerStatus = async (filters = {}) => {
  const params = new URLSearchParams();
  if (filters.roomId) {
    params.append("roomId", filters.roomId);
  }
  if (filters.status) {
    params.append("status", filters.status);
  }
  const queryString = params.toString() ? `?${params.toString()}` : "";
  return getFromOpenElisServerV2(`/rest/coldstorage/status${queryString}`);
};

export const fetchAlerts = async (entityType, entityId) => {
  const params = new URLSearchParams({ entityType, entityId });
  return getFromOpenElisServerV2(`/rest/alerts?${params.toString()}`);
};

export const fetchOpenAlerts = async () => {
  try {
    const params = new URLSearchParams({ entityType: "Freezer" });
    const response = await getFromOpenElisServerV2(
      `/rest/alerts?${params.toString()}`,
    );
    return Array.isArray(response) ? response : [];
  } catch (error) {
    console.error("Error fetching open alerts:", error);
    return [];
  }
};

const postColdStorageJson = (path, payload) =>
  new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      path,
      JSON.stringify(payload),
      (json) => {
        if (json && (json.status >= 400 || json.statusCode >= 400)) {
          const error = new Error(
            json.message ||
              json.error ||
              `Request failed with status ${json.status || json.statusCode}`,
          );
          error.status = json.status || json.statusCode;
          reject(error);
        } else {
          resolve(json);
        }
      },
      null,
    );
  });

export const acknowledgeAlert = async (alertId, userId, notes = "") => {
  return new Promise((resolve, reject) => {
    fetch(`${config.serverBaseUrl}/rest/alerts/${alertId}/acknowledge`, {
      credentials: "include",
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify({ userId, notes }),
    })
      .then((response) => {
        if (!response.ok) {
          // For error responses, try to parse JSON error message
          return response
            .json()
            .catch(() => ({}))
            .then((errorJson) => {
              const error = new Error(
                errorJson.message ||
                  `Failed to acknowledge alert: HTTP ${response.status}`,
              );
              error.status = response.status;
              throw error;
            });
        }
        return response.json();
      })
      .then((json) => resolve(json))
      .catch((error) => reject(error));
  });
};

export const resolveAlert = async (alertId, userId, resolutionNotes) => {
  return new Promise((resolve, reject) => {
    fetch(`${config.serverBaseUrl}/rest/alerts/${alertId}/resolve`, {
      credentials: "include",
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify({ userId, resolutionNotes }),
    })
      .then((response) => {
        if (!response.ok) {
          // For error responses, try to parse JSON error message
          return response
            .json()
            .catch(() => ({}))
            .then((errorJson) => {
              const error = new Error(
                errorJson.message ||
                  `Failed to resolve alert: HTTP ${response.status}`,
              );
              error.status = response.status;
              throw error;
            });
        }
        return response.json();
      })
      .then((json) => resolve(json))
      .catch((error) => reject(error));
  });
};

export const fetchHistoricalReadings = async (freezerId, start, end) => {
  const params = new URLSearchParams({
    start,
    end,
  });
  return getFromOpenElisServerV2(
    `/rest/coldstorage/id/${freezerId}/readings?${params.toString()}`,
  );
};

export const fetchCorrectiveActions = async (filters = {}) => {
  const params = new URLSearchParams();
  if (filters.freezerId) {
    params.append("freezerId", filters.freezerId);
  }
  if (filters.status) {
    params.append("status", filters.status);
  }
  if (filters.startDate) {
    params.append("startDate", filters.startDate);
  }
  if (filters.endDate) {
    params.append("endDate", filters.endDate);
  }
  const queryString = params.toString() ? `?${params.toString()}` : "";
  return getFromOpenElisServerV2(
    `/rest/coldstorage/corrective-actions${queryString}`,
  );
};

export const fetchCorrectiveActionById = async (actionId) => {
  return getFromOpenElisServerV2(
    `/rest/coldstorage/corrective-actions/${actionId}`,
  );
};

export const createCorrectiveAction = async (
  freezerId,
  actionType,
  description,
) => {
  return postColdStorageJson("/rest/coldstorage/corrective-actions", {
    freezerId,
    actionType,
    description,
  });
};

export const updateCorrectiveAction = async (actionId, description, status) => {
  return new Promise((resolve, reject) => {
    putToOpenElisServer(
      `/rest/coldstorage/corrective-actions/${actionId}`,
      JSON.stringify({ description, status }),
      (response) => {
        try {
          const json = JSON.parse(response);
          resolve(json);
        } catch (e) {
          resolve({ success: true });
        }
      },
    );
  });
};

export const completeCorrectiveAction = async (actionId, completionNotes) => {
  return new Promise((resolve, reject) => {
    putToOpenElisServer(
      `/rest/coldstorage/corrective-actions/${actionId}/complete`,
      JSON.stringify({ completionNotes }),
      (response) => {
        try {
          const json = JSON.parse(response);
          resolve(json);
        } catch (e) {
          resolve({ success: true });
        }
      },
    );
  });
};

export const retractCorrectiveAction = async (actionId, retractionReason) => {
  return new Promise((resolve, reject) => {
    putToOpenElisServer(
      `/rest/coldstorage/corrective-actions/${actionId}/retract`,
      JSON.stringify({ retractionReason }),
      (response) => {
        try {
          const json = JSON.parse(response);
          resolve(json);
        } catch (e) {
          resolve({ success: true });
        }
      },
    );
  });
};

export const fetchReportExcursions = async ({ freezerId, start, end }) => {
  const params = new URLSearchParams({ start, end });
  if (freezerId) {
    params.append("freezerId", freezerId);
  }
  return getFromOpenElisServerV2(
    `/rest/coldstorage/reports/excursions?${params.toString()}`,
  );
};

export const fetchAuditTrail = async ({ freezerId }) => {
  const params = new URLSearchParams();
  if (freezerId) {
    params.append("freezerId", freezerId);
  }
  // Fetch all audit trail data without date filtering (client-side filtering instead)
  return getFromOpenElisServerV2(
    `/rest/coldstorage/audit-trail${params.toString() ? `?${params.toString()}` : ""}`,
  );
};

export const downloadReportDirect = ({
  reportName,
  format,
  startDate,
  endDate,
  freezerId,
}) => {
  return new Promise((resolve, reject) => {
    const endpoint = `/rest/coldstorage/reports/generate`;
    const payload = JSON.stringify({
      reportType: reportName,
      freezerId:
        freezerId && freezerId !== "All Freezers" ? String(freezerId) : null,
      startDate: startDate,
      endDate: endDate,
    });

    postToOpenElisServerForPDF(endpoint, payload, (success, blob) => {
      if (success) {
        resolve(blob);
      } else {
        reject(new Error("Failed to generate report"));
      }
    });
  });
};

export const fetchLocations = async () => {
  return getFromOpenElisServerV2("/rest/storage/rooms");
};

export const createRoom = async (roomData) => {
  return postColdStorageJson("/rest/storage/rooms", roomData);
};

export const fetchDevices = async (search = "") => {
  const params = search ? `?search=${encodeURIComponent(search)}` : "";
  return getFromOpenElisServerV2(`/rest/coldstorage/devices${params}`);
};

export const fetchDevice = async (id) => {
  return getFromOpenElisServerV2(`/rest/coldstorage/devices/${id}`);
};

export const fetchDeviceByName = async (name) => {
  return getFromOpenElisServerV2(
    `/rest/coldstorage/devices/name/${encodeURIComponent(name)}`,
  );
};

export const createDevice = async (deviceData) => {
  const { roomId, ...freezer } = deviceData;
  const params = new URLSearchParams({ roomId });
  return postColdStorageJson(
    `/rest/coldstorage/devices?${params.toString()}`,
    freezer,
  );
};

export const updateDevice = async (id, deviceData) => {
  const { roomId, ...freezer } = deviceData;
  const params = new URLSearchParams();
  if (roomId) {
    params.append("roomId", roomId);
  }
  const queryString = params.toString() ? `?${params.toString()}` : "";
  // putToOpenElisServer only exposes the status code, so a validation
  // failure's actual message (e.g. duplicate name/code) never reached the
  // user - putToOpenElisServerFullResponse gives the real Response to parse.
  return new Promise((resolve, reject) => {
    putToOpenElisServerFullResponse(
      `/rest/coldstorage/devices/${id}${queryString}`,
      JSON.stringify(freezer),
      (response) => {
        if (!response) {
          reject(new Error("Failed to update device: network error"));
          return;
        }
        response
          .json()
          .catch(() => ({}))
          .then((json) => {
            if (!response.ok) {
              const error = new Error(
                json.message ||
                  `Failed to update device: HTTP ${response.status}`,
              );
              error.status = response.status;
              reject(error);
            } else {
              resolve(json);
            }
          });
      },
    );
  });
};

export const updateDeviceThresholds = async (
  id,
  targetTemperature,
  warningThreshold,
  criticalThreshold,
  pollingIntervalSeconds,
) => {
  return new Promise((resolve, reject) => {
    putToOpenElisServer(
      `/rest/coldstorage/devices/${id}/thresholds`,
      JSON.stringify({
        targetTemperature,
        warningThreshold,
        criticalThreshold,
        pollingIntervalSeconds,
      }),
      (status) => {
        if (status >= 200 && status < 300) {
          resolve({ success: true });
        } else {
          reject(
            new Error(
              `Failed to update thresholds: HTTP ${status}${
                status === 404 ? " - Endpoint not found" : ""
              }`,
            ),
          );
        }
      },
    );
  });
};

export const toggleDeviceStatus = async (id, active) => {
  return postColdStorageJson(`/rest/coldstorage/devices/${id}/toggle-status`, {
    active,
  });
};

export const deleteDevice = async (id) => {
  return postColdStorageJson(`/rest/coldstorage/devices/${id}/delete`, {});
};

export const fetchFilteredAlerts = async (filters = {}) => {
  const params = new URLSearchParams();
  if (filters.entityType) {
    params.append("entityType", filters.entityType);
  }
  if (filters.entityId) {
    params.append("entityId", filters.entityId);
  }
  if (filters.severity) {
    params.append("severity", filters.severity);
  }
  if (filters.status) {
    params.append("status", filters.status);
  }
  if (filters.page !== undefined && filters.page !== null) {
    params.append("page", filters.page);
  }
  if (filters.size !== undefined && filters.size !== null) {
    params.append("size", filters.size);
  }

  const queryString = params.toString() ? `?${params.toString()}` : "";
  return getFromOpenElisServerV2(`/rest/alerts${queryString}`).then(
    (alerts) => ({
      content: alerts || [],
      totalElements: alerts ? alerts.length : 0,
    }),
  );
};

export const deleteAlert = async (alertId) => {
  return new Promise((resolve, reject) => {
    fetch(`${config.serverBaseUrl}/rest/alerts/${alertId}`, {
      credentials: "include",
      method: "DELETE",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
    })
      .then((response) => {
        if (!response.ok) {
          return response
            .json()
            .catch(() => ({}))
            .then((errorJson) => {
              const error = new Error(
                errorJson.message ||
                  `Failed to delete alert: HTTP ${response.status}`,
              );
              error.status = response.status;
              throw error;
            });
        }
        return response.status === 204 ? {} : response.json().catch(() => ({}));
      })
      .then((json) => resolve(json))
      .catch((error) => reject(error));
  });
};

export const fetchAlertDetails = async (alertId) => {
  return getFromOpenElisServerV2(`/rest/alerts/${alertId}`);
};

export const bulkAcknowledgeAlerts = async (alertIds, userId, notes = "") => {
  const promises = alertIds.map((id) => acknowledgeAlert(id, userId, notes));
  return Promise.all(promises).then(() => ({ success: true }));
};

export const bulkResolveAlerts = async (alertIds, userId, resolutionNotes) => {
  const promises = alertIds.map((id) =>
    resolveAlert(id, userId, resolutionNotes),
  );
  return Promise.all(promises).then(() => ({ success: true }));
};

export const fetchStorageDevices = async () => {
  return getFromOpenElisServerV2("/rest/coldstorage/storage-devices");
};

export const fetchUsers = async () => {
  return getFromOpenElisServerV2("/rest/coldstorage/users");
};

export const fetchAlertConfig = async () => {
  return getFromOpenElisServerV2("/rest/alert-notification-config");
};

export const saveAlertConfig = async (configData) => {
  return postColdStorageJson("/rest/alert-notification-config", configData);
};
