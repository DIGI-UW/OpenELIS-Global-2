import config from "../../../config.json";

const request = async (path, options = {}) => {
  const response = await fetch(config.serverBaseUrl + path, {
    credentials: "include",
    ...options,
    headers: {
      "Accept-Language":
        localStorage.getItem("locale") || navigator.language || "en",
      ...(options.body && !(options.body instanceof FormData)
        ? { "Content-Type": "application/json" }
        : {}),
      ...(options.method && options.method !== "GET"
        ? { "X-CSRF-Token": localStorage.getItem("CSRF") }
        : {}),
      ...options.headers,
    },
  });
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : await response.text();
  if (!response.ok) {
    const error = new Error(body?.message || response.statusText);
    error.status = response.status;
    error.payload = body;
    throw error;
  }
  return body;
};

export const getReferencePage = (resource, query, signal) =>
  request(`/rest/microbiology/admin/reference/${resource}?${query}`, {
    signal,
  });

export const getReferenceItem = (resource, id, signal) =>
  request(
    `/rest/microbiology/admin/reference/${resource}/${encodeURIComponent(id)}`,
    { signal },
  );

export const getReferenceOptions = (resource, signal) =>
  request(
    `/rest/microbiology/admin/reference/options/${encodeURIComponent(resource)}`,
    { signal },
  );

export const saveReference = (resource, value) =>
  request(
    `/rest/microbiology/admin/reference/${resource}${value.id ? `/${value.id}` : ""}`,
    {
      method: value.id ? "PUT" : "POST",
      body: JSON.stringify(value),
    },
  );

export const setReferenceActive = (resource, id, active) =>
  request(
    `/rest/microbiology/admin/reference/${resource}/${encodeURIComponent(id)}/active?active=${active}`,
    { method: "PATCH" },
  );

export const getAstPanel = (id, signal) =>
  request(
    `/rest/microbiology/admin/reference/ast-panels/${encodeURIComponent(id)}`,
    { signal },
  );

export const publishAstPanel = (panel) =>
  request(
    panel.id
      ? `/rest/microbiology/admin/reference/ast-panels/${encodeURIComponent(panel.id)}/versions`
      : "/rest/microbiology/admin/reference/ast-panels",
    { method: "POST", body: JSON.stringify(panel) },
  );

export const getBreakpointStandards = (query, signal) =>
  request(`/rest/microbiology/admin/breakpoints/standards?${query}`, {
    signal,
  });

export const getBreakpointRules = (standardId, query, signal) =>
  request(
    `/rest/microbiology/admin/breakpoints/standards/${encodeURIComponent(standardId)}/rules?${query}`,
    { signal },
  );

export const activateBreakpointStandard = (standardId, effectiveDate) =>
  request(
    `/rest/microbiology/admin/breakpoints/standards/${encodeURIComponent(standardId)}/activate?effectiveDate=${encodeURIComponent(effectiveDate)}`,
    { method: "POST" },
  );

export const archiveBreakpointStandard = (standardId) =>
  request(
    `/rest/microbiology/admin/breakpoints/standards/${encodeURIComponent(standardId)}/archive`,
    { method: "POST" },
  );

export const previewBreakpointImport = (csv) =>
  request("/rest/microbiology/admin/breakpoints/imports/preview", {
    method: "POST",
    headers: { "Content-Type": "text/csv" },
    body: csv,
  });

export const applyBreakpointImport = (previewToken) =>
  request(
    `/rest/microbiology/admin/breakpoints/imports/${encodeURIComponent(previewToken)}/apply`,
    { method: "POST" },
  );
