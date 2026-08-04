import {
  getFromOpenElisServer,
  postToOpenElisServerForBlob,
} from "../utils/Utils";

const requestSearch = (query) => {
  const params = new URLSearchParams();
  ["from", "to", "significance", "dedup", "page", "pageSize"].forEach((key) =>
    params.set(key, String(query[key])),
  );
  return params.toString();
};

export const getWhonetPreview = (query) =>
  new Promise((resolve) => {
    getFromOpenElisServer(
      `/rest/microbiology/whonet/preview?${requestSearch(query)}`,
      resolve,
    );
  });

const attachmentFilename = (response) => {
  const disposition = response.headers.get("Content-Disposition") || "";
  const match = disposition.match(/filename="?([^";]+)"?/i);
  return match?.[1] || "WHONET_export.csv";
};

export const generateWhonetExport = (query) =>
  new Promise((resolve, reject) => {
    postToOpenElisServerForBlob(
      "/rest/microbiology/whonet/exports",
      JSON.stringify(query),
      (blob, response) =>
        resolve({
          blob,
          filename: attachmentFilename(response),
          runId: response.headers.get("X-WHONET-Export-Run-Id") || "",
        }),
      reject,
    );
  });
