export const DEFAULT_REFERENCE_QUERY = Object.freeze({
  q: "",
  status: "ALL",
  category: "",
  workflow: "",
  authority: "",
  organism: "",
  antibiotic: "",
  method: "",
  specimenTypeId: "",
  edit: "",
  sort: "name",
  page: 1,
  pageSize: 20,
});

const TEXT_KEYS = [
  "q",
  "status",
  "category",
  "workflow",
  "authority",
  "organism",
  "antibiotic",
  "method",
  "specimenTypeId",
  "edit",
  "sort",
];

const positiveInteger = (value, fallback) => {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
};

export const parseReferenceQuery = (search = "") => {
  const params = new URLSearchParams(search);
  const query = { ...DEFAULT_REFERENCE_QUERY };
  TEXT_KEYS.forEach((key) => {
    if (params.has(key)) query[key] = params.get(key) || "";
  });
  query.page = positiveInteger(params.get("page"), 1);
  const requestedPageSize = positiveInteger(params.get("pageSize"), 20);
  query.pageSize = [20, 50, 100].includes(requestedPageSize)
    ? requestedPageSize
    : 20;
  return query;
};

export const buildReferenceQuery = (query) => {
  const normalized = { ...DEFAULT_REFERENCE_QUERY, ...query };
  const params = new URLSearchParams();
  TEXT_KEYS.forEach((key) => {
    if (normalized[key]) params.set(key, String(normalized[key]));
  });
  params.set("page", String(positiveInteger(normalized.page, 1)));
  params.set("pageSize", String(positiveInteger(normalized.pageSize, 20)));
  return params.toString();
};

export const updateReferenceQuery = (current, updates) => {
  const next = { ...current, ...updates };
  const changedFilter = Object.keys(updates).some(
    (key) => !["page", "pageSize"].includes(key),
  );
  if (changedFilter && !Object.prototype.hasOwnProperty.call(updates, "page")) {
    next.page = 1;
  }
  return next;
};
