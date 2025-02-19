import config from "../../config.json";

export const getFromOpenElisServerAsync = async (endPoint) => {
  try {
    const response = await fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: "GET",
    });

    const contentType = response.headers.get("content-type");
    if (contentType?.includes("application/json")) {
      return await response.json();
    }
    return undefined;
  } catch (error) {
    console.error(error);
    throw error;
  }
};

export const getFromOpenElisServer = (endPoint, callback) => {
  getFromOpenElisServerAsync(endPoint)
    .then((data) => callback(data))
    .catch((error) => {
      console.error(error);
      callback(null, error);
    });
};

export const postToOpenElisServerAsync = async (
  endPoint,
  payLoad,
  extraParams,
) => {
  try {
    const response = await fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: payLoad,
    });
    return { status: response.status, extraParams };
  } catch (error) {
    console.error(error);
    throw error;
  }
};

export const postToOpenElisServer = (
  endPoint,
  payLoad,
  callback,
  extraParams,
) => {
  postToOpenElisServerAsync(endPoint, payLoad, extraParams)
    .then((result) => callback(result.status, result.extraParams))
    .catch((error) => {
      console.error(error);
      callback(null, error);
    });
};

export const postToOpenElisServerFullResponseAsync = async (
  endPoint,
  payLoad,
  extraParams,
) => {
  try {
    const response = await fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: payLoad,
    });
    return { response, extraParams };
  } catch (error) {
    console.error(error);
    throw error;
  }
};

export const postToOpenElisServerFullResponse = (
  endPoint,
  payLoad,
  callback,
  extraParams,
) => {
  postToOpenElisServerFullResponseAsync(endPoint, payLoad, extraParams)
    .then((result) => callback(result.response, result.extraParams))
    .catch((error) => {
      console.error(error);
      callback(null, error);
    });
};

export const postToOpenElisServerFormDataAsync = async (
  endPoint,
  formData,
  extraParams,
) => {
  try {
    const response = await fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: "POST",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: formData,
    });
    return { status: response.status, extraParams };
  } catch (error) {
    console.error(error);
    throw error;
  }
};

export const postToOpenElisServerFormData = (
  endPoint,
  formData,
  callback,
  extraParams,
) => {
  postToOpenElisServerFormDataAsync(endPoint, formData, extraParams)
    .then((result) => callback(result.status, result.extraParams))
    .catch((error) => {
      console.error(error);
      callback(null, error);
    });
};
export const postToOpenElisServerJsonResponseAsync = async (
  endPoint,
  payLoad,
  extraParams,
) => {
  try {
    const response = await fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: payLoad,
    });
    const json = await response.json();
    return { json, extraParams };
  } catch (error) {
    console.error(error);
    throw error;
  }
};

export const postToOpenElisServerJsonResponse = (
  endPoint,
  payLoad,
  callback,
  extraParams,
) => {
  postToOpenElisServerJsonResponseAsync(endPoint, payLoad, extraParams)
    .then((result) => callback(result.json, result.extraParams))
    .catch((error) => {
      console.error(error);
      callback(null, error);
    });
};

export const getFromOpenElisServerSync = (endPoint, callback) => {
  const request = new XMLHttpRequest();
  request.open("GET", config.serverBaseUrl + endPoint, false);
  request.setRequestHeader("credentials", "include");
  request.send();
  return callback(JSON.parse(request.response));
};

export const postToOpenElisServerForPDFAsync = async (endPoint, payLoad) => {
  try {
    const response = await fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: payLoad,
    });
    const blob = await response.blob();

    const link = document.createElement("a");
    link.href = window.URL.createObjectURL(blob, { type: "application/pdf" });
    link.target = "_blank";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    return { success: true, blob };
  } catch (error) {
    console.error(error);
    return { success: false };
  }
};

export const postToOpenElisServerForPDF = (endPoint, payLoad, callback) => {
  postToOpenElisServerForPDFAsync(endPoint, payLoad)
    .then((result) => callback(result.success, result.blob))
    .catch((error) => {
      console.error(error);
      callback(false);
    });
};

export const putToOpenElisServerAsync = async (endPoint, payLoad) => {
  const options = {
    credentials: "include",
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-Token": localStorage.getItem("CSRF"),
    },
    ...(payLoad && { body: payLoad }),
  };

  try {
    const response = await fetch(config.serverBaseUrl + endPoint, options);
    return response.status;
  } catch (error) {
    console.error(error);
    throw error;
  }
};

export const putToOpenElisServer = (endPoint, payLoad, callback) => {
  putToOpenElisServerAsync(endPoint, payLoad)
    .then((status) => callback(status))
    .catch((error) => console.error(error));
};

export const hasRole = (userSessionDetails, role) => {
  return userSessionDetails.roles && userSessionDetails.roles.includes(role);
};

// Helper functions (unchanged)
export const convertAlphaNumLabNumForDisplay = (labNumber) => {
  if (!labNumber) {
    return labNumber;
  }
  if (labNumber.length > 15) {
    console.warn("labNumber is not alphanumeric (too long), ignoring format");
    return labNumber;
  }
  let labNumberParts = labNumber.split("-");
  let isAnalysisLabNumber = labNumberParts.length > 1;
  let labNumberForDisplay = labNumberParts[0];
  if (labNumberParts[0].length < 8) {
    labNumberForDisplay = labNumberParts[0].slice(0, 2);
    if (labNumberParts[0].length > 2) {
      labNumberForDisplay = labNumberForDisplay + "-";
      labNumberForDisplay = labNumberForDisplay + labNumberParts[0].slice(2);
    }
  } else {
    labNumberForDisplay = labNumberParts[0].slice(0, 2) + "-";
    if (labNumberParts[0].length > 8) {
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

  function parseDate(dateStr) {
    const [day, month, year] = dateStr.split("/").map(Number);
    return new Date(year, month - 1, day);
  }

  function correctDate(firstDate) {
    let dateParts = firstDate.split("/");
    if (dateParts[0].length === 4) {
      return dateParts[1] + "/" + dateParts[0] + "/" + dateParts[2];
    }
    return firstDate;
  }

  const firstDate = parseDate(correctDate(date1));
  const secondDate = parseDate(correctDate(date2));

  const timeDifference = secondDate - firstDate;
  const millisecondsPerDay = 1000 * 60 * 60 * 24;
  const dayDifference = timeDifference / millisecondsPerDay;

  return dayDifference;
}

export function formatTimestamp(timestamp) {
  const date = new Date(timestamp * 1000);
  const hours = date.getUTCHours();
  const minutes = date.getUTCMinutes();
  const day = date.getUTCDate();
  const month = date.getUTCMonth() + 1;
  const year = date.getUTCFullYear();

  const ampm = hours >= 12 ? "PM" : "AM";
  const formattedHours = (hours % 12 || 12).toString().padStart(2, "0");
  const formattedMinutes = minutes.toString().padStart(2, "0");

  const formattedDay = day.toString().padStart(2, "0");
  const formattedMonth = month.toString().padStart(2, "0");

  return `${formattedHours}:${formattedMinutes} ${ampm}; ${formattedDay}/${formattedMonth}/${year}`;
}

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
  RECEPTION: "Reception",
  RESULTS: "Results",
  VALIDATION: "Validation",
  REPORTS: "Reports",
  PATHOLOGIST: "Pathologist",
};
