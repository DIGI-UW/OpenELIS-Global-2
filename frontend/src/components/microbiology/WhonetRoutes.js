export const MICROBIOLOGY_WHONET_PATH = "/Microbiology/whonet";
export const WHONET_PAGE_SIZES = [20, 50, 100];

const SIGNIFICANCE_VALUES = new Set([
  "CLINICALLY_SIGNIFICANT",
  "CONTAMINANT",
  "NORMAL_FLORA",
  "UNKNOWN",
]);
const DEDUP_POLICIES = new Set(["FIRST_ISOLATE_7_DAY", "NONE"]);
const STEPS = new Set(["configure", "preview"]);

const isoDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const previousCompleteMonth = (now) => {
  const from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const to = new Date(now.getFullYear(), now.getMonth(), 0);
  return { from: isoDate(from), to: isoDate(to) };
};

const validIsoDate = (value) =>
  typeof value === "string" &&
  /^\d{4}-\d{2}-\d{2}$/.test(value) &&
  !Number.isNaN(new Date(`${value}T00:00:00Z`).getTime()) &&
  new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value;

const positiveInteger = (value, fallback) => {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
};

const values = (value) =>
  (Array.isArray(value) ? value : value == null ? [] : [value])
    .map((item) => String(item).trim())
    .filter(Boolean);

const normalizedValues = (value, allowed) =>
  [
    ...new Set(values(value).filter((item) => !allowed || allowed.has(item))),
  ].sort();

const repeatedValues = (params, key, allowed) =>
  normalizedValues(params.getAll(key), allowed);

export const parseWhonetSearch = (search = "", now = new Date()) => {
  const params = new URLSearchParams(search);
  const defaults = previousCompleteMonth(now);
  const requestedSignificance = params.getAll("significance");
  const significance = requestedSignificance.includes("ALL")
    ? [...SIGNIFICANCE_VALUES]
    : normalizedValues(requestedSignificance, SIGNIFICANCE_VALUES);
  const dedup = params.get("dedup");
  const step = params.get("step");
  const requestedPageSize = positiveInteger(params.get("pageSize"), 20);
  return {
    from: validIsoDate(params.get("from")) ? params.get("from") : defaults.from,
    to: validIsoDate(params.get("to")) ? params.get("to") : defaults.to,
    specimen: repeatedValues(params, "specimen"),
    organism: repeatedValues(params, "organism"),
    origin: repeatedValues(params, "origin"),
    significance:
      significance.length > 0 ? significance : ["CLINICALLY_SIGNIFICANT"],
    dedup: DEDUP_POLICIES.has(dedup) ? dedup : "FIRST_ISOLATE_7_DAY",
    step: STEPS.has(step) ? step : "configure",
    page: positiveInteger(params.get("page"), 1),
    pageSize: WHONET_PAGE_SIZES.includes(requestedPageSize)
      ? requestedPageSize
      : 20,
  };
};

export const buildWhonetSearch = (state, now = new Date()) => {
  const draft = new URLSearchParams();
  ["from", "to", "dedup", "step", "page", "pageSize"].forEach((key) => {
    if (state[key] != null) draft.set(key, String(state[key]));
  });
  ["specimen", "organism", "origin", "significance"].forEach((key) =>
    values(state[key]).forEach((value) => draft.append(key, value)),
  );
  const normalized = parseWhonetSearch(draft.toString(), now);
  const params = new URLSearchParams();
  ["from", "to"].forEach((key) => params.set(key, String(normalized[key])));
  ["specimen", "organism", "origin", "significance"].forEach((key) =>
    normalized[key].forEach((value) => params.append(key, value)),
  );
  ["dedup", "step", "page", "pageSize"].forEach((key) =>
    params.set(key, String(normalized[key])),
  );
  return params.toString();
};

export const getWhonetMappingRepairUrl = (
  resource,
  resourceId,
  returnTo = "",
) => {
  if (!resourceId) {
    return "";
  }
  if (resource === "specimen-types") {
    const params = new URLSearchParams({ focus: "whonet" });
    if (
      returnTo === MICROBIOLOGY_WHONET_PATH ||
      returnTo.startsWith(`${MICROBIOLOGY_WHONET_PATH}?`)
    ) {
      params.set("returnTo", returnTo);
    }
    return `/MasterListsPage/SampleTypeEditor/${encodeURIComponent(resourceId)}/basic-info?${params.toString()}`;
  }
  if (!["organisms", "antibiotics"].includes(resource)) {
    return "";
  }
  const params = new URLSearchParams({ edit: resourceId });
  return `/MasterListsPage/MicrobiologyReference/${resource}?${params.toString()}`;
};

export const toWhonetRequest = (state) => ({
  from: state.from,
  to: state.to,
  specimen: [...state.specimen],
  organism: [...state.organism],
  origin: [...state.origin],
  significance: [...state.significance],
  dedup: state.dedup,
  page: state.page,
  pageSize: state.pageSize,
});
