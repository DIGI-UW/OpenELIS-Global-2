import config from "../../config.json";

/**
 * Get the current locale from localStorage for API requests.
 * Falls back to browser language or 'en' if not set.
 */
const getAcceptLanguageHeader = () => {
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
  intl,
  payload,
  fallbackId,
  fallbackValues = {},
) => {
  const key = payload?.messageKey || payload?.errorKey;
  const keyArgs = payload?.messageArgs || payload?.errorArgs || {};
  if (key) {
    return intl.formatMessage({ id: key }, keyArgs);
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
  return intl.formatMessage({ id: fallbackId }, fallbackValues);
};

const handleSessionError = (response) => {
  if (response.status === 403) {
    response
      .clone()
      .json()
      .then((body) => {
        if (body && body.message && body.message.includes("CSRF")) {
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

export const getFromOpenElisServer = (endPoint, callback, signal = null) => {
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
          callback(jsonResp);
        });
      } else {
        callback();
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

export const postToOpenElisServer = (
  endPoint,
  payLoad,
  callback,
  extraParams,
) => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad,
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

export const postToOpenElisServerFullResponse = (
  endPoint,
  payLoad,
  callback,
  extraParams,
) => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad,
    },
  )
    .then(handleSessionError)
    .then((response) => callback(response, extraParams))
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const postToOpenElisServerFormData = (
  endPoint,
  formData,
  callback,
  extraParams,
) => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      credentials: "include",
      method: "POST",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
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

export const postToOpenElisServerJsonResponse = (
  endPoint,
  payLoad,
  callback,
  extraParams,
) => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad,
    },
  )
    .then(handleSessionError)
    .then((response) => {
      // Check if response is ok (status 200-299)
      if (!response.ok) {
        // For error responses, try to parse JSON error message
        return response.json().then((errorJson) => {
          // Include status code in error response for better error handling
          return {
            ...errorJson,
            status: response.status,
            statusCode: response.status,
            statusText: response.statusText,
          };
        });
      }
      // For successful responses, parse JSON normally
      return response.json();
    })
    .then((json) => {
      callback(json, extraParams);
    })
    .catch((error) => {
      console.error("postToOpenElisServerJsonResponse error:", error);
      // Pass error to callback so calling code can handle it
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

//provides Synchronous calls to the api
export const getFromOpenElisServerSync = (endPoint, callback) => {
  const request = new XMLHttpRequest();
  request.open("GET", config.serverBaseUrl + endPoint, false);
  request.setRequestHeader("credentials", "include");
  request.setRequestHeader("Accept-Language", getAcceptLanguageHeader());
  request.send();
  // if (request.response.url.includes("LoginPage")) {
  //     throw "No Login Session";
  // }
  return callback(JSON.parse(request.response));
};

export const postToOpenElisServerForBlob = (
  endPoint,
  payLoad,
  callback,
  errorCallback,
) => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad,
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

export const postToOpenElisServerForPDF = (endPoint, payLoad, callback) => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad,
    },
  )
    .then(handleSessionError)
    .then((response) => response.blob())
    .then((blob) => {
      callback(true, blob);
      let link = document.createElement("a");
      link.href = window.URL.createObjectURL(blob, { type: "application/pdf" });
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

export const putToOpenElisServer = (endPoint, payLoad, callback) => {
  // Build the request options
  let options = {
    // includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
      "Accept-Language": getAcceptLanguageHeader(),
    },
  };

  // Include the body only if payLoad is provided
  if (payLoad) {
    options.body = payLoad;
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

export const putToOpenElisServerFullResponse = (
  endPoint,
  payLoad,
  callback,
  extraParams,
) => {
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
    .then((response) => callback(response, extraParams))
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const deleteFromOpenElisServer = (endPoint, callback) => {
  fetch(config.serverBaseUrl + endPoint, {
    // includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
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

export const deleteFromOpenElisServerFullResponse = (
  endPoint,
  callback,
  extraParams,
) => {
  fetch(config.serverBaseUrl + endPoint, {
    // includes the browser sessionId in the Header for Authentication on the backend server
    credentials: "include",
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
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

export const hasRole = (userSessionDetails, role) => {
  if (!userSessionDetails || !userSessionDetails.roles) {
    return false;
  }
  return userSessionDetails.roles.includes(role);
};

export const hasPrivilege = (userSessionDetails, privilege) => {
  if (!userSessionDetails) {
    return false;
  }
  if (
    userSessionDetails.roles &&
    userSessionDetails.roles.includes(Roles.GLOBAL_ADMIN)
  ) {
    return true;
  }
  if (!userSessionDetails.privileges) {
    return false;
  }
  return userSessionDetails.privileges.includes(privilege);
};

// this is complicated to enable it to format "smartly" as a person types
// possible rework could allow it to only format completed numbers

export const getFromOpenElisServerV2 = (url) => {
  return new Promise((resolve, reject) => {
    // Simulating the original callback-based function
    getFromOpenElisServer(url, (res) => {
      if (res) {
        resolve(res);
      } else {
        reject("Failed to fetch Subscription data");
      }
    });
  });
};

export const patchToOpenElisServerJsonResponse = (
  endPoint,
  payLoad,
  callback,
  extraParams,
) => {
  fetch(
    config.serverBaseUrl + endPoint,

    {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Accept-Language": getAcceptLanguageHeader(),
      },
      body: payLoad,
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
      callback(json, extraParams);
    })
    .catch((error) => {
      console.error(error);
      callback(undefined, extraParams);
    });
};

export const convertAlphaNumLabNumForDisplay = (labNumber) => {
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
  let labNumberParts = labNumber.split("-");
  let isAnalysisLabNumber = labNumberParts.length > 1;
  let labNumberForDisplay = labNumberParts[0];
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

export function encodeDate(dateString) {
  if (typeof dateString === "string" && dateString.trim() !== "") {
    return dateString.split("/").map(encodeURIComponent).join("%2F");
  } else {
    return "";
  }
}

export function getDifferenceInDays(date1, date2) {
  console.log("secondDate", date2);

  // Function to parse dates in DD/MM/YYYY format
  function parseDate(dateStr) {
    const [day, month, year] = dateStr.split("/").map(Number);
    return new Date(year, month - 1, day); // Months are 0-based in JavaScript Date
  }

  function correctDate(firstDate) {
    // "08/05/2024" the error is 08 is not day it is month and 05 is day
    let dateParts = firstDate.split("/");
    if (dateParts[0].length === 4) {
      return dateParts[1] + "/" + dateParts[0] + "/" + dateParts[2];
    }
    return firstDate;
  }

  // Convert the date strings to Date objects
  const firstDate = parseDate(correctDate(date1));
  const secondDate = parseDate(correctDate(date2));

  // Calculate the difference in time (milliseconds)
  const timeDifference = secondDate - firstDate;

  // Convert the time difference from milliseconds to days
  const millisecondsPerDay = 1000 * 60 * 60 * 24;
  const dayDifference = timeDifference / millisecondsPerDay;

  // Return the rounded difference in days
  return dayDifference;
}

export function formatTimestamp(timestamp) {
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
export function urlBase64ToUint8Array(base64String) {
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
};

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
  SITE_INFO_VIEW: "siteinfo:view",
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

export const toBase64 = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
  });
