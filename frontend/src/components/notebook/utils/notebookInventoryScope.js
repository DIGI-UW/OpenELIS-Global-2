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

export const NOTEBOOK_INVENTORY_SCOPE_STATUS = {
  READY: "ready",
  DEPARTMENT_SCOPE_UNAVAILABLE: "departmentScopeUnavailable",
  NO_INVENTORY_EQUIPMENT: "noInventoryEquipment",
  NO_INVENTORY_LOTS: "noInventoryLots",
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
    callback([], null, {
      scopeStatus: NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE,
      departmentIds: [],
    });
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
        callback(idsFromLinkedDepartments, null, {
          scopeStatus: NOTEBOOK_INVENTORY_SCOPE_STATUS.READY,
          departmentIds: idsFromLinkedDepartments,
        });
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
            callback(idsFromNotebook, null, {
              scopeStatus: NOTEBOOK_INVENTORY_SCOPE_STATUS.READY,
              departmentIds: idsFromNotebook,
            });
            return;
          }
          loadAssignableDepartmentIds(
            notebook,
            (matchedIds, matchedError) => {
              callback(matchedIds, departmentError || notebookError || matchedError, {
                scopeStatus:
                  matchedIds.length > 0
                    ? NOTEBOOK_INVENTORY_SCOPE_STATUS.READY
                    : NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE,
                departmentIds: matchedIds,
              });
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
  loadNotebookDepartmentIds(
    notebookId,
    (departmentIds, departmentError, scopeMeta = {}) => {
      if (departmentError || departmentIds.length === 0) {
        callback([], departmentError, {
          ...scopeMeta,
          scopeStatus:
            scopeMeta.scopeStatus ||
            NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE,
        });
        return;
      }

      getFromOpenElisServer(
        appendDepartmentScope(endpoint, departmentIds),
        (response, error) => {
          const nextStatus =
            error || !Array.isArray(response) || response.length > 0
              ? scopeMeta.scopeStatus || NOTEBOOK_INVENTORY_SCOPE_STATUS.READY
              : NOTEBOOK_INVENTORY_SCOPE_STATUS.NO_INVENTORY_LOTS;

          callback(response, error, {
            ...scopeMeta,
            departmentIds,
            scopeStatus: nextStatus,
          });
        },
        signal,
      );
    },
    signal,
  );
};

export const mergeInventoryOptionsWithLinkedSelections = (
  inventoryOptions = [],
  linkedSelections = [],
  unavailableSuffix = "Unavailable in department inventory",
) => {
  const merged = [...(inventoryOptions || [])];
  const seenIds = new Set(merged.map((item) => String(item.id)));

  (linkedSelections || []).forEach((item) => {
    const id = String(item?.id ?? item?.value ?? "");
    if (!id || seenIds.has(id)) {
      return;
    }
    seenIds.add(id);
    merged.push({
      id: item.id ?? item.value,
      value: item.value || item.label || item.name || String(item.id ?? item.value),
      label: item.label || item.value || item.name || String(item.id ?? item.value),
      itemType: item.itemType,
      unavailableInDepartmentInventory: true,
      availabilityNote: unavailableSuffix,
    });
  });

  return merged;
};

export const loadNotebookEquipmentOptions = (notebookId, buildUrl, callback, signal = null) => {
  loadNotebookDepartmentIds(
    notebookId,
    (departmentIds, departmentError, scopeMeta = {}) => {
      if (departmentError || departmentIds.length === 0) {
        callback([], departmentError, {
          ...scopeMeta,
          scopeStatus:
            scopeMeta.scopeStatus ||
            NOTEBOOK_INVENTORY_SCOPE_STATUS.DEPARTMENT_SCOPE_UNAVAILABLE,
        });
        return;
      }
      getFromOpenElisServer(
        buildUrl(departmentIds),
        (response, error) => {
          if (error) {
            callback([], error, {
              ...scopeMeta,
              departmentIds,
              scopeStatus: scopeMeta.scopeStatus || NOTEBOOK_INVENTORY_SCOPE_STATUS.READY,
            });
            return;
          }

          const mappedOptions = mapLinkedEquipmentOptions(response);
          callback(mappedOptions, null, {
            ...scopeMeta,
            departmentIds,
            scopeStatus:
              mappedOptions.length > 0
                ? NOTEBOOK_INVENTORY_SCOPE_STATUS.READY
                : NOTEBOOK_INVENTORY_SCOPE_STATUS.NO_INVENTORY_EQUIPMENT,
          });
        },
        signal,
      );
    },
    signal,
  );
};
