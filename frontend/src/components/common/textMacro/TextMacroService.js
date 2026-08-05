import config from "../../../config.json";
import { filenameFromContentDisposition } from "../../utils/downloadAttachment";

const requestResponse = async (path, options = {}) => {
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
  return response;
};

const request = async (path, options = {}) => {
  const response = await requestResponse(path, options);
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

export const getTextMacros = (context, signal) =>
  request(`/rest/text-macros?context=${encodeURIComponent(context)}&limit=50`, {
    signal,
  });

export const getAdminMacroPage = (query, signal) =>
  request(`/rest/text-macros/admin?${query}`, { signal });

export const getAdminMacro = (id, signal) =>
  request(`/rest/text-macros/admin/${encodeURIComponent(id)}`, { signal });

export const saveAdminMacro = (macro) =>
  request(
    `/rest/text-macros/admin${macro.id ? `/${encodeURIComponent(macro.id)}` : ""}`,
    {
      method: macro.id ? "PUT" : "POST",
      body: JSON.stringify({
        code: macro.code,
        expansionText: macro.expansionText,
        contexts: macro.contexts,
        active: macro.active,
      }),
    },
  );

export const bulkAdminMacros = ({ ids, action }) =>
  request("/rest/text-macros/admin/bulk", {
    method: "POST",
    body: JSON.stringify({ ids, action }),
  });

export const exportAdminMacros = async () => {
  const response = await requestResponse("/rest/text-macros/admin/export");
  if (!response.ok) {
    const error = new Error(response.statusText);
    error.status = response.status;
    throw error;
  }
  return {
    blob: await response.blob(),
    filename: filenameFromContentDisposition(
      response.headers.get("Content-Disposition"),
      "openelis-text-macros.csv",
    ),
  };
};
