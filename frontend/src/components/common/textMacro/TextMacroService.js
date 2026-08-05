import config from "../../../config.json";

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
