import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { requestFailed, serverMessage } from "../../utils/requestOutcome";

/**
 * OGC-1363: the Locations & Organizations endpoints, as promises. A refusal
 * (422 with field errors, 409 with the stored record, 404) rejects with an
 * error carrying `status`, `fieldErrors` and `current`, so a form can show
 * each message inline and a stale save can offer the other admin's values.
 */
const BASE = "/rest/locations";

export class LocationsError extends Error {
  constructor(response) {
    super(
      (response && (serverMessage(response) || response.error)) ||
        "The request failed",
    );
    this.status =
      response && typeof response.status === "number" ? response.status : 0;
    this.fieldErrors = (response && response.fieldErrors) || {};
    this.current = response ? response.current : undefined;
  }
}

const settle = (resolve, reject) => (response) => {
  if (requestFailed(response)) {
    reject(new LocationsError(response));
    return;
  }
  resolve(response);
};

export const getJson = (url) =>
  new Promise((resolve, reject) => {
    getFromOpenElisServer(url, (body) => {
      if (body === undefined || body === null) {
        reject(new LocationsError({ status: 0 }));
        return;
      }
      settle(resolve, reject)(body);
    });
  });

export const postJson = (url, body) =>
  new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      url,
      JSON.stringify(body),
      settle(resolve, reject),
    );
  });

export const putJson = (url, body) =>
  new Promise((resolve, reject) => {
    putToOpenElisServerJsonResponse(
      url,
      JSON.stringify(body),
      settle(resolve, reject),
    );
  });

const query = (params) => {
  const out = new URLSearchParams();
  Object.keys(params || {}).forEach((key) => {
    const value = params[key];
    if (Array.isArray(value)) {
      value.filter(Boolean).forEach((item) => out.append(key, item));
    } else if (value !== undefined && value !== null && String(value) !== "") {
      out.set(key, String(value));
    }
  });
  const text = out.toString();
  return text ? `?${text}` : "";
};

export const listOrganizations = (params) =>
  getJson(`${BASE}/organizations${query(params)}`);

export const getOrganization = (id) =>
  getJson(`${BASE}/organizations/${encodeURIComponent(id)}`);

export const createOrganization = (body) =>
  postJson(`${BASE}/organizations`, body);

export const updateOrganization = (id, body) =>
  putJson(`${BASE}/organizations/${encodeURIComponent(id)}`, body);

export const getUsage = (id) =>
  getJson(`${BASE}/organizations/${encodeURIComponent(id)}/usage`);

export const setActive = (ids, active, includeChildren = false) =>
  postJson(`${BASE}/organizations/active`, { ids, active, includeChildren });

export const listWards = (id, includeInactive) =>
  getJson(
    `${BASE}/organizations/${encodeURIComponent(id)}/wards${query({ includeInactive })}`,
  );

export const createWard = (organizationId, body) =>
  postJson(
    `${BASE}/organizations/${encodeURIComponent(organizationId)}/wards`,
    body,
  );

export const updateWard = (organizationId, wardId, body) =>
  putJson(
    `${BASE}/organizations/${encodeURIComponent(organizationId)}/wards/${encodeURIComponent(wardId)}`,
    body,
  );

export const moveWard = (wardId, parentId) =>
  postJson(`${BASE}/wards/${encodeURIComponent(wardId)}/move`, { parentId });

export const getHistory = (id) =>
  getJson(`${BASE}/organizations/${encodeURIComponent(id)}/history`);

export const getLists = () => getJson(`${BASE}/lists`);

export const getAreaLevels = () => getJson(`${BASE}/areas/levels`);

export const listAreas = (parentId, status) =>
  getJson(`${BASE}/areas${query({ parentId, status })}`);

export const searchAreas = (q, status) =>
  getJson(`${BASE}/areas${query({ q, status })}`);

export const createArea = (body) => postJson(`${BASE}/areas`, body);

export const updateArea = (id, body) =>
  putJson(`${BASE}/areas/${encodeURIComponent(id)}`, body);

export const setAreaActive = (id, active) =>
  postJson(`${BASE}/areas/${encodeURIComponent(id)}/active`, { active });

/** The area path as one line, top-down. */
export const pathText = (location) =>
  location && Array.isArray(location.path) ? location.path.join(" / ") : "";
