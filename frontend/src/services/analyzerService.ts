/**
 * Analyzer Service API Client
 *
 * Provides methods for CRUD operations on analyzers and analyzer field mappings
 * Follows OpenELIS pattern using getFromOpenElisServer, postToOpenElisServerJsonResponse, and fetch for PUT/DELETE
 *
 * Pattern Reference: AGENTS.md Section 5 (Frontend Data Fetching Pattern)
 */

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../components/utils/Utils";
import type {
  Analyzer,
  AnalyzerApiError,
  AnalyzerApiResponse,
  AnalyzerProtocol,
} from "../components/analyzers/types";
import config from "../config.json";

type ExtraParams = unknown;
type JsonObject = Record<string, unknown>;
type ApiCallback<T = AnalyzerApiResponse> = (
  response: T | undefined,
  extraParams?: ExtraParams,
) => void;
type DataCallback<T> = (data: T) => void;
type SuccessCallback = (
  success: boolean,
  error: AnalyzerApiError | null,
) => void;

const asExtraParamsObject = (extraParams?: ExtraParams): JsonObject =>
  typeof extraParams === "object" && extraParams !== null
    ? (extraParams as JsonObject)
    : {};

export interface AnalyzerFilters {
  status?: string;
  search?: string;
  testUnit?: string;
  analyzerType?: string;
}

export interface AnalyzersResponse {
  analyzers?: Analyzer[];
}

export interface AnalyzerTypeSummary {
  profileId: string;
  revision: number;
  revisionFingerprint: string;
  displayName: string;
  manufacturer?: string | null;
  model?: string | null;
  source: "SHIPPED" | "SITE" | string;
  status: "ACTIVE" | "INACTIVE" | string;
  protocol: AnalyzerProtocol;
  parentProfileId?: string | null;
  parentRevision?: number | null;
  affectedAnalyzers?: Array<{
    id: string;
    name: string;
    active: boolean;
  }>;
}

export interface AnalyzerTypeCatalog {
  schemaVersion: string;
  catalogFingerprint: string;
  summary: {
    total: number;
    inUse: number;
    needsAttention: number;
    deactivated: number;
  };
  types: AnalyzerTypeSummary[];
}

export interface AnalyzerLabUnit {
  id: string;
  name: string;
}

export type AnalyzerMappingState = "BOUND" | "EXCLUDED" | "UNRESOLVED";

export interface AnalyzerMappingTestOption {
  id: string;
  name: string;
  code?: string | null;
  loincCodes: string[];
}

export interface AnalyzerMappingResultOption {
  id: string;
  value: string;
  label: string;
}

export interface AnalyzerTypeMappingResultRow {
  rawValue: string;
  mappingState: AnalyzerMappingState;
  resultOptionId?: string | null;
  selectedOption?: AnalyzerMappingResultOption | null;
}

export interface AnalyzerTypeMappingTestRow {
  sourceRowKey: string;
  rawCode: string;
  aliases: string[];
  testNameHint?: string | null;
  loinc?: string | null;
  unit?: string | null;
  resultType?: string | null;
  normalizedCoding?: {
    system: string;
    code: string;
    display?: string | null;
  } | null;
  mappingState: AnalyzerMappingState;
  testId?: string | null;
  selectedTest?: AnalyzerMappingTestOption | null;
  suggestedTest?: AnalyzerMappingTestOption | null;
  results: AnalyzerTypeMappingResultRow[];
}

export interface AnalyzerTypeMappingView {
  profileId: string;
  profileRevision: number;
  profileFingerprint: string;
  displayName: string;
  protocol: AnalyzerProtocol;
  siteBindingId?: string | null;
  siteBindingRevision: number;
  bindingFingerprint?: string | null;
  tests: AnalyzerTypeMappingTestRow[];
  controlRecognition: {
    recognitionFingerprint: string;
    mode: "RULES" | "NONE" | string;
    description: string;
    affirmedNoControlResults: boolean;
    conditions: Array<{
      key: string;
      kind: string;
      sourceLabel: string;
      value?: string | null;
      description: string;
      controlLevel?: string | null;
      controlType?: string | null;
    }>;
  };
  confirmation: {
    state: "UNCONFIRMED" | "CURRENT" | "STALE" | string;
    profileId?: string | null;
    profileRevision: number;
    bindingFingerprint?: string | null;
    recognitionFingerprint?: string | null;
    confirmedBy?: string | null;
    confirmedByDisplayName?: string | null;
    confirmedAt?: string | null;
    confirmedRows: Array<{ sourceRowKey: string; rawValue?: string | null }>;
    excludedRows: Array<{ sourceRowKey: string; rawValue?: string | null }>;
  };
}

export interface AnalyzerTypeMappingUpdate {
  baseBindingFingerprint?: string | null;
  tests: Array<{
    sourceRowKey: string;
    mappingState: AnalyzerMappingState;
    testId?: string | null;
  }>;
  results: Array<{
    sourceRowKey: string;
    rawValue: string;
    mappingState: AnalyzerMappingState;
    testResultId?: string | null;
  }>;
}

export interface AnalyzerTypeMappingConfirmationRequest {
  baseBindingFingerprint: string;
  recognitionFingerprint: string;
  confirmedRows: Array<{ sourceRowKey: string; rawValue?: string | null }>;
  excludedRows: Array<{ sourceRowKey: string; rawValue?: string | null }>;
}

export interface AnalyzerProfileDraftResponse extends AnalyzerApiError {
  draftId?: string;
  kind?: "CREATE" | "DUPLICATE" | "UPDATE" | string;
  baseProfileId?: string | null;
  baseRevision?: number | null;
  profile?: {
    profileMeta?: {
      id?: string;
      displayName?: string;
    };
    catalog?: {
      revision?: number;
      source?: string;
      status?: string;
    };
  };
  validationIssues?: string[];
}

export interface AnalyzerControlRecognitionCondition {
  key?: string | null;
  kind: string;
  sourceKey?: string | null;
  sourceLabel?: string | null;
  description?: string | null;
  value?: string | null;
  editable?: boolean;
  controlLevel?: string | null;
  controlType?: string | null;
}

export interface AnalyzerControlRecognitionDraft extends AnalyzerApiError {
  draftId?: string;
  kind?: "CREATE" | "DUPLICATE" | "UPDATE" | string;
  baseProfileId?: string | null;
  baseRevision?: number | null;
  displayName?: string;
  updatedBy?: string;
  updatedAt?: string;
  validationIssues?: string[];
  recognition?: {
    mode?: "RULES" | "NONE" | string | null;
    affirmedNoControlResults: boolean;
    description?: string;
    conditions: AnalyzerControlRecognitionCondition[];
    availableSources: Array<{ key: string; label: string }>;
  };
}

export interface AnalyzerControlRecognitionUpdate {
  mode: "RULES" | "NONE" | string;
  affirmedNoControlResults: boolean;
  conditions: Array<{
    key?: string | null;
    kind: string;
    sourceKey?: string | null;
    value?: string | null;
    controlLevel?: string | null;
    controlType?: string | null;
  }>;
}

/**
 * Get all analyzers with optional filters
 * @param {Object} filters - Optional filters { status, search }
 * @param {Function} callback - Callback function (data) => void
 * @param {AbortSignal|null} signal - Optional AbortSignal to cancel on unmount
 */
export const getAnalyzers = (
  filters: AnalyzerFilters = {},
  callback: DataCallback<AnalyzersResponse | undefined>,
  signal: AbortSignal | null = null,
) => {
  let endpoint = "/rest/analyzer/analyzers";
  const params = new URLSearchParams();

  if (filters) {
    if (filters.status) {
      params.append("status", filters.status);
    }
    if (filters.search) {
      params.append("search", filters.search);
    }
  }

  if (params.toString()) {
    endpoint += "?" + params.toString();
  }

  getFromOpenElisServer(endpoint, callback, signal);
};

/**
 * Get analyzer by ID
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (data) => void
 */
export const getAnalyzer = (
  id: string,
  callback: DataCallback<Analyzer | undefined>,
) => {
  const endpoint = `/rest/analyzer/analyzers/${id}`;
  getFromOpenElisServer(endpoint, callback);
};

export const getAnalyzerLabUnits = (
  callback: DataCallback<AnalyzerLabUnit[]>,
  signal: AbortSignal | null = null,
) => {
  getFromOpenElisServer<AnalyzerLabUnit[]>(
    "/rest/test-catalog/lab-units",
    (response) => callback(response ?? []),
    signal,
  );
};

/**
 * Create new analyzer
 * @param {Object} analyzerData - Analyzer data { name, analyzerType, ipAddress, port, testUnitIds, active }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createAnalyzer = (
  analyzerData: Partial<Analyzer> & JsonObject,
  callback: ApiCallback,
  extraParams?: ExtraParams,
) => {
  const endpoint = "/rest/analyzer/analyzers";
  const payload = JSON.stringify(analyzerData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

/**
 * Update analyzer
 * @param {String} id - Analyzer ID
 * @param {Object} analyzerData - Analyzer data to update
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const updateAnalyzer = (
  id: string,
  analyzerData: Partial<Analyzer> & JsonObject,
  callback: ApiCallback,
  extraParams?: ExtraParams,
) => {
  const endpoint = `/rest/analyzer/analyzers/${id}`;
  const payload = JSON.stringify(analyzerData);

  // Use fetch directly to get JSON response (controllers return Map<String, Object>)
  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF") || "",
    },
    body: payload,
  })
    .then(async (response) => {
      if (!response.ok) {
        // For error responses, try to parse JSON error message
        const errorJson = await response.json().catch(() => ({
          error: `HTTP ${response.status}: ${response.statusText}`,
        }));
        callback(
          {
            ...errorJson,
            status: response.status,
            statusCode: response.status,
            statusText: response.statusText,
          },
          extraParams,
        );
        return;
      }
      // For successful responses, parse JSON normally
      const json = await response.json();
      callback(json, extraParams);
    })
    .catch((error: Error) => {
      callback(
        {
          error: error.message || "Network error",
          message: error.message || "Network error",
          status: 0,
        },
        extraParams,
      );
    });
};

/**
 * Delete analyzer (soft delete - sets active=false)
 *
 * Note: Uses POST /delete endpoint instead of DELETE HTTP method due to Spring
 * Security 6 CSRF protection issues with DELETE requests.
 *
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (success, error) => void
 */
export const deleteAnalyzer = (id: string, callback: SuccessCallback) => {
  const endpoint = `/rest/analyzer/analyzers/${id}/delete`;
  const csrfToken = localStorage.getItem("CSRF");

  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": csrfToken || "",
    },
  })
    .then(async (response) => {
      // Read response body if present
      let responseData = null;
      try {
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
          responseData = await response.json();
        } else if (response.status !== 204) {
          await response.text();
        }
      } catch {
        // Response body could not be parsed
      }

      if (response.ok || response.status === 204 || response.status === 200) {
        callback(true, null);
      } else {
        // Parse error response
        let errorData;
        try {
          const contentType = response.headers.get("content-type");
          if (contentType && contentType.indexOf("application/json") !== -1) {
            errorData = responseData || {
              error: `HTTP ${response.status}: ${response.statusText}`,
              status: response.status,
              statusText: response.statusText,
            };
          } else {
            errorData = {
              error: `HTTP ${response.status}: ${response.statusText}`,
              status: response.status,
              statusText: response.statusText,
            };
          }
        } catch {
          errorData = {
            error: `HTTP ${response.status}: ${response.statusText}`,
            status: response.status,
            statusText: response.statusText,
          };
        }
        callback(false, errorData);
      }
    })
    .catch((error: Error) => {
      callback(false, { error: error.message || "Network error" });
    });
};

/**
 * Test TCP connection to analyzer
 * @param {String} id - Analyzer ID
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const testConnection = (
  id: string,
  callback: ApiCallback,
  extraParams?: ExtraParams,
) => {
  const endpoint = `/rest/analyzer/analyzers/${id}/test-connection`;
  // POST with empty body
  postToOpenElisServerJsonResponse(
    endpoint,
    JSON.stringify({}),
    callback,
    extraParams,
  );
};

/**
 * Create a new OpenELIS field
 * @param {Object} fieldData - Field data { fieldName, entityType, loincCode, description, fieldType, acceptedUnits }
 * @param {Function} callback - Callback function (response, extraParams) => void
 * @param {*} extraParams - Optional extra parameters passed to callback
 */
export const createField = (
  fieldData: JsonObject,
  callback: ApiCallback,
  extraParams?: ExtraParams,
) => {
  const endpoint = "/rest/analyzer/openelis-fields";
  const payload = JSON.stringify(fieldData);
  postToOpenElisServerJsonResponse(endpoint, payload, callback, extraParams);
};

export const getAnalyzerTypeCatalog = (
  callback: DataCallback<AnalyzerTypeCatalog | undefined>,
  signal: AbortSignal | null = null,
) => {
  getFromOpenElisServer("/rest/analyzer-types", callback, signal);
};

export const getAnalyzerTypeRevision = (
  profileId: string,
  revision: number,
  callback: DataCallback<AnalyzerTypeSummary | undefined>,
) => {
  getFromOpenElisServer(
    `/rest/analyzer-types/${encodeURIComponent(profileId)}?revision=${revision}`,
    callback,
  );
};

export const getAnalyzerTypeMapping = (
  profileId: string,
  revision: number,
  callback: DataCallback<AnalyzerTypeMappingView | undefined>,
) => {
  getFromOpenElisServer(
    `/rest/analyzer-types/${encodeURIComponent(profileId)}/mapping?revision=${revision}`,
    callback,
  );
};

export const getAnalyzerMappingTests = (
  callback: DataCallback<AnalyzerMappingTestOption[] | undefined>,
) => {
  getFromOpenElisServer("/rest/analyzer-types/mapping-catalog/tests", callback);
};

export const getAnalyzerMappingResultOptions = (
  testId: string,
  callback: DataCallback<AnalyzerMappingResultOption[] | undefined>,
) => {
  getFromOpenElisServer(
    `/rest/analyzer-types/mapping-catalog/tests/${encodeURIComponent(testId)}/result-options`,
    callback,
  );
};

const mutateAnalyzerType = <T>(
  endpoint: string,
  method: "POST" | "PUT",
  body: JsonObject,
  callback: ApiCallback<T & AnalyzerApiError>,
) => {
  fetch(config.serverBaseUrl + endpoint, {
    credentials: "include",
    method,
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF") || "",
    },
    body: JSON.stringify(body),
  })
    .then(async (response) => {
      const json = await response.json().catch(() => ({}));
      if (!response.ok) {
        callback({
          ...json,
          status: response.status,
          statusCode: response.status,
          statusText: response.statusText,
        });
        return;
      }
      callback(json);
    })
    .catch((error: Error) => {
      callback({
        error: error.message || "Network error",
        message: error.message || "Network error",
        status: 0,
      } as T & AnalyzerApiError);
    });
};

export const saveAnalyzerTypeMapping = (
  profileId: string,
  revision: number,
  update: AnalyzerTypeMappingUpdate,
  callback: ApiCallback<AnalyzerTypeMappingView & AnalyzerApiError>,
) => {
  mutateAnalyzerType(
    `/rest/analyzer-types/${encodeURIComponent(profileId)}/mapping?revision=${revision}`,
    "PUT",
    update as unknown as JsonObject,
    callback,
  );
};

export const confirmAnalyzerTypeMapping = (
  profileId: string,
  revision: number,
  request: AnalyzerTypeMappingConfirmationRequest,
  callback: ApiCallback<
    AnalyzerTypeMappingView["confirmation"] & AnalyzerApiError
  >,
) => {
  mutateAnalyzerType(
    `/rest/analyzer-types/${encodeURIComponent(profileId)}/mapping/confirm?revision=${revision}`,
    "POST",
    request as unknown as JsonObject,
    callback,
  );
};

export const createAnalyzerTypeDraft = (
  displayName: string,
  callback: ApiCallback<AnalyzerProfileDraftResponse>,
) => {
  postToOpenElisServerJsonResponse(
    "/rest/analyzer-types/drafts",
    JSON.stringify({ displayName }),
    callback,
  );
};

export const getAnalyzerTypeDraft = (
  draftId: string,
  callback: DataCallback<AnalyzerProfileDraftResponse | undefined>,
) => {
  getFromOpenElisServer(
    `/rest/analyzer-types/drafts/${encodeURIComponent(draftId)}`,
    callback,
  );
};

export const getAnalyzerTypeControlRecognition = (
  draftId: string,
  callback: DataCallback<AnalyzerControlRecognitionDraft | undefined>,
) => {
  getFromOpenElisServer(
    `/rest/analyzer-types/drafts/${encodeURIComponent(draftId)}/control-recognition`,
    callback,
  );
};

export const updateAnalyzerTypeControlRecognition = (
  draftId: string,
  update: AnalyzerControlRecognitionUpdate,
  callback: ApiCallback<AnalyzerControlRecognitionDraft>,
) => {
  mutateAnalyzerType(
    `/rest/analyzer-types/drafts/${encodeURIComponent(draftId)}/control-recognition`,
    "PUT",
    update as unknown as JsonObject,
    callback,
  );
};

export const duplicateAnalyzerType = (
  profileId: string,
  sourceRevision: number,
  displayName: string,
  callback: ApiCallback<AnalyzerProfileDraftResponse>,
) => {
  postToOpenElisServerJsonResponse(
    `/rest/analyzer-types/${encodeURIComponent(profileId)}/duplicate`,
    JSON.stringify({ sourceRevision, displayName }),
    callback,
  );
};

export const updateSharedAnalyzerType = (
  profileId: string,
  sourceRevision: number,
  callback: ApiCallback<AnalyzerProfileDraftResponse>,
) => {
  postToOpenElisServerJsonResponse(
    `/rest/analyzer-types/${encodeURIComponent(profileId)}/update`,
    JSON.stringify({ sourceRevision }),
    callback,
  );
};

export const publishAnalyzerTypeDraft = (
  draftId: string,
  callback: ApiCallback<AnalyzerProfileDraftResponse>,
) => {
  postToOpenElisServerJsonResponse(
    `/rest/analyzer-types/drafts/${encodeURIComponent(draftId)}/publish`,
    "{}",
    callback,
  );
};
