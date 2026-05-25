import { getFromOpenElisServer } from "../../utils/Utils";
import { mapLinkedEquipmentOptions } from "../notebookLinkedEquipment";

/** Pseudo-departments — admin role assignment only, never owning scope. */
export const OWNERSHIP_PSEUDO_DEPARTMENT_NAMES = ["AllLabUnits", "All Lab Units"];

export const isOwningDepartment = (department) => {
  const name = String(
    department?.name ?? department?.shortName ?? department?.testSectionName ?? "",
  ).trim();
  if (!name) {
    return true;
  }
  return !OWNERSHIP_PSEUDO_DEPARTMENT_NAMES.some(
    (pseudo) => pseudo.toLowerCase() === name.toLowerCase(),
  );
};

export const filterOwningDepartments = (departments = []) =>
  (departments || []).filter(isOwningDepartment);

export const buildDepartmentQuery = (departmentIds = []) =>
  departmentIds
    .filter(
      (departmentId) => departmentId !== undefined && departmentId !== null,
    )
    .map((departmentId) => `departmentIds=${encodeURIComponent(departmentId)}`)
    .join("&");

export const appendDepartmentScope = (endpoint, departmentIds = []) => {
  const departmentQuery = buildDepartmentQuery(departmentIds);
  if (!departmentQuery) {
    return endpoint;
  }
  return `${endpoint}${endpoint.includes("?") ? "&" : "?"}${departmentQuery}`;
};

export const loadNotebookDepartmentIds = (notebookId, callback, signal = null) => {
  if (!notebookId) {
    callback([]);
    return;
  }

  getFromOpenElisServer(
    `/rest/notebook/${notebookId}/departments`,
    (departments, departmentError) => {
      if (departmentError || !Array.isArray(departments)) {
        callback([], departmentError);
        return;
      }
      const ids = filterOwningDepartments(departments)
        .map((department) => department?.id)
        .filter(Boolean);
      callback(ids, null);
    },
    signal,
  );
};

export const loadNotebookScopedInventory = (
  notebookId,
  endpoint,
  callback,
  signal = null,
) => {
  loadNotebookDepartmentIds(notebookId, (departmentIds, departmentError) => {
    if (departmentError || departmentIds.length === 0) {
      callback([], departmentError);
      return;
    }

    getFromOpenElisServer(
      appendDepartmentScope(endpoint, departmentIds),
      callback,
      signal,
    );
  }, signal);
};

export const loadNotebookEquipmentOptions = (notebookId, buildUrl, callback, signal = null) => {
  loadNotebookDepartmentIds(notebookId, (departmentIds, departmentError) => {
    if (departmentError || departmentIds.length === 0) {
      callback([], departmentError);
      return;
    }
    getFromOpenElisServer(
      buildUrl(departmentIds),
      (response, error) => {
        if (error) {
          callback([], error);
          return;
        }
        callback(mapLinkedEquipmentOptions(response));
      },
      signal,
    );
  }, signal);
};
