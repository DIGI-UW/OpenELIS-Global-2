import config from "../../config.json";

const requestSearch = (query) => {
  const params = new URLSearchParams();
  ["from", "to"].forEach((key) => params.set(key, String(query[key])));
  ["specimen", "organism", "origin", "significance"].forEach((key) =>
    [...(query[key] || [])]
      .sort()
      .forEach((value) => params.append(key, String(value))),
  );
  ["dedup", "page", "pageSize"].forEach((key) =>
    params.set(key, String(query[key])),
  );
  return params.toString();
};

const request = async (path, options = {}) => {
  const response = await fetch(config.serverBaseUrl + path, {
    credentials: "include",
    ...options,
    headers: {
      "Accept-Language":
        localStorage.getItem("locale") || navigator.language || "en",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.method && options.method !== "GET"
        ? { "X-CSRF-Token": localStorage.getItem("CSRF") }
        : {}),
      ...options.headers,
    },
  });
  if (!response.ok) {
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
      ? await response.json()
      : await response.text();
    const error = new Error(payload?.message || response.statusText);
    error.status = response.status;
    error.code = payload?.error;
    error.payload = payload;
    throw error;
  }
  return response;
};

export const getWhonetPreview = async (query) => {
  const response = await request(
    `/rest/microbiology/whonet/preview?${requestSearch(query)}`,
  );
  return response.json();
};

export const getWhonetFilterOptions = async (query) => {
  const params = new URLSearchParams({ from: query.from, to: query.to });
  const response = await request(
    `/rest/microbiology/whonet/filter-options?${params.toString()}`,
  );
  return response.json();
};

const attachmentFilename = (response) => {
  const disposition = response.headers.get("Content-Disposition") || "";
  const match = disposition.match(/filename="?([^";]+)"?/i);
  return match?.[1] || "WHONET_export.csv";
};

export const generateWhonetExport = async (query) => {
  const response = await request("/rest/microbiology/whonet/exports", {
    method: "POST",
    body: JSON.stringify(query),
  });
  return {
    blob: await response.blob(),
    filename: attachmentFilename(response),
  };
};
