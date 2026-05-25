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

const normalizeDepartmentText = (value) =>
  String(value || "")
    .trim()
    .toLowerCase()
    .replace(/&/g, " and ")
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();

const extractDepartmentIds = (departments = []) =>
  filterOwningDepartments(departments)
    .map((department) => department?.id)
    .filter(Boolean);

const inferDepartmentIdsFromNotebook = (notebook, candidates = []) => {
  const haystacks = [
    notebook?.title,
    notebook?.name,
    notebook?.protocol,
    notebook?.objective,
  ]
    .filter(Boolean)
    .map(normalizeDepartmentText)
    .filter(Boolean);

  if (haystacks.length === 0) {
    return [];
  }

  return filterOwningDepartments(candidates)
    .filter((department) => {
      const labels = [
        department?.name,
        department?.shortName,
        department?.label,
        department?.testSectionName,
        department?.value,
      ]
        .filter(Boolean)
        .map(normalizeDepartmentText)
        .filter(Boolean);

      return labels.some((label) =>
        haystacks.some(
          (haystack) =>
            haystack.includes(label) || label.includes(haystack),
        ),
      );
    })
    .map((department) => department?.id)
    .filter(Boolean);
};

const loadAssignableDepartmentIds = (notebook, callback, signal = null) => {
  getFromOpenElisServer(
    "/rest/inventory/items/assignable-departments",
    (departments, error) => {
      if (error || !Array.isArray(departments)) {
        callback([], error);
        return;
      }
      callback(inferDepartmentIdsFromNotebook(notebook, departments), null);
    },
    signal,
  );
};

export const loadNotebookDepartmentIds = (notebookId, callback, signal = null) => {
  if (!notebookId) {
    callback([]);
    return;
  }

  const resolveIdsFromNotebook = (notebook) =>
    extractDepartmentIds(notebook?.departments || []);

  getFromOpenElisServer(
    `/rest/notebook/${notebookId}/departments`,
    (departments, departmentError) => {
      const idsFromLinkedDepartments = Array.isArray(departments)
        ? filterOwningDepartments(departments)
            .map((department) => department?.id)
            .filter(Boolean)
        : [];

      if (idsFromLinkedDepartments.length > 0) {
        callback(idsFromLinkedDepartments, null);
        return;
      }

      getFromOpenElisServer(
        `/rest/notebook/view/${notebookId}`,
        (notebook, notebookError) => {
          if (notebookError || !notebook) {
            callback([], departmentError || notebookError);
            return;
          }
          const idsFromNotebook = resolveIdsFromNotebook(notebook);
          if (idsFromNotebook.length > 0) {
            callback(idsFromNotebook, null);
            return;
          }
          loadAssignableDepartmentIds(
            notebook,
            (matchedIds, matchedError) => {
              callback(matchedIds, departmentError || notebookError || matchedError);
            },
            signal,
          );
        },
        signal,
      );
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
