export const MICROBIOLOGY_WHONET_PATH = "/Microbiology/whonet";
export const WHONET_PAGE_SIZES = [20, 50, 100];

const SIGNIFICANCE_VALUES = new Set([
  "CLINICALLY_SIGNIFICANT",
  "CONTAMINANT",
  "NORMAL_FLORA",
  "UNKNOWN",
]);
const DEDUP_POLICIES = new Set([
  "FIRST_ISOLATE_7_DAY",
  "FIRST_ISOLATE_14_DAY",
  "FIRST_ISOLATE_30_DAY",
  "NONE",
]);
const DEDUP_BASES = new Set(["COLLECTION_DATE", "RELEASE_DATE"]);
const DEDUP_SCOPES = new Set(["ANY_SOURCE", "SAME_SOURCE"]);
const PROFILE_SENSITIVITIES = new Set(["INSENSITIVE", "SENSITIVE"]);
const STEPS = new Set(["configure", "preview"]);
const SOURCES = new Set(["ast-worklist"]);

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

export const getWhonetDateRange = (preset, now = new Date()) => {
  if (preset === "LAST_MONTH") return previousCompleteMonth(now);
  if (preset === "THIS_QUARTER") {
    const firstMonth = Math.floor(now.getMonth() / 3) * 3;
    return {
      from: isoDate(new Date(now.getFullYear(), firstMonth, 1)),
      to: isoDate(new Date(now.getFullYear(), firstMonth + 3, 0)),
    };
  }
  return {
    from: isoDate(new Date(now.getFullYear(), now.getMonth(), 1)),
    to: isoDate(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
  };
};

export const getWhonetDatePreset = (state, now = new Date()) => {
  for (const preset of ["THIS_MONTH", "LAST_MONTH", "THIS_QUARTER"]) {
    const range = getWhonetDateRange(preset, now);
    if (state.from === range.from && state.to === range.to) return preset;
  }
  return "CUSTOM";
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

const enabled = (params, key) => params.get(key) === "true";

const booleanValue = (params, key, fallback) => {
  const value = params.get(key);
  if (value === "true") return true;
  if (value === "false") return false;
  return fallback;
};

const allowedValue = (params, key, allowed, fallback) => {
  const value = params.get(key);
  return allowed.has(value) ? value : fallback;
};

export const parseWhonetSearch = (search = "", now = new Date()) => {
  const params = new URLSearchParams(search);
  const requestedSource = params.get("source");
  const source = SOURCES.has(requestedSource) ? requestedSource : "";
  const defaults = source
    ? getWhonetDateRange("THIS_MONTH", now)
    : previousCompleteMonth(now);
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
    includeScreening: enabled(params, "includeScreening"),
    includeUnspecified: enabled(params, "includeUnspecified"),
    dedup: DEDUP_POLICIES.has(dedup) ? dedup : "FIRST_ISOLATE_7_DAY",
    dedupBasis: allowedValue(
      params,
      "dedupBasis",
      DEDUP_BASES,
      "COLLECTION_DATE",
    ),
    dedupScope: allowedValue(params, "dedupScope", DEDUP_SCOPES, "ANY_SOURCE"),
    excludeContaminants: booleanValue(params, "excludeContaminants", true),
    profileSensitivity: allowedValue(
      params,
      "profileSensitivity",
      PROFILE_SENSITIVITIES,
      "INSENSITIVE",
    ),
    step: STEPS.has(step) ? step : "configure",
    page: positiveInteger(params.get("page"), 1),
    pageSize: WHONET_PAGE_SIZES.includes(requestedPageSize)
      ? requestedPageSize
      : 20,
    source,
  };
};

export const buildWhonetSearch = (state, now = new Date()) => {
  const draft = new URLSearchParams();
  [
    "from",
    "to",
    "dedup",
    "dedupBasis",
    "dedupScope",
    "profileSensitivity",
    "source",
    "step",
    "page",
    "pageSize",
  ].forEach((key) => {
    if (state[key] != null) draft.set(key, String(state[key]));
  });
  ["specimen", "organism", "origin", "significance"].forEach((key) =>
    values(state[key]).forEach((value) => draft.append(key, value)),
  );
  ["includeScreening", "includeUnspecified", "excludeContaminants"].forEach(
    (key) => draft.set(key, String(Boolean(state[key]))),
  );
  const normalized = parseWhonetSearch(draft.toString(), now);
  const params = new URLSearchParams();
  ["from", "to"].forEach((key) => params.set(key, String(normalized[key])));
  ["specimen", "organism", "origin", "significance"].forEach((key) =>
    normalized[key].forEach((value) => params.append(key, value)),
  );
  ["includeScreening", "includeUnspecified"].forEach((key) =>
    params.set(key, String(normalized[key])),
  );
  params.set("dedup", String(normalized.dedup));
  params.set("dedupBasis", String(normalized.dedupBasis));
  params.set("dedupScope", String(normalized.dedupScope));
  params.set("excludeContaminants", String(normalized.excludeContaminants));
  params.set("profileSensitivity", String(normalized.profileSensitivity));
  if (normalized.source) params.set("source", normalized.source);
  ["step", "page", "pageSize"].forEach((key) =>
    params.set(key, String(normalized[key])),
  );
  return params.toString();
};

export const getWhonetExportUrlFromWorklist = (
  worklistState,
  now = new Date(),
) => {
  const currentMonth = getWhonetDateRange("THIS_MONTH", now);
  const state = {
    ...parseWhonetSearch("", now),
    from: validIsoDate(worklistState.from)
      ? worklistState.from
      : currentMonth.from,
    to: validIsoDate(worklistState.to) ? worklistState.to : currentMonth.to,
    specimen: normalizedValues(worklistState.specimen),
    organism: normalizedValues(worklistState.organism),
    origin: normalizedValues(worklistState.origin),
    significance:
      normalizedValues(worklistState.significance, SIGNIFICANCE_VALUES).length >
      0
        ? normalizedValues(worklistState.significance, SIGNIFICANCE_VALUES)
        : [...SIGNIFICANCE_VALUES],
    source: "ast-worklist",
    step: "configure",
    page: 1,
    pageSize: 20,
  };
  return `${MICROBIOLOGY_WHONET_PATH}?${buildWhonetSearch(state, now)}`;
};

export const clearWhonetWorklistScope = (_state, now = new Date()) =>
  parseWhonetSearch("", now);

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
  includeScreening: state.includeScreening,
  includeUnspecified: state.includeUnspecified,
  dedup: state.dedup,
  dedupBasis: state.dedupBasis,
  dedupScope: state.dedupScope,
  excludeContaminants: state.excludeContaminants,
  profileSensitivity: state.profileSensitivity,
  page: state.page,
  pageSize: state.pageSize,
});
