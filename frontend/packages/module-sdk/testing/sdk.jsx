/**
 * Doubles for `openelis_core/sdk`. The API object answers from `apiResponses`
 * (an endpoint to a value or a function of the endpoint and body) and records
 * every call on `apiCalls`; an unscripted GET, POST or PUT answers {} (what
 * the shell resolves for a 2xx with no body), a DELETE answers 204. A refusal
 * is scripted as the shell reports it, `{ statusCode, error, fieldErrors }`.
 * Permissions are granted unless the key is in `deniedPermissions`.
 * `resetSdkStubs()` clears everything between tests.
 */
import React, { useContext, useMemo } from "react";
import {
  ConfigurationContext,
  NotificationContext,
  UserSessionDetailsContext,
  testSession,
} from "./contexts.js";

export const FRONTEND_CONTRACT_VERSION = "1.0.0";

export const apiCalls = [];
export const apiResponses = {};
export const deniedPermissions = new Set();
export const moduleConfigs = {};

export const resetSdkStubs = () => {
  apiCalls.length = 0;
  for (const key of Object.keys(apiResponses)) delete apiResponses[key];
  for (const key of Object.keys(moduleConfigs)) delete moduleConfigs[key];
  deniedPermissions.clear();
};

const answer = (method, endpoint, body) => {
  apiCalls.push({ method, endpoint, body });
  const scripted = apiResponses[endpoint];
  if (typeof scripted === "function") return scripted(endpoint, body, method);
  if (scripted !== undefined) return scripted;
  return method === "DELETE" ? 204 : {};
};

const api = {
  get: async (endpoint) => answer("GET", endpoint),
  post: async (endpoint, body) => answer("POST", endpoint, body),
  put: async (endpoint, body) => answer("PUT", endpoint, body),
  del: async (endpoint) => answer("DELETE", endpoint),
};

export const ExtensionSlot = () => null;

export const ModuleErrorBoundary = ({ children }) => <>{children}</>;

export const useSession = () => {
  const value = useContext(UserSessionDetailsContext);
  return value && value.userSessionDetails
    ? value
    : { userSessionDetails: testSession };
};

export const usePermission = (key) => !deniedPermissions.has(key);

export const useRole = (role) => {
  const { userSessionDetails } = useSession();
  return (userSessionDetails.roles || []).includes(role);
};

export const useConfigurationProperties = () => {
  const value = useContext(ConfigurationContext);
  return (value && value.configurationProperties) || {};
};

export const useNotifications = () => useContext(NotificationContext);

export const useOpenElisApi = () => useMemo(() => api, []);

export const useModuleConfig = (moduleId) => moduleConfigs[moduleId] || {};

export const useModule = () => undefined;
