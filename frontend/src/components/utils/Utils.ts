import config from "../../config.json";

// ---------------------------------------------------------------------------
// Types

export interface UserSessionDetails {
  roles?: string[];
  [key: string]: unknown;
}

export interface ApiErrorResponse {
  error: string;
  message: string;
  status: number;
  statusCode?: number;
  statusText?: string;
}

// Defaulted to `any` so old JavaScript can work without changes.
// Typed callers can pass an explicit generic: ApiCallback<MyType>
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type ApiCallback<T = any> = (data: T | undefined) => void;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type ApiCallbackWithParams<T = any, P = unknown> = (
  data: T | undefined,
  extraParams?: P,
) => void;
export type StatusCallback = (status: number) => void;
export type StatusCallbackWithParams<P = unknown> = (
  status: number,
  extraParams?: P,
) => void;
export type ResponseCallback<P = unknown> = (
  response: Response | undefined,
  extraParams?: P,
) => void;

// ---------------------------------------------------------------------------
// Internal helpers

/** Accepts either a ready-to-send `BodyInit` or a plain object (auto-serialized to JSON). */
type Payload = BodyInit | Record<string, unknown>;

const normalizePayload = (payload: Payload): BodyInit => {
  if (
    payload === null ||
    typeof payload === "string" ||
    payload instanceof Blob ||
    payload instanceof FormData ||
    payload instanceof URLSearchParams ||
    payload instanceof ArrayBuffer ||
    ArrayBuffer.isView(payload)
  ) {
    return payload as BodyInit;
  }
  return JSON.stringify(payload);
};

const getAcceptLanguageHeader = (): string => {
  return localStorage.getItem("locale") || navigator.language || "en";
};

const handleSessionError = (response: Response): Response => {
  if (response.status === 403) {
    response
      .clone()
      .json()
      .then((body: { message?: string }) => {
        if (body?.message?.includes("CSRF")) {
          alert(
            "Your session has expired. The page will reload so you can continue.",
          );
          window.location.reload();
        }
      })
      .catch(() => {});
  }
  return response;
};

const authHeaders = (): HeadersInit => ({
  "Content-Type": "application/json",
  "X-CSRF-Token": localStorage.getItem("CSRF") ?? "",
  "Accept-Language": getAcceptLanguageHeader(),
});

// ---------------------------------------------------------------------------
// GET

export const getFromOpenElisServer = (
  endPoint: string,
  callback: ApiCallback,
  signal: AbortSignal | null = null,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "GET",
    signal: signal ?? undefined,
    headers: { "Accept-Language": getAcceptLanguageHeader() },
  })
    .then(async (response) => {
      const contentType = response.headers.get("content-type");
      if (contentType?.includes("application/json")) {
        callback(await response.json());
      } else {
        callback(undefined);
      }
    })
    .catch((error: Error) => {
      if (error.name === "AbortError") return;
      console.error(error);
      callback(undefined);
    });
};

/**
 * @deprecated Synchronous XHR blocks the main thread and is discouraged.
 * Use `getFromOpenElisServer` or `getFromOpenElisServerV2` instead.
 */
export const getFromOpenElisServerSync = (
  endPoint: string,
  callback: ApiCallback,
): unknown => {
  const request = new XMLHttpRequest();
  request.open("GET", config.serverBaseUrl + endPoint, false);
  request.setRequestHeader("credentials", "include");
  request.setRequestHeader("Accept-Language", getAcceptLanguageHeader());
  request.send();
  return callback(JSON.parse(request.response as string));
};

export const getFromOpenElisServerV2 = (url: string): Promise<unknown> => {
  return new Promise((resolve, reject) => {
    getFromOpenElisServer(url, (res) => {
      if (res !== undefined) {
        resolve(res);
      } else {
        reject("Failed to fetch data");
      }
    });
  });
};

// ---------------------------------------------------------------------------
// POST

export const postToOpenElisServer = <P = unknown>(
  endPoint: string,
  payLoad: Payload,
  callback: StatusCallbackWithParams<P>,
  extraParams?: P,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "POST",
    headers: authHeaders(),
    body: normalizePayload(payLoad),
  })
    .then(handleSessionError)
    .then((response) => response.status)
    .then((status) => callback(status, extraParams))
    .catch((error: Error) => {
      console.error(error);
      callback(0, extraParams);
    });
};

export const postToOpenElisServerFullResponse = <P = unknown>(
  endPoint: string,
  payLoad: Payload,
  callback: ResponseCallback<P>,
  extraParams?: P,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "POST",
    headers: authHeaders(),
    body: normalizePayload(payLoad),
  })
    .then(handleSessionError)
    .then((response) => callback(response, extraParams))
    .catch((error: Error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const postToOpenElisServerFormData = <P = unknown>(
  endPoint: string,
  formData: FormData,
  callback: StatusCallbackWithParams<P>,
  extraParams?: P,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "POST",
    headers: {
      "X-CSRF-Token": localStorage.getItem("CSRF") ?? "",
      "Accept-Language": getAcceptLanguageHeader(),
    },
    body: formData,
  })
    .then(handleSessionError)
    .then((response) => response.status)
    .then((status) => callback(status, extraParams))
    .catch((error: Error) => {
      console.error(error);
      callback(0, extraParams);
    });
};

export const postToOpenElisServerJsonResponse = <T = unknown, P = unknown>(
  endPoint: string,
  payLoad: Payload,
  callback: ApiCallbackWithParams<T | ApiErrorResponse, P>,
  extraParams?: P,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "POST",
    headers: authHeaders(),
    body: normalizePayload(payLoad),
  })
    .then(handleSessionError)
    .then(async (response) => {
      if (!response.ok) {
        const errorJson: Record<string, unknown> = await response.json();
        return callback(
          {
            ...errorJson,
            status: response.status,
            statusCode: response.status,
            statusText: response.statusText,
          } as ApiErrorResponse,
          extraParams,
        );
      }
      callback(await response.json(), extraParams);
    })
    .catch((error: Error) => {
      console.error("postToOpenElisServerJsonResponse error:", error);
      callback(
        { error: error.message, message: error.message, status: 0 },
        extraParams,
      );
    });
};

export const postToOpenElisServerForBlob = (
  endPoint: string,
  payLoad: Payload,
  callback: (blob: Blob, response: Response) => void,
  errorCallback?: (error: Error) => void,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "POST",
    headers: authHeaders(),
    body: normalizePayload(payLoad),
  })
    .then(handleSessionError)
    .then(async (response) => {
      if (!response.ok)
        throw new Error(`HTTP error! status: ${response.status}`);
      const blob = await response.blob();
      callback(blob, response);
    })
    .catch((error: Error) => {
      console.error(error);
      errorCallback?.(error);
    });
};

export const postToOpenElisServerForPDF = (
  endPoint: string,
  payLoad: Payload,
  callback: (success: boolean, blob?: Blob) => void,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "POST",
    headers: authHeaders(),
    body: normalizePayload(payLoad),
  })
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
    .catch((error: Error) => {
      callback(false);
      console.error(error);
    });
};

// ---------------------------------------------------------------------------
// PUT

export const putToOpenElisServer = (
  endPoint: string,
  payLoad: Payload | null,
  callback: StatusCallback,
): void => {
  const options: RequestInit = {
    credentials: "include",
    method: "PUT",
    headers: authHeaders(),
  };
  if (payLoad) options.body = normalizePayload(payLoad);

  fetch(config.serverBaseUrl + endPoint, options)
    .then(handleSessionError)
    .then((response) => response.status)
    .then((status) => callback(status))
    .catch((error: Error) => {
      console.error(error);
      callback(0);
    });
};

export const putToOpenElisServerFullResponse = <P = unknown>(
  endPoint: string,
  payLoad: Payload,
  callback: ResponseCallback<P>,
  extraParams?: P,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "PUT",
    headers: authHeaders(),
    body: normalizePayload(payLoad),
  })
    .then(handleSessionError)
    .then((response) => callback(response, extraParams))
    .catch((error: Error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

// ---------------------------------------------------------------------------
// DELETE

export const deleteFromOpenElisServer = (
  endPoint: string,
  callback: StatusCallback,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "DELETE",
    headers: authHeaders(),
  })
    .then(handleSessionError)
    .then((response) => response.status)
    .then((status) => callback(status))
    .catch((error: Error) => {
      console.error(error);
      callback(0);
    });
};

export const deleteFromOpenElisServerFullResponse = <P = unknown>(
  endPoint: string,
  callback: ResponseCallback<P>,
  extraParams?: P,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "DELETE",
    headers: authHeaders(),
  })
    .then(handleSessionError)
    .then((response) => callback(response, extraParams))
    .catch((error: Error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

// ---------------------------------------------------------------------------
// PATCH

export const patchToOpenElisServerJsonResponse = <T = unknown, P = unknown>(
  endPoint: string,
  payLoad: Payload,
  callback: ApiCallbackWithParams<T, P>,
  extraParams?: P,
): void => {
  fetch(config.serverBaseUrl + endPoint, {
    credentials: "include",
    method: "PATCH",
    headers: authHeaders(),
    body: normalizePayload(payLoad),
  })
    .then(handleSessionError)
    .then((response) => {
      if (!response.ok)
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      return response.json();
    })
    .then((json: T) => callback(json, extraParams))
    .catch((error: Error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

// ---------------------------------------------------------------------------
// Auth / Roles

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
} as const;

export type Role = (typeof Roles)[keyof typeof Roles];

export const hasRole = (
  userSessionDetails: UserSessionDetails | null | undefined,
  role: Role,
): boolean => {
  if (!userSessionDetails?.roles) return false;
  return userSessionDetails.roles.includes(role);
};

// ---------------------------------------------------------------------------
// Formatting utilities

export const convertAlphaNumLabNumForDisplay = (
  labNumber: string | null | undefined,
): string | null | undefined => {
  if (!labNumber) return labNumber;
  if (labNumber.length > 15) {
    console.warn("labNumber is not alphanumeric (too long), ignoring format");
    return labNumber;
  }

  const labNumberParts = labNumber.split("-");
  const isAnalysisLabNumber = labNumberParts.length > 1;
  let labNumberForDisplay: string;
  const base = labNumberParts[0];

  if (base.length < 8) {
    labNumberForDisplay = base.slice(0, 2);
    if (base.length > 2) labNumberForDisplay += "-" + base.slice(2);
  } else {
    labNumberForDisplay = base.slice(0, 2) + "-";
    if (base.length > 8) {
      labNumberForDisplay += base.slice(2, base.length - 6) + "-";
    }
    labNumberForDisplay +=
      base.slice(base.length - 6, base.length - 3) +
      "-" +
      base.slice(base.length - 3);
  }

  if (isAnalysisLabNumber) {
    labNumberForDisplay += "-" + labNumberParts[1];
  }
  return labNumberForDisplay.toUpperCase();
};

export function encodeDate(dateString: string): string {
  if (typeof dateString === "string" && dateString.trim() !== "") {
    return dateString.split("/").map(encodeURIComponent).join("%2F");
  }
  return "";
}

export function getDifferenceInDays(date1: string, date2: string): number {
  function parseDate(dateStr: string): Date {
    const [day, month, year] = dateStr.split("/").map(Number);
    return new Date(year, month - 1, day);
  }

  function correctDate(d: string): string {
    const parts = d.split("/");
    if (parts[0].length === 4)
      return parts[1] + "/" + parts[0] + "/" + parts[2];
    return d;
  }

  const firstDate = parseDate(correctDate(date1));
  const secondDate = parseDate(correctDate(date2));
  const ms = secondDate.getTime() - firstDate.getTime();
  return ms / (1000 * 60 * 60 * 24);
}

export function formatTimestamp(timestamp: number): string {
  const date = new Date(timestamp * 1000);
  const hours = date.getUTCHours();
  const minutes = date.getUTCMinutes();
  const ampm = hours >= 12 ? "PM" : "AM";
  const fh = (hours % 12 || 12).toString().padStart(2, "0");
  const fm = minutes.toString().padStart(2, "0");
  const fd = date.getUTCDate().toString().padStart(2, "0");
  const fmo = (date.getUTCMonth() + 1).toString().padStart(2, "0");
  const fy = date.getUTCFullYear();
  return `${fh}:${fm} ${ampm}; ${fd}/${fmo}/${fy}`;
}

export function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  const output = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; i++) {
    output[i] = rawData.charCodeAt(i);
  }
  return output;
}

export const toBase64 = (file: File): Promise<string | ArrayBuffer | null> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
  });
