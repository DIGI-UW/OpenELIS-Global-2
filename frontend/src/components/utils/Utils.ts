import config from "../../config.json";
import type { IntlShape } from "react-intl";

// This utility is the compatibility boundary for hundreds of legacy JavaScript
// callers whose API response contracts have not yet been migrated.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type LegacyApiResponse = any;

export type RequestPayload = BodyInit | Record<string, unknown> | null;

interface ApiFieldError {
  field?: string;
  defaultMessage?: string;
}

export interface ApiMessagePayload {
  messageKey?: string;
  errorKey?: string;
  messageArgs?: Record<string, unknown>;
  errorArgs?: Record<string, unknown>;
  message?: string;
  error?: string;
  fieldErrors?: ApiFieldError[];
  [key: string]: unknown;
}

interface UserSessionDetails {
  roles?: string[];
}

const csrfToken = (): string => localStorage.getItem("CSRF") as string;

/**
 * Get the current locale from localStorage for API requests.
 * Falls back to browser language or 'en' if not set.
 */
const getAcceptLanguageHeader = (): string => {
  return localStorage.getItem("locale") || navigator.language || "en";
};

/**
 * Resolve an API error/success payload to user-facing text. Generalised from
 * the original analyzer-specific helper so any feature that POSTs JSON and
 * needs to surface a backend error message can use the same logic. Recognises
 * (in order): `messageKey`/`errorKey` (+ optional `messageArgs`/`errorArgs`)
 * → React-Intl id; plain `message`/`error` string → verbatim; Spring
 * `BindingResult.fieldErrors` → joined; otherwise the supplied fallback id.
 */
export const resolveApiErrorMessage = (
  intl: IntlShape,
  payload: ApiMessagePayload | null | undefined,
  fallbackId: string,
  fallbackValues: Record<string, unknown> = {},
): string => {
  const key = payload?.messageKey || payload?.errorKey;
  const keyArgs = payload?.messageArgs || payload?.errorArgs || {};
  if (key) {
    return String(intl.formatMessage({ id: key }, keyArgs));
  }
  const text =
    typeof payload?.message === "string"
      ? payload.message
      : typeof payload?.error === "string"
        ? payload.error
        : null;
  if (text) {
    return text;
  }
  if (Array.isArray(payload?.fieldErrors) && payload.fieldErrors.length > 0) {
    return payload.fieldErrors
      .map((fe) =>
        fe.field
          ? `${fe.field}: ${fe.defaultMessage || ""}`
          : fe.defaultMessage,
      )
      .filter(Boolean)
      .join("; ");
  }
  return String(intl.formatMessage({ id: fallbackId }, fallbackValues));
};

const handleSessionError = (response: Response): Response => {
  if (response.status === 403) {
    response
      .clone()
      .json()
      .then((body: ApiMessagePayload) => {
        if (body && body.message && body.message.includes("CSRF")) {
          alert(
            "Your session has expired. The page will reload so you can continue.",
          );
          window.location.reload();
        }
      })
      .catch(() => undefined);
  }
  return response;
};

export const getFromOpenElisServer = <T = LegacyApiResponse>(
  endPoint: string,
  callback: (response: T | undefined) => void,
  signal: AbortSignal | null = null,
): void => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "GET",
      signal: signal,
      headers: {
        "Accept-Language": getAcceptLanguageHeader(),
      },
    },
  )
    .then((response) => {
      console.debug("checking response");
      // if (response.url.includes("LoginPage")) {
      //     throw "No Login Session";
      // }
      const contentType = response.headers.get("content-type");
      if (contentType && contentType.indexOf("application/json") !== -1) {
        return response.json().then((jsonResp) => {
          callback(jsonResp as T);
        });
      } else {
        callback(undefined);
      }
    })
    .catch((error) => {
      if (error.name === "AbortError") {
        return; // Component is unmounting — don't call callback
      }
      console.error(error);
      callback(undefined);
    });
};

export const postToOpenElisServer = <TExtra = unknown>(
  endPoint: string,
  payLoad: RequestPayload,
  callback: (status: number, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrfToken(),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad as BodyInit,
    },
  )
    .then(handleSessionError)
    .then((response) => response.status)
    .then((status) => {
      callback(status, extraParams);
    })
    .catch((error) => {
      console.error(error);
      callback(0, extraParams);
    });
};

export const postToOpenElisServerFullResponse = <TExtra = unknown>(
  endPoint: string,
  payLoad: RequestPayload,
  callback: (response: Response | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrfToken(),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad as BodyInit,
    },
  )
    .then(handleSessionError)
    .then((response) => callback(response, extraParams))
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const postToOpenElisServerFormData = <TExtra = unknown>(
  endPoint: string,
  formData: FormData,
  callback: (status: number, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      credentials: "include",
      method: "POST",
      headers: {
        "X-CSRF-Token": csrfToken(),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: formData,
    },
  )
    .then(handleSessionError)
    .then((response) => response.status)
    .then((status) => {
      callback(status, extraParams);
    })
    .catch((error) => {
      console.error(error);
      callback(0, extraParams);
    });
};

export const postToOpenElisServerJsonResponse = <
  T = LegacyApiResponse,
  TExtra = unknown,
>(
  endPoint: string,
  payLoad: RequestPayload,
  callback: (response: T | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrfToken(),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad as BodyInit,
    },
  )
    .then(handleSessionError)
    .then((response) => {
      // Check if response is ok (status 200-299)
      if (!response.ok) {
        // For error responses, try to parse JSON. If the body is empty
        // (older endpoints return .build() with no payload) the parse will
        // fail — preserve the HTTP status so callers can still distinguish
        // a 409 from a network error.
        return response
          .text()
          .then((raw) => {
            const parsed = raw ? JSON.parse(raw) : {};
            return {
              ...parsed,
              status: response.status,
              statusCode: response.status,
              statusText: response.statusText,
            };
          })
          .catch(() => ({
            error: `Request failed (HTTP ${response.status} ${response.statusText || ""})`,
            message: `Request failed (HTTP ${response.status} ${response.statusText || ""})`,
            status: response.status,
            statusCode: response.status,
            statusText: response.statusText,
          }));
      }
      // For successful responses, parse JSON normally
      return response.json();
    })
    .then((json) => {
      callback(json as T, extraParams);
    })
    .catch((error) => {
      console.error("postToOpenElisServerJsonResponse error:", error);
      // Pass error to callback so calling code can handle it
      callback(
        {
          error: error.message || "Network error",
          message: error.message || "Network error",
          status: 0,
        } as T,
        extraParams,
      );
    });
};

export const postToOpenElisServerForBlob = (
  endPoint: string,
  payLoad: RequestPayload,
  callback: (blob: Blob, response: Response) => void,
  errorCallback?: (error: unknown) => void,
): void => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrfToken(),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad as BodyInit,
    },
  )
    .then(handleSessionError)
    .then((response) => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return response.blob().then((blob) => ({ blob, response }));
    })
    .then(({ blob, response }) => {
      callback(blob, response);
    })
    .catch((error) => {
      console.error(error);
      if (errorCallback) {
        errorCallback(error);
      }
    });
};

export const getFromOpenElisServerForBlob = (
  endPoint: string,
  callback: (blob: Blob, response: Response) => void,
  errorCallback?: (error: Error) => void,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    headers: {
      "Accept-Language": getAcceptLanguageHeader(),
    },
  })
    .then(handleSessionError)
    .then((response) => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      return response.blob().then((blob) => ({ blob, response }));
    })
    .then(({ blob, response }) => {
      callback(blob, response);
    })
    .catch((error) => {
      console.error(error);
      if (errorCallback) {
        errorCallback(error);
      }
    });
};

export const postToOpenElisServerForPDF = (
  endPoint: string,
  payLoad: RequestPayload,
  callback: (success: boolean, blob?: Blob) => void,
): void => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrfToken(),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad as BodyInit,
    },
  )
    .then(handleSessionError)
    .then((response) => response.blob())
    .then((blob) => {
      callback(true, blob);
      const link = document.createElement("a");
      link.href = window.URL.createObjectURL(blob);
      link.target = "_blank";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    })
    .catch((error) => {
      callback(false);
      console.error(error);
    });
};

export const putToOpenElisServer = (
  endPoint: string,
  payLoad: RequestPayload | undefined,
  callback: (status: number) => void,
): void => {
  // Build the request options
  const options: RequestInit = {
    // includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": csrfToken(),
      "Accept-Language": getAcceptLanguageHeader(),
    },
  };

  // Include the body only if payLoad is provided
  if (payLoad) {
    options.body = payLoad as BodyInit;
  }

  fetch(config.serverBaseUrl + endPoint, options)
    .then(handleSessionError)
    .then((response) => response.status)
    .then((status) => {
      callback(status);
    })
    .catch((error) => {
      console.error(error);
      callback(0);
    });
};

export const putToOpenElisServerJsonResponse = <TExtra = unknown>(
  endPoint: string,
  payLoad: RequestPayload,
  callback: (json: any, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    //includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
      "Accept-Language": getAcceptLanguageHeader(),
    },
    body: payLoad,
  })
    .then(handleSessionError)
    .then((response) => {
      if (!response.ok) {
        return response.json().then((errorJson) => ({
          ...errorJson,
          status: response.status,
          statusCode: response.status,
          statusText: response.statusText,
        }));
      }
      return response.json();
    })
    .then((json) => {
      callback(json, extraParams);
    })
    .catch((error) => {
      console.error("putToOpenElisServerJsonResponse error:", error);
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

export const putToOpenElisServerFullResponse = <TExtra = unknown>(
  endPoint: string,
  payLoad: RequestPayload,
  callback: (response: Response | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    //includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": csrfToken(),
      "Accept-Language": getAcceptLanguageHeader(),
    },
    body: payLoad as BodyInit,
  })
    .then(handleSessionError)
    .then((response) => callback(response, extraParams))
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const deleteFromOpenElisServer = (
  endPoint: string,
  callback: (status: number) => void,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    // includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": csrfToken(),
      "Accept-Language": getAcceptLanguageHeader(),
    },
  })
    .then(handleSessionError)
    .then((response) => response.status)
    .then((status) => {
      callback(status);
    })
    .catch((error) => {
      console.error(error);
      callback(0);
    });
};

export const deleteFromOpenElisServerFullResponse = <TExtra = unknown>(
  endPoint: string,
  callback: (response: Response | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    // includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": csrfToken(),
      "Accept-Language": getAcceptLanguageHeader(),
    },
  })
    .then(handleSessionError)
    .then((response) => callback(response, extraParams))
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const hasRole = (
  userSessionDetails: UserSessionDetails | null | undefined,
  role: string,
): boolean => {
  if (!userSessionDetails || !userSessionDetails.roles) {
    return false;
  }
  return userSessionDetails.roles.includes(role);
};

/**
 * Privilege name constants — mirrors Privileges.java (spec 012 T036). The
 * /session payload's `privileges` array uses these raw names; gate UI with
 * hasPrivilege() against them instead of role-name strings.
 */
export const Privileges = {
  ORDER_CREATE: "order:create",
  ORDER_VIEW: "order:view",
  ORDER_EDIT: "order:edit",
  ORDER_DELETE: "order:delete",
  PANEL_VIEW: "panel:view",
  PANEL_MANAGE: "panel:manage",
  ANALYTE_VIEW: "analyte:view",
  ANALYTE_MANAGE: "analyte:manage",
  METHOD_VIEW: "method:view",
  METHOD_MANAGE: "method:manage",
  SAMPLE_TYPE_VIEW: "sample_type:view",
  SAMPLE_TYPE_MANAGE: "sample_type:manage",
  SAMPLE_STATUS_VIEW: "sample_status:view",
  RESULT_VIEW: "result:view",
  RESULT_ENTER: "result:enter",
  RESULT_MODIFY: "result:modify",
  RESULT_VALIDATE: "result:validate",
  RESULT_PATHOLOGY_SIGN_OFF: "result:pathology-sign-off",
  RESULT_CYTOPATHOLOGY_SIGN_OFF: "result:cytopathology-sign-off",
  PATIENT_VIEW: "patient:view",
  PATIENT_CREATE: "patient:create",
  PATIENT_EDIT: "patient:edit",
  PATIENT_MANAGE: "patient:manage",
  REPORT_RUN: "report:run",
  REPORT_EXPORT: "report:export",
  NCE_VIEW: "nce:view",
  NCE_CREATE: "nce:create",
  NCE_EDIT: "nce:edit",
  NCE_ASSIGN: "nce:assign",
  ANALYZER_IMPORT: "analyzer:import",
  ANALYZER_CONFIGURE: "analyzer:configure",
  USER_MANAGE: "user:manage",
  SYSTEM_CONFIGURE: "system:configure",
  TEST_CONFIGURE: "test:configure",
  REPORT_CONFIGURE: "report:configure",
  AUDIT_VIEW: "audit:view",
  SHIPMENT_VIEW: "shipment:view",
  SHIPMENT_CREATE: "shipment:create",
  SHIPMENT_EDIT: "shipment:edit",
  SHIPMENT_MANAGE: "shipment:manage",
  SHIPMENT_DELETE: "shipment:delete",
  EQA_VIEW: "eqa:view",
  EQA_MANAGE: "eqa:manage",
  ESIG_USE: "esig:use",
  ALERT_VIEW: "alert:view",
  ALERT_MANAGE: "alert:manage",
  BARCODE_VIEW: "barcode:view",
  BARCODE_MANAGE: "barcode:manage",
  CALENDAR_VIEW: "calendar:view",
  CALENDAR_MANAGE: "calendar:manage",
  COLDSTORAGE_VIEW: "coldstorage:view",
  COLDSTORAGE_MANAGE: "coldstorage:manage",
  DICTIONARY_VIEW: "dictionary:view",
  DICTIONARY_MANAGE: "dictionary:manage",
  EXTCONNECTION_VIEW: "extconnection:view",
  EXTCONNECTION_MANAGE: "extconnection:manage",
  INVENTORY_VIEW: "inventory:view",
  INVENTORY_MANAGE: "inventory:manage",
  LOCALIZATION_VIEW: "localization:view",
  LOCALIZATION_MANAGE: "localization:manage",
  NOTEBOOK_VIEW: "notebook:view",
  NOTEBOOK_MANAGE: "notebook:manage",
  NOTIFICATION_VIEW: "notification:view",
  NOTIFICATION_MANAGE: "notification:manage",
  ORGANIZATION_VIEW: "organization:view",
  ORGANIZATION_MANAGE: "organization:manage",
  PROGRAM_VIEW: "program:view",
  PROGRAM_MANAGE: "program:manage",
  BRANDING_VIEW: "branding:view",
  BRANDING_MANAGE: "branding:manage",
  PROVIDER_VIEW: "provider:view",
  PROVIDER_MANAGE: "provider:manage",
  SITE_INFO_VIEW: "site_info:view",
  REFERRAL_VIEW: "referral:view",
  REFERRAL_MANAGE: "referral:manage",
  STORAGE_VIEW: "storage:view",
  STORAGE_MANAGE: "storage:manage",
  TESTCALC_VIEW: "testcalc:view",
  TESTCALC_MANAGE: "testcalc:manage",
  ROLE_VIEW: "role:view",
  ROLE_MANAGE: "role:manage",
  SYSTEM_USER_VIEW: "system_user:view",
  SYSTEM_USER_MANAGE: "system_user:manage",
  USER_ROLE_VIEW: "user_role:view",
  USER_ROLE_MANAGE: "user_role:manage",
  SAMPLE_REQUESTER_VIEW: "sample_requester:view",
  SAMPLE_REQUESTER_MANAGE: "sample_requester:manage",
};

/**
 * Checks the resolved privilege set from /session (spec 012 T035). Global
 * Administrator needs no special-casing — the backend expands the sentinel to
 * the full catalog before it reaches the client.
 */
export const hasPrivilege = (userSessionDetails, ...privileges) => {
  if (!userSessionDetails || !userSessionDetails.privileges) {
    return false;
  }
  return privileges.some((privilege) =>
    userSessionDetails.privileges.includes(privilege),
  );
};

/**
 * The privilege a legacy role-gated route is really guarding (spec 012 T040).
 * SecureRoute grants access when the user holds the role OR its equivalent
 * privilege, so a Validation user (whose seeded set includes result:enter)
 * reaches Results pages without an explicit Results role assignment.
 */
export const RoleEquivalentPrivileges = {
  Reception: [Privileges.ORDER_CREATE],
  Results: [Privileges.RESULT_ENTER],
  Validation: [Privileges.RESULT_VALIDATE],
  Reports: [Privileges.REPORT_RUN],
  Pathologist: [Privileges.RESULT_PATHOLOGY_SIGN_OFF],
  Cytopathologist: [Privileges.RESULT_CYTOPATHOLOGY_SIGN_OFF],
  "User Account Administrator": [Privileges.USER_MANAGE],
  "Audit Trail": [Privileges.AUDIT_VIEW],
  "Analyser Import": [Privileges.ANALYZER_IMPORT],
  "Global Administrator": [Privileges.SYSTEM_CONFIGURE],
};

/**
 * Pure route-access decision used by SecureRoute (extracted so it is unit
 * testable). Given the session and a route's guard props ({ role, privilege,
 * labUnitRole }), returns whether access is granted.
 *
 * Semantics:
 * - No guard props at all → granted (an authenticated user may enter).
 * - An explicit role/privilege is satisfied by holding that role OR the
 *   privilege it maps to (RoleEquivalentPrivileges) OR an explicitly-listed
 *   privilege.
 * - A labUnitRole is satisfied by holding the named lab-unit role (or the
 *   AllLabUnits wildcard).
 * - When a route names BOTH an explicit role/privilege AND a labUnitRole,
 *   EITHER one grants access (OR) — e.g. the Pathology dashboard is reachable
 *   both by a global Pathologist (role / sign-off privilege) and by a
 *   technician assigned the Pathology unit's Results lab role. When only one
 *   dimension is named, the unnamed dimension is trivially satisfied (AND).
 */
export const computeRouteAccess = (userDetails, props = {}) => {
  const requestedRoles = [].concat(props.role || []);
  const equivalentPrivileges = requestedRoles.flatMap(
    (role) => RoleEquivalentPrivileges[role] || [],
  );
  const explicitPrivileges = [].concat(props.privilege || []);
  const explicitAccessRequested =
    Boolean(props.role) || Boolean(props.privilege);
  const matchesExplicitRoleOrPrivilege =
    requestedRoles.some(
      (role) => userDetails?.roles && userDetails.roles.includes(role),
    ) ||
    hasPrivilege(userDetails, ...equivalentPrivileges, ...explicitPrivileges);
  const hasRole = !explicitAccessRequested || matchesExplicitRoleOrPrivilege;

  let containsLabUnitRole = false;
  if (props.labUnitRole) {
    Object.keys(props.labUnitRole).forEach((labunit) => {
      if (userDetails?.userLabRolesMap) {
        const userRoles = userDetails.userLabRolesMap["AllLabUnits"]
          ? userDetails.userLabRolesMap["AllLabUnits"]
          : userDetails.userLabRolesMap[labunit] || [];
        props.labUnitRole[labunit].forEach((r) => {
          if (userRoles.includes(r)) {
            containsLabUnitRole = true;
          }
        });
      }
    });
  }
  const hasLabUnitRole = !props.labUnitRole || containsLabUnitRole;

  if (explicitAccessRequested && props.labUnitRole) {
    return matchesExplicitRoleOrPrivilege || containsLabUnitRole;
  }
  return hasRole && hasLabUnitRole;
};

// this is complicated to enable it to format "smartly" as a person types
// possible rework could allow it to only format completed numbers

export const getFromOpenElisServerV2 = <T = LegacyApiResponse>(
  url: string,
): Promise<T> => {
  return new Promise<T>((resolve, reject) => {
    // Simulating the original callback-based function
    getFromOpenElisServer(url, (res) => {
      if (res) {
        resolve(res as T);
      } else {
        reject("Failed to fetch Subscription data");
      }
    });
  });
};

export const patchToOpenElisServerJsonResponse = <
  T = LegacyApiResponse,
  TExtra = unknown,
>(
  endPoint: string,
  payLoad: RequestPayload,
  callback: (response: T | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrfToken(),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad as BodyInit,
    },
  )
    .then(handleSessionError)
    .then((response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      return response.json();
    })
    .then((json) => {
      callback(json as T, extraParams);
    })
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const convertAlphaNumLabNumForDisplay = (
  labNumber: string | null | undefined,
): string | null | undefined => {
  if (!labNumber) {
    return labNumber;
  }
  if (labNumber.length > 15) {
    // Longer-than-15 accessions (e.g. 20-char SiteYearNum like
    // DEV01263000000000001) aren't reformatted — they're opaque IDs.
    // Return as-is without warning; legacy dashed formatting below is only
    // for the old 12-char Tacoma-style lab numbers.
    return labNumber;
  }
  //if dash made it into value, then it's part of the analysis number, not the base lab number
  const labNumberParts = labNumber.split("-");
  const isAnalysisLabNumber = labNumberParts.length > 1;
  let labNumberForDisplay: string;
  //incomplete lab number
  if (labNumberParts[0].length < 8) {
    labNumberForDisplay = labNumberParts[0].slice(0, 2);
    if (labNumberParts[0].length > 2) {
      labNumberForDisplay = labNumberForDisplay + "-";
      labNumberForDisplay = labNumberForDisplay + labNumberParts[0].slice(2);
    }
  } else {
    //possibly complete lab number
    labNumberForDisplay = labNumberParts[0].slice(0, 2) + "-";
    if (labNumberParts[0].length > 8) {
      // lab number contains prefix
      labNumberForDisplay =
        labNumberForDisplay +
        labNumberParts[0].slice(2, labNumberParts[0].length - 6) +
        "-";
    }
    labNumberForDisplay =
      labNumberForDisplay +
      labNumberParts[0].slice(
        labNumberParts[0].length - 6,
        labNumberParts[0].length - 3,
      ) +
      "-";

    labNumberForDisplay =
      labNumberForDisplay +
      labNumberParts[0].slice(labNumberParts[0].length - 3);
  }
  //re-add dash
  if (isAnalysisLabNumber) {
    labNumberForDisplay = labNumberForDisplay + "-" + labNumberParts[1];
  }
  return labNumberForDisplay.toUpperCase();
};

export function encodeDate(dateString: string): string {
  if (typeof dateString === "string" && dateString.trim() !== "") {
    return dateString.split("/").map(encodeURIComponent).join("%2F");
  } else {
    return "";
  }
}

export function getDifferenceInDays(date1: string, date2: string): number {
  console.log("secondDate", date2);

  // Function to parse dates in DD/MM/YYYY format
  function parseDate(dateStr: string): Date {
    const [day, month, year] = dateStr.split("/").map(Number);
    return new Date(year, month - 1, day); // Months are 0-based in JavaScript Date
  }

  function correctDate(firstDate: string): string {
    // "08/05/2024" the error is 08 is not day it is month and 05 is day
    const dateParts = firstDate.split("/");
    if (dateParts[0].length === 4) {
      return dateParts[1] + "/" + dateParts[0] + "/" + dateParts[2];
    }
    return firstDate;
  }

  // Convert the date strings to Date objects
  const firstDate = parseDate(correctDate(date1));
  const secondDate = parseDate(correctDate(date2));

  // Calculate the difference in time (milliseconds)
  const timeDifference = secondDate.getTime() - firstDate.getTime();

  // Convert the time difference from milliseconds to days
  const millisecondsPerDay = 1000 * 60 * 60 * 24;
  const dayDifference = timeDifference / millisecondsPerDay;

  // Return the rounded difference in days
  return dayDifference;
}

export function formatTimestamp(timestamp: number): string {
  // Convert the timestamp to milliseconds and create a Date object
  const date = new Date(timestamp * 1000);

  // Extract and format components
  const hours = date.getUTCHours();
  const minutes = date.getUTCMinutes();
  const day = date.getUTCDate();
  const month = date.getUTCMonth() + 1; // Months are zero-based
  const year = date.getUTCFullYear();

  // Determine AM or PM and format hours
  const ampm = hours >= 12 ? "PM" : "AM";
  const formattedHours = (hours % 12 || 12).toString().padStart(2, "0");
  const formattedMinutes = minutes.toString().padStart(2, "0");

  // Format day and month
  const formattedDay = day.toString().padStart(2, "0");
  const formattedMonth = month.toString().padStart(2, "0");

  // Combine and return the formatted string
  return `${formattedHours}:${formattedMinutes} ${ampm}; ${formattedDay}/${formattedMonth}/${year}`;
}

// Helper function to convert a URL-safe base64 string to a Uint8Array
export function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");

  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);

  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export const Roles = {
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
  EQA_COORDINATOR: "EQA Coordinator",
} as const;

export const toBase64 = (file: Blob): Promise<string> =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
  });
