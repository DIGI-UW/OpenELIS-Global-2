import config from "../../config.json";

export const getFromOpenElisServer = async (endPoint, callback) => {
  try {
    const response = await fetch(config.serverBaseUrl + endPoint, {
      credentials: "include",
      method: "GET",
    });

    console.debug("checking response");
    const contentType = response.headers.get("content-type");

    if (contentType && contentType.indexOf("application/json") !== -1) {
      const jsonResp = await response.json();
      callback(jsonResp);
    } else {
      callback();
    }
  } catch (error) {
    console.error(error);
  }
};

export const postToOpenElisServer = async (
  endPoint,
  payLoad,
  callback,
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

    const status = response.status;
    callback(status, extraParams);
  } catch (error) {
    console.error(error);
  }
};

export const postToOpenElisServerFullResponse = async (
  endPoint,
  payLoad,
  callback,
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

    callback(response, extraParams);
  } catch (error) {
    console.error(error);
  }
};

export const postToOpenElisServerFormData = async (
  endPoint,
  formData,
  callback,
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

    const status = response.status;
    callback(status, extraParams);
  } catch (error) {
    console.error(error);
  }
};

export const postToOpenElisServerJsonResponse = async (
  endPoint,
  payLoad,
  callback,
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
    callback(json, extraParams);
  } catch (error) {
    console.error(error);
  }
};

export const getFromOpenElisServerSync = (endPoint, callback) => {
  const request = new XMLHttpRequest();
  request.open("GET", config.serverBaseUrl + endPoint, false);
  request.setRequestHeader("credentials", "include");
  request.send();
  return callback(JSON.parse(request.response));
};

export const postToOpenElisServerForPDF = async (
  endPoint,
  payLoad,
  callback,
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

    const blob = await response.blob();
    callback(true, blob);

    const link = document.createElement("a");
    link.href = window.URL.createObjectURL(blob, { type: "application/pdf" });
    link.target = "_blank";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    callback(false);
    console.error(error);
  }
};

export const putToOpenElisServer = async (endPoint, payLoad, callback) => {
  try {
    const options = {
      credentials: "include",
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
    };

    if (payLoad) {
      options.body = payLoad;
    }

    const response = await fetch(config.serverBaseUrl + endPoint, options);
    const status = response.status;
    callback(status);
  } catch (error) {
    console.error(error);
  }
};

export const hasRole = (userSessionDetails, role) => {
  return userSessionDetails.roles && userSessionDetails.roles.includes(role);
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

export const convertAlphaNumLabNumForDisplay = (labNumber) => {
  if (!labNumber) {
    return labNumber;
  }
  if (labNumber.length > 15) {
    console.warn("labNumber is not alphanumeric (too long), ignoring format");
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
  RECEPTION: "Reception",
  RESULTS: "Results",
  VALIDATION: "Validation",
  REPORTS: "Reports",
  PATHOLOGIST: "Pathologist",
};
