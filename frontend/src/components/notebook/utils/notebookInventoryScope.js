import { getFromOpenElisServer } from "../../utils/Utils";

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

export const loadNotebookScopedInventory = (
  notebookId,
  endpoint,
  callback,
  signal = null,
) => {
  if (!notebookId) {
    callback([]);
    return;
  }

  getFromOpenElisServer(
    `/rest/notebook/${notebookId}/departments`,
    (departments, departmentError) => {
      if (
        departmentError ||
        !Array.isArray(departments) ||
        departments.length === 0
      ) {
        callback([], departmentError);
        return;
      }

      const departmentIds = departments
        .map((department) => department?.id)
        .filter(Boolean);

      if (departmentIds.length === 0) {
        callback([]);
        return;
      }

      getFromOpenElisServer(
        appendDepartmentScope(endpoint, departmentIds),
        callback,
        signal,
      );
    },
    signal,
  );
};
