import config from "../../config.json";
import { format } from "date-fns";
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
  permissions?: string[];
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

const DATE_FMT = "yyyy-MM-dd";

/**
 * Format a Date as a local `yyyy-MM-dd` string. Unlike `Date.toISOString()`,
 * this reads the browser's LOCAL date components, so a date-only value picked in
 * a UTC+ timezone is not rolled back a day when sent to the server. Non-Date
 * input is returned as-is (or "" for null/undefined).
 */
export const toLocalIsoDate = (d: Date | string | null | undefined): string =>
  !(d instanceof Date)
    ? d || ""
    : isNaN(d.getTime())
      ? ""
      : format(d, DATE_FMT);

/**
 * Strict parser for a date picker with dateFormat "Y-m-d": a real `yyyy-MM-dd`
 * becomes that local date; anything else is refused (false). Flatpickr's own
 * parser turns text in another shape into 1 January, which was then saved.
 */
export const parseIsoDate = (text: string | null | undefined): Date | false => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec((text || "").trim());
  if (!match) {
    return false;
  }
  const [year, month, day] = match.slice(1).map(Number);
  const date = new Date(year, month - 1, day);
  return date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day
    ? date
    : false;
};

/**
 * Turn a date string as CustomDatePicker renders it (`MM/dd/yyyy`, or
 * `dd/MM/yyyy` under the French locale) back into the `yyyy-MM-dd` the server
 * reads. Returns "" for anything that is not a three-part date.
 */
export const displayDateToIso = (
  displayed: string | null | undefined,
  dateLocale?: string,
): string => {
  const parts = (displayed || "").split("/");
  if (parts.length !== 3) return "";
  const [month, day] =
    dateLocale === "fr-FR" ? [parts[1], parts[0]] : [parts[0], parts[1]];
  return `${parts[2]}-${month}-${day}`;
};

/**
 * Format a timestamp (epoch millis / ISO string / Date) as local
 * `yyyy-MM-dd HH:mm`, or "—" when absent. Companion to toLocalIsoDate for
 * date-time display columns. (Distinct from the legacy `formatTimestamp`
 * below, which takes Unix SECONDS and renders a UTC AM/PM string.)
 */
export const toLocalIsoDateTime = (
  value: Date | string | number | null | undefined,
): string => {
  const d = value ? new Date(value) : null;
  return d && !isNaN(d.getTime()) ? format(d, `${DATE_FMT} HH:mm`) : "—";
};

/**
 * Render a date-of-record (deadline, due date) as `dd/MM/yyyy`. Such values are
 * stored as an end-of-day timestamp, so reading LOCAL components rolls them to
 * the next day for any browser east of the server; the UTC components give the
 * calendar date that was actually entered. Returns "" for absent values.
 */
export const formatDateOnly = (
  value: Date | string | number | null | undefined,
): string => {
  if (!value) {
    return "";
  }
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) {
    return "";
  }
  const day = String(d.getUTCDate()).padStart(2, "0");
  const month = String(d.getUTCMonth() + 1).padStart(2, "0");
  return `${day}/${month}/${d.getUTCFullYear()}`;
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
      // An error response carries a JSON body too. Handing that body to the
      // caller as if it were data turns a 500 into a render-time crash, so a
      // failed request reports nothing instead.
      if (!response.ok) {
        console.error(`GET ${endPoint} failed: HTTP ${response.status}`);
        callback(undefined);
        return;
      }
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
      if (error.name === "AbortError" || signal?.aborted) {
        return; // Component is unmounting, don't call callback
      }
      console.error(error);
      callback(undefined);
    });
};

/**
 * Promise-based GET for the query layer.
 *
 * Legacy callers intentionally keep the callback contract above: many of
 * them interpret an application error body as part of their existing flow.
 * Cached reads need a different contract. A non-success HTTP response must
 * reject so TanStack Query can put the screen in its error state instead of
 * treating an error payload as usable data.
 */
export const fetchFromOpenElisServer = async <T>(
  endPoint: string,
  signal?: AbortSignal,
): Promise<T> => {
  const response = await fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "GET",
    signal,
    headers: {
      "Accept-Language": getAcceptLanguageHeader(),
    },
  });

  if (!response.ok) {
    throw new Error(`Request failed (${response.status}): ${endPoint}`);
  }

  const contentType = response.headers.get("content-type");
  if (!contentType || !contentType.includes("application/json")) {
    throw new Error(`Expected a JSON response: ${endPoint}`);
  }

  return (await response.json()) as T;
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

/**
 * The one body shared by every *FullResponse helper. The callback gets the raw
 * Response, so a 4xx body — a state-machine refusal, a validation message — can
 * be read and shown instead of being flattened into "it failed".
 */
const sendForFullResponse = <TExtra = unknown>(
  method: "POST" | "PUT" | "PATCH" | "DELETE",
  endPoint: string,
  payLoad: RequestPayload | undefined,
  callback: (response: Response | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    //includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: method,
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": csrfToken(),
      "Accept-Language": getAcceptLanguageHeader(),
    },
    ...(payLoad === undefined ? {} : { body: payLoad as BodyInit }),
  })
    .then(handleSessionError)
    .then((response) => callback(response, extraParams))
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const postToOpenElisServerFullResponse = <TExtra = unknown>(
  endPoint: string,
  payLoad: RequestPayload,
  callback: (response: Response | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void =>
  sendForFullResponse("POST", endPoint, payLoad, callback, extraParams);

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

/**
 * Posts a multipart form and hands back the parsed JSON body, for endpoints
 * that answer an upload with a result rather than a bare status (the catalog
 * import's preview and apply). A non-2xx answer still resolves, carrying the
 * status, so callers can show the server's own message.
 */
export const postToOpenElisServerFormDataJsonResponse = <
  T = LegacyApiResponse,
  TExtra = unknown,
>(
  endPoint: string,
  formData: FormData,
  callback: (response: T | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "POST",
    headers: {
      "X-CSRF-Token": csrfToken(),
      "Accept-Language": getAcceptLanguageHeader(),
    },
    body: formData,
  })
    .then(handleSessionError)
    .then((response) =>
      response
        .text()
        .then((raw) => (raw ? JSON.parse(raw) : {}))
        .then((parsed) =>
          response.ok
            ? parsed
            : {
                ...parsed,
                status: response.status,
                statusCode: response.status,
              },
        )
        .catch(() => ({
          error: `Request failed (HTTP ${response.status})`,
          status: response.status,
          statusCode: response.status,
        })),
    )
    .then((body) => {
      callback(body as T, extraParams);
    })
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
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
        // fail, preserve the HTTP status so callers can still distinguish
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
): void => sendForFullResponse("PUT", endPoint, payLoad, callback, extraParams);

/**
 * PATCH counterpart. The JSON-only PATCH variant discards the body on !ok, which
 * is exactly what a state-machine refusal must not do.
 */
export const patchToOpenElisServerFullResponse = <TExtra = unknown>(
  endPoint: string,
  payLoad: RequestPayload,
  callback: (response: Response | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void =>
  sendForFullResponse("PATCH", endPoint, payLoad, callback, extraParams);

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
): void =>
  sendForFullResponse("DELETE", endPoint, undefined, callback, extraParams);

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
 * Privilege name constants, mirrors Privileges.java (spec 012 T036). The
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
  MICRO_VIEW: "micro:view",
  MICRO_BENCH: "micro:bench",
  MICRO_SUPERVISE: "micro:supervise",
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
 * Administrator needs no special-casing, the backend expands the sentinel to
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
/**
 * Bridges a role name to the privilege that means the same capability.
 *
 * <p>App.jsx no longer guards any route on a role, all 85 role guards were
 * converted to {@code privilege={Privileges.X}}, so nothing in the routing table
 * depends on this map any more. It is kept because {@code computeRouteAccess}
 * still accepts a {@code role} prop, so a caller passing one (including an
 * external or future component) keeps working and keeps admitting
 * privilege-holders rather than only exact role-name matches.
 *
 * <p>This is a compatibility shim, not part of the access model. Once nothing
 * passes {@code role} at all, it and the {@code role} branch of
 * computeRouteAccess can both go.
 */
export const RoleEquivalentPrivileges = {
  Reception: [Privileges.ORDER_CREATE],
  Results: [Privileges.RESULT_ENTER],
  Validation: [Privileges.RESULT_VALIDATE],
  Reports: [Privileges.REPORT_RUN],
  Pathologist: [Privileges.RESULT_PATHOLOGY_SIGN_OFF],
  Cytopathologist: [Privileges.RESULT_PATHOLOGY_SIGN_OFF],
  "User Account Administrator": [Privileges.USER_MANAGE],
  "Audit Trail": [Privileges.AUDIT_VIEW],
  "Analyser Import": [Privileges.ANALYZER_IMPORT],
  "EQA Coordinator": [Privileges.EQA_VIEW],
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
 *   EITHER one grants access (OR), e.g. the Pathology dashboard is reachable
 *   both by a global Pathologist (role / sign-off privilege) and by a
 *   technician assigned the Pathology unit's Results lab role. When only one
 *   dimension is named, the unnamed dimension is trivially satisfied (AND).
 */
// Declared here, above ROUTE_GUARDS, because that map's entries name
// Roles.* and the object literal is evaluated at module load: with Roles
// further down the file, importing Utils threw "Cannot access 'Roles'
// before initialization" and every consumer failed.
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
  // Used by App.jsx for /qa/qc/reagent-qc but declared on neither side of the
  // merge: Roles.LAB_SUPERVISOR evaluated to undefined, so that route's guard
  // matched nobody. The seeded role is "Lab Supervisor".
  LAB_SUPERVISOR: "Lab Supervisor",
} as const;

/**
 * The guard props each SecureRoute in App.jsx carries, keyed by its `path`.
 *
 * The sidebar is built from /rest/menu, which returns every menu row the
 * installation has configured with no reference to the caller's privileges.
 * SecureRoute then refuses the ones the user cannot open, so roughly half of
 * each role's menu led to a blank screen: 74 such entries for Reception, 88 for
 * Results and Validation, 55 for Reports, out of 153.
 *
 * The props are stored verbatim and handed straight back to computeRouteAccess
 * rather than being reduced to a privilege here. 31 routes are still guarded by
 * `role=` rather than `privilege=` (the privilege conversion did not reach
 * them), and a role guard is satisfied through RoleEquivalentPrivileges.
 * Flattening would mean re-implementing that mapping and drifting from it;
 * passing the props through means the menu and the route cannot disagree.
 *
 * This duplicates App.jsx, so it is pinned: `menuRouteGuards.test.js` parses
 * App.jsx and fails if a guard is added, removed or changed without the
 * matching change here.
 */
export const ROUTE_GUARDS = {
  "/AccessionResults": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/AccessionValidation": {
    privilege: Privileges.RESULT_VALIDATE,
    role: [Roles.VALIDATION],
  },
  "/AccessionValidationRange": {
    privilege: Privileges.RESULT_VALIDATE,
    role: [Roles.VALIDATION],
  },
  "/Alerts": { role: [Roles.RECEPTION, Roles.RESULTS] },
  "/Aliquot": { privilege: Privileges.ORDER_CREATE, role: [Roles.RECEPTION] },
  "/AnalyzerResults": { role: [Roles.GLOBAL_ADMIN, Roles.ANALYSER_IMPORT] },
  "/ElectronicOrders": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/EnvironmentalDashboard": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  // Guarded by the privilege the cold-storage services actually enforce
  // (PRIV_COLDSTORAGE_VIEW), not by role. The role form offered the five
  // freezer tabs to Reception, which holds storage:view but not
  // coldstorage:view, so every tab rendered an empty page and retried its
  // denied reads in a loop.
  "/FreezerMonitoring": { privilege: Privileges.COLDSTORAGE_VIEW },
  "/GenericSample/Edit": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/GenericSample/Import": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/GenericSample/Order": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/GenericSample/Results": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/ImmunohistochemistryCaseView/:immunohistochemistrySampleId": {
    privilege: Privileges.RESULT_PATHOLOGY_SIGN_OFF,
  },
  "/ImmunohistochemistryDashboard": {
    privilege: Privileges.RESULT_PATHOLOGY_SIGN_OFF,
  },
  "/LaporanHasil": { privilege: Privileges.REPORT_RUN, role: [Roles.REPORTS] },
  "/LogbookResults": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  // App.jsx guards this through REPORTING_ROUTE_PATHS (a constant), so the
  // menu map never mirrored it and the sidebar offered Custom Data Export to
  // every role; SecureRoute then rendered a blank page. Mirrors role={Roles.REPORTS}.
  "/CustomDataExport": { role: [Roles.REPORTS] },
  "/MasterListsPage": {
    privilege: Privileges.SYSTEM_CONFIGURE,
    role: [Roles.GLOBAL_ADMIN],
  },
  // Both are guarded in App.jsx through MICROBIOLOGY_*_PATH constants, which
  // the menu map never mirrored, so the bench worklist and the WHONET export
  // showed for every role and then rendered blank. Mirrors App.jsx exactly.
  "/Microbiology/whonet": {
    role: [Roles.GLOBAL_ADMIN, Roles.RESULTS, Roles.REPORTS],
  },
  "/Microbiology/worklist": {
    role: [Roles.GLOBAL_ADMIN, Roles.RESULTS, Roles.VALIDATION],
  },
  "/ModifyOrder": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/NCECorrectiveAction": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.VALIDATION],
  },
  "/NceDashboard": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.VALIDATION],
  },
  // /rest/menu serves /NotebookDashboard while App.jsx routes
  // /NoteBookDashboard. React Router matches either, but the menu map was keyed
  // only on App.jsx's spelling, so the row the menu actually emits was
  // unguarded. Same guard as its twin below.
  "/NotebookDashboard": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.VALIDATION],
  },
  "/NoteBookDashboard": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.VALIDATION],
  },
  "/NoteBookEntryForm": {
    privilege: Privileges.SYSTEM_CONFIGURE,
    role: [Roles.GLOBAL_ADMIN],
  },
  "/NoteBookEntryForm/:notebookid": {
    privilege: Privileges.SYSTEM_CONFIGURE,
    role: [Roles.GLOBAL_ADMIN],
  },
  "/NoteBookInstanceEditForm/:notebookentryid": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/NoteBookInstanceEntryForm/:notebookid": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/NotebookSampleOrder/:notebookId": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/NotebookSampleOrder/:notebookId/:notebookEntryId": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/PathologyCaseView/:pathologySampleId": {
    privilege: Privileges.RESULT_PATHOLOGY_SIGN_OFF,
  },
  "/PathologyDashboard": { privilege: Privileges.RESULT_PATHOLOGY_SIGN_OFF },
  // Patient History's only action is to open /PatientResults/:patientId, the
  // results viewer, whose read needs result:view; no legacy module ever gave
  // Reception patient results. Routed on the same privilege as the page it
  // leads to, so Reception is not offered a search whose every hit 403s.
  "/PatientHistory": { privilege: Privileges.RESULT_VIEW },
  "/PatientManagement/:patientId?": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/PatientMerge": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/PatientResults": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  // A patient's RESULTS viewer. Its read (/rest/result-tree,
  // PatientResultTreeService.getResultTree) is gated on result:view, and the
  // legacy PatientResults module belongs to Results, so routing it to
  // Reception on order:create sent Reception to a page that 403s on load from
  // the header search and Patient History. Guarded on the privilege the page
  // actually needs.
  "/PatientResults/:patientId": { privilege: Privileges.RESULT_VIEW },
  "/PrintBarcode": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/RangeResults": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/Report": { privilege: Privileges.REPORT_RUN, role: [Roles.REPORTS] },
  // Declared as develop declares it (nonConformityRoutePermissions.test.js):
  // the EQA lane deep-links these pages and the QA Officer holds neither role,
  // so the permission is what admits them. The screen's first call needs
  // nce:create; 012-004y grants it to every role this declaration admits, so
  // route and service agree.
  "/ReportNonConformingEvent": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.VALIDATION],
  },
  "/ResultValidation": {
    privilege: Privileges.RESULT_VALIDATE,
    role: [Roles.VALIDATION],
  },
  "/ResultValidationByTestDate": {
    privilege: Privileges.RESULT_VALIDATE,
    role: [Roles.VALIDATION],
  },
  "/Results": { privilege: Privileges.RESULT_ENTER, role: [Roles.RESULTS] },
  "/RoutineReport": { privilege: Privileges.REPORT_RUN, role: [Roles.REPORTS] },
  "/RoutineReports": {
    privilege: Privileges.REPORT_RUN,
    role: [Roles.REPORTS],
  },
  "/SampleBatchEntrySetup": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/SampleEdit": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/SampleManagement": { role: [Roles.RECEPTION, Roles.RESULTS] },
  "/SamplePatientEntry": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/SampleShipment": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/SampleShipment/:tab": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/SampleShipment/box/:boxId": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/SampleShipment/create-box": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/SampleShipment/receive": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/SampleShipment/reference-lab-results": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/SampleShipment/reports": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/SampleShipment/settings": { role: [Roles.RECEPTION, Roles.GLOBAL_ADMIN] },
  "/StatusResults": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/Storage": { role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN] },
  "/Storage/:resource(sample-items|inventory-lots|rooms|devices|shelves|racks|boxes)":
    { role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN] },
  "/StudyReport": { privilege: Privileges.REPORT_RUN, role: [Roles.REPORTS] },
  "/StudyReports": { privilege: Privileges.REPORT_RUN, role: [Roles.REPORTS] },
  "/TATReport": { privilege: Privileges.REPORT_RUN, role: [Roles.REPORTS] },
  "/VectorManualEntry": {
    privilege: Privileges.REPORT_RUN,
    role: [Roles.REPORTS],
  },
  "/VectorSurveillanceReport": {
    privilege: Privileges.REPORT_RUN,
    role: [Roles.REPORTS],
  },
  "/ViewNonConformingEvent": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.VALIDATION],
  },
  "/WorkPlanByTestSection": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/Workplan": { role: [Roles.RESULTS] },
  "/WorkplanByPanel": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/WorkplanByPriority": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/WorkplanByTest": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/admin": {
    privilege: Privileges.SYSTEM_CONFIGURE,
    role: [Roles.GLOBAL_ADMIN],
  },
  "/analyzers": { role: [Roles.ANALYSER_IMPORT, Roles.GLOBAL_ADMIN] },
  "/analyzers/qc/charts/:analyzerId": {
    privilege: Privileges.ANALYZER_CONFIGURE,
    role: [Roles.LAB_SUPERVISOR],
  },
  "/analyzers/qc/control-lots/:id": {
    privilege: Privileges.ANALYZER_CONFIGURE,
    role: [Roles.LAB_SUPERVISOR],
  },
  "/analyzers/qc/control-lots/new": {
    privilege: Privileges.ANALYZER_CONFIGURE,
    role: [Roles.LAB_SUPERVISOR],
  },
  "/analyzers/qc/instruments/:instrumentId": {
    privilege: Privileges.ANALYZER_CONFIGURE,
    role: [Roles.LAB_SUPERVISOR],
  },
  "/analyzers/types": { role: [Roles.ANALYSER_IMPORT, Roles.GLOBAL_ADMIN] },
  "/analyzers/types/:profileId/mapping": {
    privilege: Privileges.ANALYZER_IMPORT,
    role: [Roles.ANALYSER_IMPORT],
  },
  "/genericProgram": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/inventory": { role: [Roles.RESULTS, Roles.GLOBAL_ADMIN] },
  // The order workflow steps are guarded in App.jsx inside nested routers
  // (path={`${match.path}/enter`} under <Route path="/order/clinical">), which
  // the menu map never mirrored, so the sidebar offered Enter Order, Prepare
  // Samples and Sample check to every role and the page rendered blank for
  // all but Reception. Mirrored exactly as App.jsx guards them.
  "/order/clinical": { role: [Roles.RECEPTION] },
  "/order/clinical/enter": { role: [Roles.RECEPTION] },
  "/order/clinical/collect": { role: [Roles.RECEPTION] },
  "/order/clinical/qa": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/order/environmental": { role: [Roles.RECEPTION] },
  "/order/environmental/enter": { role: [Roles.RECEPTION] },
  "/order/environmental/label": { role: [Roles.RECEPTION] },
  "/order/environmental/qa": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/order/vector": { role: [Roles.RECEPTION] },
  "/order/vector/enter": { role: [Roles.RECEPTION] },
  "/order/vector/label": { role: [Roles.RECEPTION] },
  "/order/vector/qa": { role: [Roles.RECEPTION] },
  "/order/vector/complete": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/programView/:programSampleId": {
    privilege: Privileges.ORDER_CREATE,
    role: [Roles.RECEPTION],
  },
  "/qa/eqa/analyst-competency": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/qa/eqa/follow-up-queue": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/qa/eqa/in-house": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/qa/eqa/in-house/new": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/qa/eqa/lab-performance/coverage": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/qa/eqa/lab-performance/recent": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  // Scheme administration, not a participant view: the page edits EQA schemes,
  // enrolment and lab-wide System Settings (FHIR integration, Z-score
  // acceptance bounds, notification policy). Its writes already require
  // EQAGuards.PROVIDER/MANAGE server-side, so a role holding only qa.view.eqa
  // could read the whole admin console and be refused only on Save. Guarded on
  // qa.manage.eqa so the menu stops offering it to Reception and Results, who
  // hold the participant tier.
  "/qa/eqa/management": {
    permission: "qa.manage.eqa",
  },
  "/qa/eqa/my-cycles": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  "/qa/eqa/my-programs": {
    permission: "qa.view.eqa",
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN],
  },
  // Scheme administration, not a participant view: the page edits EQA schemes,
  // enrolment and lab-wide System Settings (FHIR integration, Z-score
  // acceptance bounds, notification policy). Its writes already require
  // EQAGuards.PROVIDER/MANAGE server-side, so a role holding only qa.view.eqa
  // could read the whole admin console and be refused only on Save. Guarded on
  // qa.manage.eqa so the menu stops offering it to Reception and Results, who
  // hold the participant tier.
  "/qa/eqa/participants": {
    permission: "qa.manage.eqa",
  },
  "/qa/eqa/provider/cycles/:cycleId/workbench": {
    permission: "qa.eqa.provider",
  },
  "/qa/eqa/provider/follow-ups": { permission: "qa.eqa.provider" },
  // The provider lane: a lab that RUNS EQA schemes for others (scheme list,
  // new cycle, workbench, performance, follow-ups). It was guarded by role
  // plus qa.view.eqa, so Reception and Results, which hold the participant
  // tier only, were offered provider operations whose every write requires
  // EQAGuards.PROVIDER. Guarded on qa.eqa.provider, held by QA Officer and
  // Global Administrator; the participant pages (My Programs, My Cycles,
  // Lab Performance, Follow-Up, In-House) are untouched.
  "/qa/eqa/provider/schemes": { permission: "qa.eqa.provider" },
  "/qa/eqa/provider/schemes/:schemeId/cycles/new": {
    permission: "qa.eqa.provider",
  },
  "/qa/eqa/provider/schemes/:schemeId/performance": {
    permission: "qa.eqa.provider",
  },
  "/qa/overview": { role: [Roles.RECEPTION, Roles.RESULTS, Roles.VALIDATION] },
  "/qa/qc/alerts": { role: [Roles.LAB_SUPERVISOR] },
  "/qa/qc/control-lots": { role: [Roles.LAB_SUPERVISOR] },
  "/qa/qc/dashboard": { role: [Roles.LAB_SUPERVISOR] },
  "/qa/qc/manual-qc": { role: [Roles.LAB_SUPERVISOR] },
  "/qa/qc/reagent-qc": { role: [Roles.LAB_SUPERVISOR] },
  "/qa/qc/rule-config": { role: [Roles.LAB_SUPERVISOR] },
  "/qa/qi/amendment": { role: [Roles.RESULTS, Roles.REPORTS] },
  "/qa/qi/callback": { role: [Roles.RESULTS, Roles.REPORTS] },
  "/qa/qi/config": {
    permission: "qa.manage.qi",
    role: [Roles.GLOBAL_ADMIN],
  },
  "/qa/qi/dashboard": {
    role: [Roles.RECEPTION, Roles.RESULTS, Roles.VALIDATION],
  },
  "/qa/qi/rejection": { role: [Roles.RESULTS, Roles.REPORTS] },
  "/qa/qi/tat": { role: [Roles.RESULTS, Roles.REPORTS] },
  "/qa/qms/accreditation": {
    permission: "qa.view.qms",
    role: [Roles.GLOBAL_ADMIN],
  },
  "/qa/qms/audit-trail": {
    privilege: Privileges.AUDIT_VIEW,
    role: [Roles.GLOBAL_ADMIN],
  },
  "/qa/qms/capa-register": {
    permission: "qa.view.qms",
    role: [Roles.GLOBAL_ADMIN],
  },
  "/qa/qms/e-signature-log": {
    permission: "qa.view.qms",
    role: [Roles.GLOBAL_ADMIN],
  },
  "/qa/qms/nce-register": {
    permission: "qa.view.qms",
    role: [Roles.RECEPTION, Roles.VALIDATION],
  },
  "/result": { privilege: Privileges.RESULT_ENTER, role: [Roles.RESULTS] },
  "/validation": {
    privilege: Privileges.RESULT_VALIDATE,
    role: [Roles.VALIDATION],
  },
  "/vector/deconvolution": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
  "/vector/identification": {
    privilege: Privileges.RESULT_ENTER,
    role: [Roles.RESULTS],
  },
};

/**
 * Whether a menu SUBTREE contains anything this user can open.
 *
 * The menu nests three deep (Reports -> Aggregate -> a report), so checking
 * only immediate children would leave an empty parent whose every grandchild
 * is hidden. Recursing keeps a section visible exactly while something inside
 * it is reachable.
 */
export const menuSubtreeVisible = (menuItem, userSessionDetails) => {
  if (!menuItem?.menu?.isActive) {
    return false;
  }
  const childVisible = (menuItem.childMenus || []).some((child) =>
    menuSubtreeVisible(child, userSessionDetails),
  );
  if (childVisible) {
    return true;
  }
  // A section header carries no actionURL, and menuEntryVisible treats "no
  // route" as unguarded. Answering true here would keep every empty section,
  // so a parent with no openable route of its own stands or falls with its
  // children.
  //
  // A LEAF with no actionURL is a different thing: ConfiguredSideNav renders it
  // as a deliberate disabled "not yet connected" placeholder with its
  // toolTipKey, so it is kept. Only a parent whose children all dropped out is
  // the empty section this guards against.
  const actionURL = menuItem.menu.actionURL;
  if (!actionURL || actionURL.length <= 1) {
    return (
      (menuItem.childMenus || []).length === 0 &&
      Boolean(menuItem.menu.toolTipKey)
    );
  }
  return menuEntryVisible(actionURL, userSessionDetails);
};

/**
 * Whether a menu entry should be shown, given the user's session.
 *
 * Mirrors SecureRoute: an unguarded route is open to any authenticated user, a
 * guarded one needs its privilege (or a role that implies it, via
 * computeRouteAccess). Query strings are stripped because menu rows carry them
 * (`/SampleEdit?type=readwrite`) while route paths do not, and `:param`
 * segments are matched by prefix for the same reason.
 *
 * Returning true for anything unrecognised is deliberate: a menu row whose
 * route is not in the map is one SecureRoute does not guard either, so hiding
 * it would remove a working link.
 */
/**
 * Menu paths the legacy module layer gates, and the SystemModules that permit
 * each one.
 *
 * Generated from system_module_url joined to system_module, restricted to the
 * paths /rest/menu actually serves. A path absent here has no
 * SystemModuleUrl row, so ModuleAuthenticationInterceptor auto-allows it and
 * the filter leaves it visible. A path listed here is shown only when the
 * session holds one of its modules, which is the interceptor's own rule.
 *
 * Kept as data rather than inferred at runtime because the mapping lives in
 * the database and the frontend cannot query it; moduleMapDrift.test.js pins
 * it against the seed so the two cannot silently diverge.
 */
export const MODULE_GUARDED_PATHS: Record<string, string[]> = {
  "/AccessionResults": ["AccessionResults"],
  "/AccessionValidation": ["ResultsValidationGeneral"],
  "/AccessionValidationRange": ["ResultsValidationGeneral"],
  "/Alerts": ["EQAView"],
  "/AnalyzerResults": ["AnalyzerResults"],
  "/AuditTrailReport": ["AuditTrailView"],
  "/CytologyDashboard": ["Cytology"],
  "/EQADistribution": ["EQAView"],
  "/EQAManagement": ["EQAView"],
  "/EQAMyPrograms": ["EQAView"],
  "/EQAOrders": ["EQAView"],
  "/EQAParticipants": ["EQAView"],
  "/EQAResults": ["EQAView"],
  "/ElectronicOrders": ["ElectronicOrderView"],
  "/LogbookResults": [
    "LogbookResults",
    "LogbookResults:Biochemistry",
    "LogbookResults:ECBU",
    "LogbookResults:EID",
    "LogbookResults:HIV",
    "LogbookResults:Hematology",
    "LogbookResults:Immunology",
    "LogbookResults:Molecular Biology",
    "LogbookResults:Parasitology",
    "LogbookResults:Serology-Immunology",
    "LogbookResults:VL",
    "LogbookResults:Virologie",
    "LogbookResults:bacteriology",
    "LogbookResults:chem",
    "LogbookResults:cytobacteriology",
    "LogbookResults:endocrin",
    "LogbookResults:hemato-immunology",
    "LogbookResults:immuno",
    "LogbookResults:liquidBio",
    "LogbookResults:mycobacteriology",
    "LogbookResults:mycology",
    "LogbookResults:mycrobacteriology",
    "LogbookResults:serologie",
    "LogbookResults:serology",
  ],
  "/NCECorrectiveAction": ["NonConformity"],
  "/PathologyDashboard": ["Pathology"],
  "/PatientEditByProject": [
    "PatientEditByProject:readonly",
    "PatientEditByProject:readwrite",
  ],
  "/PatientEntryByProject": [
    "PatientEntryByProject:initial",
    "PatientEntryByProject:verify",
  ],
  "/PatientManagement": ["SamplePatientEntry"],
  "/PatientResults": ["PatientResults"],
  "/PrintBarcode": ["PrintBarcode"],
  "/RangeResults": ["RangeResults"],
  "/Report": [
    "Report:RoutineExport",
    "Report:indicator",
    "Report:patient",
    "Report:summary",
    "ReportCovid",
  ],
  "/ReportNonConformingEvent": ["NonConformity"],
  "/ReportPrint": [
    "Report:RoutineExport",
    "Report:indicator",
    "Report:patient",
    "Report:summary",
    "ReportCovid",
  ],
  "/ResultValidation": [
    "ResultValidation",
    "ResultValidation:Bacteria",
    "ResultValidation:Biochemistry",
    "ResultValidation:Cytobacteriologie",
    "ResultValidation:ECBU",
    "ResultValidation:EID",
    "ResultValidation:Endocrinologie",
    "ResultValidation:Hematology",
    "ResultValidation:Hemto-Immunology",
    "ResultValidation:Immunology",
    "ResultValidation:Liquides biologique",
    "ResultValidation:Molecular Biology",
    "ResultValidation:Mycobacteriology",
    "ResultValidation:Parasitology",
    "ResultValidation:Serologie",
    "ResultValidation:Serology-Immunology",
    "ResultValidation:VCT",
    "ResultValidation:VL",
    "ResultValidation:Virologie",
    "ResultValidation:mycology",
    "ResultValidation:serology",
    "ResultValidation:virology",
  ],
  "/ResultValidationByTestDate": ["ResultsValidationGeneral"],
  "/ResultValidationRetroC": [
    "ResultValidation",
    "ResultValidation:Bacteria",
    "ResultValidation:Biochemistry",
    "ResultValidation:Cytobacteriologie",
    "ResultValidation:ECBU",
    "ResultValidation:EID",
    "ResultValidation:Endocrinologie",
    "ResultValidation:Hematology",
    "ResultValidation:Hemto-Immunology",
    "ResultValidation:Immunology",
    "ResultValidation:Liquides biologique",
    "ResultValidation:Molecular Biology",
    "ResultValidation:Mycobacteriology",
    "ResultValidation:Parasitology",
    "ResultValidation:Serologie",
    "ResultValidation:Serology-Immunology",
    "ResultValidation:VCT",
    "ResultValidation:VL",
    "ResultValidation:Virologie",
    "ResultValidation:mycology",
    "ResultValidation:serology",
    "ResultValidation:virology",
  ],
  "/SampleBatchEntrySetup": ["SampleBatchEntry"],
  "/SampleEdit": ["SampleEdit", "SampleEdit:readonly", "SampleEdit:readwrite"],
  "/SampleEntryByProject": [
    "SampleEntryByProject:initial",
    "SampleEntryByProject:verify",
  ],
  "/SamplePatientEntry": ["SamplePatientEntry"],
  "/SampleShipment": ["SampleShipmentManagement"],
  "/StatusResults": ["StatusResults"],
  "/StudyElectronicOrders": ["StudyElectronicOrderView"],
  "/VectorSurveillanceReport": ["VectorSurveillanceDashboard"],
  "/ViewNonConformingEvent": ["NonConformity"],
  "/WorkPlanByPanel": [
    "Workplan",
    "Workplan:Biochemistry",
    "Workplan:ECBU",
    "Workplan:EID",
    "Workplan:HIV",
    "Workplan:Hematology",
    "Workplan:Immunology",
    "Workplan:Molecular Biology",
    "Workplan:Parasitology",
    "Workplan:Serology",
    "Workplan:Serology-Immunology",
    "Workplan:VL",
    "Workplan:Virologie",
    "Workplan:bacteriology",
    "Workplan:chem",
    "Workplan:cytobacteriology",
    "Workplan:endocrin",
    "Workplan:hemato-immunology",
    "Workplan:immuno",
    "Workplan:liquidBio",
    "Workplan:mycology",
    "Workplan:mycrobacteriology",
    "Workplan:panel",
    "Workplan:serologie",
    "Workplan:test",
  ],
  "/WorkPlanByPriority": ["Workplan"],
  "/WorkPlanByTest": [
    "Workplan",
    "Workplan:Biochemistry",
    "Workplan:ECBU",
    "Workplan:EID",
    "Workplan:HIV",
    "Workplan:Hematology",
    "Workplan:Immunology",
    "Workplan:Molecular Biology",
    "Workplan:Parasitology",
    "Workplan:Serology",
    "Workplan:Serology-Immunology",
    "Workplan:VL",
    "Workplan:Virologie",
    "Workplan:bacteriology",
    "Workplan:chem",
    "Workplan:cytobacteriology",
    "Workplan:endocrin",
    "Workplan:hemato-immunology",
    "Workplan:immuno",
    "Workplan:liquidBio",
    "Workplan:mycology",
    "Workplan:mycrobacteriology",
    "Workplan:panel",
    "Workplan:serologie",
    "Workplan:test",
  ],
  "/WorkPlanByTestSection": [
    "Workplan",
    "Workplan:Biochemistry",
    "Workplan:ECBU",
    "Workplan:EID",
    "Workplan:HIV",
    "Workplan:Hematology",
    "Workplan:Immunology",
    "Workplan:Molecular Biology",
    "Workplan:Parasitology",
    "Workplan:Serology",
    "Workplan:Serology-Immunology",
    "Workplan:VL",
    "Workplan:Virologie",
    "Workplan:bacteriology",
    "Workplan:chem",
    "Workplan:cytobacteriology",
    "Workplan:endocrin",
    "Workplan:hemato-immunology",
    "Workplan:immuno",
    "Workplan:liquidBio",
    "Workplan:mycology",
    "Workplan:mycrobacteriology",
    "Workplan:panel",
    "Workplan:serologie",
    "Workplan:test",
  ],
};

export const menuEntryVisible = (actionURL, userSessionDetails) => {
  if (!actionURL) {
    return true;
  }
  const path = actionURL.split("?")[0];
  let guard = ROUTE_GUARDS[path];
  if (!guard) {
    // A parameterised route ("/PathologyCaseView/:id") never matches a menu
    // row literally; match the portion before the first parameter instead.
    const match = Object.keys(ROUTE_GUARDS).find((route) => {
      const base = route.split("/:")[0];
      return base.length > 1 && (path === base || path.startsWith(base + "/"));
    });
    guard = match ? ROUTE_GUARDS[match] : undefined;
  }
  if (!guard) {
    // No SecureRoute guard: the React route admits any authenticated user, but
    // a second, older authorization layer may still refuse the page.
    // ModuleAuthenticationInterceptor checks the URL against system_module_url
    // and the caller's permitted modules BEFORE method security runs, and its
    // denial is invisible here: a full-page redirect to /Home?access=denied for
    // a server-rendered path, or a page that loads and then 403s its data
    // calls. That is why Reception was still shown /CytologyDashboard,
    // /ResultValidationRetroC, /WorkPlanByTest* and /ReportPrint after the
    // guard-based filter landed.
    const typeParam = new URLSearchParams(actionURL.split("?")[1] || "").get(
      "type",
    );
    return moduleEntryVisible(path, userSessionDetails, typeParam || undefined);
  }
  return computeRouteAccess(userSessionDetails, guard);
};

/**
 * Whether the legacy module layer would admit this path.
 *
 * <p>The session carries `modules`, the names of the SystemModules the caller's
 * roles grant. MODULE_GUARDED_PATHS lists the menu paths that map to at least
 * one SystemModuleUrl row; a path absent from it has no module mapping and is
 * auto-allowed by the interceptor, so it stays visible. A listed path is shown
 * only when the caller holds one of its modules, which is exactly the
 * interceptor's own rule (any one match permits).
 *
 * <p>Global Administrator bypasses the interceptor via isUserAdmin(), so it is
 * admitted here too.
 */
export const moduleEntryVisible = (path, userSessionDetails, typeParam?) => {
  let required = MODULE_GUARDED_PATHS[path];
  if (!required) {
    return true;
  }
  // A menu row such as /ResultValidationRetroC?type=Immunology is served by
  // the module named for its type (ResultValidation:Immunology), not by any
  // module in the family: ModuleAuthenticationInterceptor checks the specific
  // one. Holding only ResultValidation:EID showed Results the whole validation
  // subtree, every row of which then redirected to /Home?access=denied. When
  // the row names a type and the family has typed modules, require the bare
  // module or the one for that type.
  if (typeParam) {
    const wanted = typeParam.toLowerCase();
    const narrowed = required.filter((name) => {
      const colon = name.indexOf(":");
      return colon === -1 || name.slice(colon + 1).toLowerCase() === wanted;
    });
    if (narrowed.length > 0 && narrowed.length < required.length) {
      required = narrowed;
    }
  }
  if (userSessionDetails?.roles?.includes(Roles.GLOBAL_ADMIN)) {
    return true;
  }
  const held = userSessionDetails?.modules;
  if (!Array.isArray(held)) {
    // The session predates this field: fall back to showing the row rather
    // than hiding working pages from everyone.
    return true;
  }
  return required.some((moduleName) => held.includes(moduleName));
};

export const computeRouteAccess = (userDetails, props = {}) => {
  const requestedRoles = [].concat(props.role || []);
  const equivalentPrivileges = requestedRoles.flatMap(
    (role) => RoleEquivalentPrivileges[role] || [],
  );
  const explicitPrivileges = [].concat(props.privilege || []);
  // qa.* permission keys (EQA V2 / the QA pillar) sit in a separate model from
  // PRIV_* and arrive on the session as `permissions`. A route naming one is
  // satisfied by holding it, or by Global Administrator, matching
  // hasQaPermission and the server-side EQAGuards expressions.
  const requestedPermissions = [].concat(props.permission || []);
  const matchesPermission = requestedPermissions.some((permission) =>
    hasQaPermission(userDetails, permission),
  );
  const explicitAccessRequested =
    Boolean(props.role) ||
    Boolean(props.privilege) ||
    Boolean(props.permission);
  const matchesExplicitRoleOrPrivilege =
    requestedRoles.some(
      (role) => userDetails?.roles && userDetails.roles.includes(role),
    ) ||
    hasPrivilege(userDetails, ...equivalentPrivileges, ...explicitPrivileges) ||
    matchesPermission;
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
/** True when the session carries the named permission. */
export const hasPermission = (
  userSessionDetails: UserSessionDetails | null | undefined,
  permission: string | null | undefined,
): boolean =>
  !!permission && !!userSessionDetails?.permissions?.includes(permission);

/**
 * The gate feature entry points use: the named permission, or the global
 * administrator role, which is allowed everything. Server-side @PreAuthorize is
 * still the real check — this only decides whether to show the entry point.
 */
export const hasPermissionOrGlobalAdmin = (
  userSessionDetails: UserSessionDetails | null | undefined,
  permission: string,
): boolean =>
  hasPermission(userSessionDetails, permission) ||
  hasRole(userSessionDetails, Roles.GLOBAL_ADMIN);

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

// Drops only a numeric analysis suffix (BASE-N), so IH-2-01 stays whole.
export const labNumberForSearch = (
  accessionNumber: string | null | undefined,
): string => (accessionNumber ?? "").trim().replace(/^([^-]*)-\d+$/, "$1");

export const convertAlphaNumLabNumForDisplay = (
  labNumber: string | null | undefined,
): string | null | undefined => {
  if (!labNumber) {
    return labNumber;
  }
  if (labNumber.length > 15) {
    // Longer-than-15 accessions (e.g. 20-char SiteYearNum like
    // DEV01263000000000001) aren't reformatted, they're opaque IDs.
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
  // Re-add every remaining part, not just the first: keeping only one dropped
  // the tail of anything with a second dash in it (an EQA blind code such as
  // IH-2-04 rendered as IH-2, the same for every row on the page).
  if (isAnalysisLabNumber) {
    labNumberForDisplay = [
      labNumberForDisplay,
      ...labNumberParts.slice(1),
    ].join("-");
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

/**
 * True when the session holds a qa.* permission, with Global Administrator as
 * the standing fallback. The real gate is @PreAuthorize on the endpoint; this
 * only hides controls from callers who would get a 403 anyway.
 */
export const hasQaPermission = (
  userSessionDetails: { permissions?: string[]; roles?: string[] } | undefined,
  permission: string,
): boolean =>
  !!userSessionDetails?.permissions?.includes(permission) ||
  !!userSessionDetails?.roles?.includes(Roles.GLOBAL_ADMIN);

export const toBase64 = (file: Blob): Promise<string> =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
  });
