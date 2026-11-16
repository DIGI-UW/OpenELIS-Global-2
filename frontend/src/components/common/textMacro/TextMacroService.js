import {
  fetchFromOpenElisServer,
  getFromOpenElisServerForBlob,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const sendJson = (send, path, body) =>
  new Promise((resolve, reject) =>
    send(path, JSON.stringify(body), (response) =>
      response?.error ? reject(response) : resolve(response),
    ),
  );

export const getTextMacros = (context, signal) =>
  fetchFromOpenElisServer(
    `/rest/text-macros?context=${encodeURIComponent(context)}`,
    signal,
  );

export const getAdminMacroPage = (query, signal) =>
  fetchFromOpenElisServer(`/rest/text-macros/admin?${query}`, signal);

export const getAdminMacro = (id, signal) =>
  fetchFromOpenElisServer(
    `/rest/text-macros/admin/${encodeURIComponent(id)}`,
    signal,
  );

export const saveAdminMacro = (macro) =>
  sendJson(
    macro.id
      ? putToOpenElisServerJsonResponse
      : postToOpenElisServerJsonResponse,
    `/rest/text-macros/admin${macro.id ? `/${encodeURIComponent(macro.id)}` : ""}`,
    {
      code: macro.code,
      expansionText: macro.expansionText,
      category: macro.category,
      contexts: macro.contexts,
      active: macro.active,
    },
  );

export const bulkAdminMacros = ({ ids, action }) =>
  sendJson(postToOpenElisServerJsonResponse, "/rest/text-macros/admin/bulk", {
    ids,
    action,
  });

export const exportAdminMacros = () =>
  new Promise((resolve, reject) =>
    getFromOpenElisServerForBlob(
      "/rest/text-macros/admin/export",
      (blob, response) =>
        resolve({
          blob,
          filename:
            response.headers
              .get("Content-Disposition")
              ?.match(/filename="?([^";]+)"?/i)?.[1] ||
            "openelis-text-macros.csv",
        }),
      reject,
    ),
  );
