/**
 * The module SDK (`openelis_core/sdk`): the hooks a module page needs most,
 * backed by the shell's own contexts and client, so a module never bundles a
 * copy of any of it. `FRONTEND_CONTRACT_VERSION` is what the registry
 * publishes and what a module's `requires.frontendContract` is checked against.
 */
import { useCallback, useContext, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import UserSessionDetailsContext from "../UserSessionDetailsContext";
import {
  ConfigurationContext,
  NotificationContext,
} from "../components/layout/contexts";
import {
  deleteFromOpenElisServer,
  fetchFromOpenElisServer,
  hasPermission,
  hasRole,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../components/utils/Utils";
import { useModuleHost } from "../modules/ModuleHostProvider";

export { FRONTEND_CONTRACT_VERSION } from "../modules/moduleRegistry";
export { default as ExtensionSlot } from "../modules/ExtensionSlot";
export { default as ModuleErrorBoundary } from "../modules/ModuleErrorBoundary";

/** The signed-in user: `userSessionDetails` (roles, permissions, user info) plus `logout` and `refresh`. */
export function useSession() {
  return useContext(UserSessionDetailsContext);
}

/** Whether the signed-in user holds a permission key (`mod.<id>.<key>` or a core key). */
export function usePermission(key) {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  return hasPermission(userSessionDetails, key);
}

/** Whether the signed-in user holds a core role name. */
export function useRole(role) {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  return hasRole(userSessionDetails, role);
}

/** The core configuration properties the shell already fetched. */
export function useConfigurationProperties() {
  const context = useContext(ConfigurationContext);
  return context ? context.configurationProperties : {};
}

/** The shell's toast notifications: `addNotification({ kind, title, message })`. */
export function useNotifications() {
  return useContext(NotificationContext);
}

/**
 * A promise-based client on the core request utilities. `get` rejects on a
 * non-2xx answer; `post`, `put` and `del` resolve with the parsed body (or the
 * status for `del`) and carry `statusCode` when the server refused.
 */
export function useOpenElisApi() {
  const get = useCallback(
    (endpoint, signal) => fetchFromOpenElisServer(endpoint, signal),
    [],
  );
  const post = useCallback(
    (endpoint, body) =>
      new Promise((resolve) =>
        postToOpenElisServerJsonResponse(
          endpoint,
          JSON.stringify(body),
          (response) => resolve(response),
        ),
      ),
    [],
  );
  const put = useCallback(
    (endpoint, body) =>
      new Promise((resolve) =>
        putToOpenElisServerJsonResponse(
          endpoint,
          JSON.stringify(body),
          (response) => resolve(response),
        ),
      ),
    [],
  );
  const del = useCallback(
    (endpoint) =>
      new Promise((resolve) =>
        deleteFromOpenElisServer(endpoint, (status) => resolve(status)),
      ),
    [],
  );
  return useMemo(() => ({ get, post, put, del }), [get, post, put, del]);
}

/** The configuration values a module's manifest exposes to its frontend. */
export function useModuleConfig(moduleId) {
  const query = useQuery({
    queryKey: ["moduleConfig", moduleId],
    queryFn: () =>
      fetchFromOpenElisServer(`/rest/module-registry/${moduleId}/config`),
    enabled: Boolean(moduleId),
    staleTime: Infinity,
  });
  return query.data || {};
}

/** The registry entry of a started module, or undefined. */
export function useModule(moduleId) {
  const host = useModuleHost();
  return host.modules.find((module) => module.id === moduleId);
}
