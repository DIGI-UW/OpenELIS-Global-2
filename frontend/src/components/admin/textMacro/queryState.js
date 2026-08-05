import { TEXT_MACRO_CONTEXTS } from "./textMacroConfig";

const CONTEXTS = new Set(["all", ...TEXT_MACRO_CONTEXTS]);
const STATUSES = new Set(["active", "inactive", "all"]);
const SORTS = new Set(["code:asc", "code:desc", "updated:asc", "updated:desc"]);
const PAGE_SIZES = new Set([10, 20, 50, 100]);

export const DEFAULT_MACRO_LIBRARY_QUERY = Object.freeze({
  q: "",
  context: "all",
  status: "active",
  sort: "code:asc",
  page: 1,
  pageSize: 20,
  edit: "",
});

export const parseMacroLibraryQuery = (search) => {
  const params = new URLSearchParams(search);
  const page = Number(params.get("page"));
  const pageSize = Number(params.get("pageSize"));
  const context = params.get("context") || "all";
  const status = params.get("status") || "active";
  const sort = params.get("sort") || "code:asc";
  return {
    q: params.get("q") || "",
    context: CONTEXTS.has(context) ? context : "all",
    status: STATUSES.has(status) ? status : "active",
    sort: SORTS.has(sort) ? sort : "code:asc",
    page: Number.isInteger(page) && page > 0 ? page : 1,
    pageSize: PAGE_SIZES.has(pageSize) ? pageSize : 20,
    edit: params.get("edit") || "",
  };
};

export const buildMacroLibraryQuery = (query) => {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== "" && value !== null && value !== undefined) {
      params.set(key, String(value));
    }
  });
  return params.toString();
};

export const updateMacroLibraryQuery = (current, updates) => {
  const filterChanged = ["q", "context", "status", "sort", "pageSize"].some(
    (key) => key in updates && updates[key] !== current[key],
  );
  return {
    ...current,
    ...updates,
    page:
      filterChanged && !("page" in updates) ? 1 : updates.page || current.page,
  };
};

export const buildMacroAdminRequestQuery = (query) => {
  const { edit, ...requestQuery } = query;
  return buildMacroLibraryQuery(requestQuery);
};
