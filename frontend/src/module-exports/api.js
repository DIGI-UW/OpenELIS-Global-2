/**
 * The core API client as modules see it (`openelis_core/api`). Every function
 * sends the session cookie, the locale and, for writes, the CSRF token, exactly
 * as the core pages do. Additions here are frontend-contract minors; removals
 * and signature changes are majors.
 */
export {
  getFromOpenElisServer,
  fetchFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
  postToOpenElisServerFullResponse,
  putToOpenElisServer,
  putToOpenElisServerJsonResponse,
  deleteFromOpenElisServer,
  hasRole,
  hasPermission,
  hasPermissionOrGlobalAdmin,
  Roles,
} from "../components/utils/Utils";
export { serverQuery, fetchFromServer } from "../components/utils/queryClient";
export {
  useServerData,
  useInvalidateServerData,
} from "../components/utils/useServerData";
export { default as config } from "../config.json";
