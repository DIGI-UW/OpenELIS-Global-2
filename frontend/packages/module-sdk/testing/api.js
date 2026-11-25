/**
 * Double for `openelis_core/api`: every call answers an empty success and is
 * recorded on `apiCalls`, so a test can assert what a page asked for. Reassign
 * a function on `apiStubs` (or `vi.spyOn(apiStubs, "getFromOpenElisServer")`)
 * to script an answer; `resetApiStubs()` restores the defaults.
 */
export const apiCalls = [];

const record = (name, args) => {
  apiCalls.push({ name, args });
};

const defaults = () => ({
  getFromOpenElisServer: (endPoint, callback) => {
    record("getFromOpenElisServer", [endPoint]);
    if (callback) callback({});
  },
  fetchFromOpenElisServer: async (endPoint) => {
    record("fetchFromOpenElisServer", [endPoint]);
    return {};
  },
  postToOpenElisServer: (endPoint, payLoad, callback, extraParams) => {
    record("postToOpenElisServer", [endPoint, payLoad]);
    if (callback) callback(200, extraParams);
  },
  postToOpenElisServerJsonResponse: (
    endPoint,
    payLoad,
    callback,
    extraParams,
  ) => {
    record("postToOpenElisServerJsonResponse", [endPoint, payLoad]);
    if (callback) callback({}, extraParams);
  },
  postToOpenElisServerFullResponse: (
    endPoint,
    payLoad,
    callback,
    extraParams,
  ) => {
    record("postToOpenElisServerFullResponse", [endPoint, payLoad]);
    if (callback) callback({ status: 200, ok: true }, extraParams);
  },
  putToOpenElisServer: (endPoint, payLoad, callback) => {
    record("putToOpenElisServer", [endPoint, payLoad]);
    if (callback) callback(200);
  },
  putToOpenElisServerJsonResponse: (
    endPoint,
    payLoad,
    callback,
    extraParams,
  ) => {
    record("putToOpenElisServerJsonResponse", [endPoint, payLoad]);
    if (callback) callback({}, extraParams);
  },
  deleteFromOpenElisServer: (endPoint, callback) => {
    record("deleteFromOpenElisServer", [endPoint]);
    if (callback) callback(200);
  },
  hasRole: () => true,
  hasPermission: () => true,
  hasPermissionOrGlobalAdmin: () => true,
  fetchFromServer: async (endPoint) => {
    record("fetchFromServer", [endPoint]);
    return {};
  },
  serverQuery: (key, endPoint) => ({
    queryKey: key,
    queryFn: async () => {
      record("serverQuery", [endPoint]);
      return {};
    },
  }),
  useServerData: () => ({
    data: undefined,
    isLoading: false,
    isFetching: false,
    error: null,
    refetch: async () => {},
  }),
  useInvalidateServerData: () => async () => {},
});

export const apiStubs = defaults();

export const resetApiStubs = () => {
  apiCalls.length = 0;
  Object.assign(apiStubs, defaults());
};

export const getFromOpenElisServer = (...args) =>
  apiStubs.getFromOpenElisServer(...args);
export const fetchFromOpenElisServer = (...args) =>
  apiStubs.fetchFromOpenElisServer(...args);
export const postToOpenElisServer = (...args) =>
  apiStubs.postToOpenElisServer(...args);
export const postToOpenElisServerJsonResponse = (...args) =>
  apiStubs.postToOpenElisServerJsonResponse(...args);
export const postToOpenElisServerFullResponse = (...args) =>
  apiStubs.postToOpenElisServerFullResponse(...args);
export const putToOpenElisServer = (...args) =>
  apiStubs.putToOpenElisServer(...args);
export const putToOpenElisServerJsonResponse = (...args) =>
  apiStubs.putToOpenElisServerJsonResponse(...args);
export const deleteFromOpenElisServer = (...args) =>
  apiStubs.deleteFromOpenElisServer(...args);
export const hasRole = (...args) => apiStubs.hasRole(...args);
export const hasPermission = (...args) => apiStubs.hasPermission(...args);
export const hasPermissionOrGlobalAdmin = (...args) =>
  apiStubs.hasPermissionOrGlobalAdmin(...args);
export const fetchFromServer = (...args) => apiStubs.fetchFromServer(...args);
export const serverQuery = (...args) => apiStubs.serverQuery(...args);
export const useServerData = (...args) => apiStubs.useServerData(...args);
export const useInvalidateServerData = (...args) =>
  apiStubs.useInvalidateServerData(...args);

export const Roles = Object.freeze({
  GLOBAL_ADMIN: "Global Administrator",
  USER_ACCOUNT_ADMIN: "User Account Administrator",
  AUDIT_TRAIL: "Audit Trail",
  ANALYSER_IMPORT: "Analyser Import",
  CYTOPATHOLOGIST: "Cytopathologist",
  PATHOLOGIST: "Pathologist",
  RECEPTION: "Reception",
  RESULTS: "Results",
  VALIDATION: "Validation",
  REPORTS: "Reports",
});

export const config = {
  title: "OpenELIS",
  loginRedirect: "/login",
  serverBaseUrl: "/api/OpenELIS-Global",
};
