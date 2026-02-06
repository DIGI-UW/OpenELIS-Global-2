import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../utils/Utils";

const BASE_PATH = "/rest/notebook";

const promisify = (fn, ...args) => {
  return new Promise((resolve, reject) => {
    fn(...args, (response) => {
      if (response && response.error) {
        reject(new Error(response.message || response.error));
      } else {
        resolve(response);
      }
    });
  });
};

const get = (endpoint) => {
  return promisify(getFromOpenElisServer, `${BASE_PATH}${endpoint}`);
};

const post = (endpoint, data) => {
  return new Promise((resolve, reject) => {
    postToOpenElisServerJsonResponse(
      `${BASE_PATH}${endpoint}`,
      JSON.stringify(data),
      (json) => {
        if (json && (json.status >= 400 || json.statusCode >= 400)) {
          if (json.errors && typeof json.errors === "object") {
            const errorMessages = Object.entries(json.errors)
              .map(([field, message]) => `${field}: ${message}`)
              .join(", ");
            reject(new Error(errorMessages));
            return;
          }
          reject(
            new Error(
              json.message ||
                json.error ||
                `Request failed with status ${json.status || json.statusCode}`,
            ),
          );
        } else {
          resolve(json);
        }
      },
      null,
    );
  });
};

const put = (endpoint, data) => {
  return new Promise((resolve, reject) => {
    putToOpenElisServer(
      `${BASE_PATH}${endpoint}`,
      JSON.stringify(data),
      (status) => {
        if (status >= 200 && status < 300) {
          resolve({ success: true });
        } else {
          reject(new Error(`Failed to update: HTTP ${status}`));
        }
      },
    );
  });
};
