export const MICROBIOLOGY_WHONET_PATH = "/Microbiology/whonet";
export const WHONET_PAGE_SIZES = [20, 50, 100];

const SIGNIFICANCE_POLICIES = new Set(["CLINICALLY_SIGNIFICANT", "ALL"]);
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
  !Number.isNaN(new Date(`${value}T00:00:00`).getTime());

const positiveInteger = (value, fallback) => {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
};

export const parseWhonetSearch = (search = "", now = new Date()) => {
  const params = new URLSearchParams(search);
  const defaults = previousCompleteMonth(now);
  const significance = params.get("significance");
  const dedup = params.get("dedup");
  const step = params.get("step");
  const requestedPageSize = positiveInteger(params.get("pageSize"), 20);
  return {
    from: validIsoDate(params.get("from")) ? params.get("from") : defaults.from,
    to: validIsoDate(params.get("to")) ? params.get("to") : defaults.to,
    significance: SIGNIFICANCE_POLICIES.has(significance)
      ? significance
      : "CLINICALLY_SIGNIFICANT",
    dedup: DEDUP_POLICIES.has(dedup) ? dedup : "FIRST_ISOLATE_7_DAY",
    step: STEPS.has(step) ? step : "configure",
    page: positiveInteger(params.get("page"), 1),
    pageSize: WHONET_PAGE_SIZES.includes(requestedPageSize)
      ? requestedPageSize
      : 20,
  };
};

export const buildWhonetSearch = (state, now = new Date()) => {
  const normalized = parseWhonetSearch(
    new URLSearchParams(state).toString(),
    now,
  );
  const params = new URLSearchParams();
  ["from", "to", "significance", "dedup", "step", "page", "pageSize"].forEach(
    (key) => params.set(key, String(normalized[key])),
  );
  return params.toString();
};

export const getWhonetUrl = (state, now = new Date()) =>
  `${MICROBIOLOGY_WHONET_PATH}?${buildWhonetSearch(state, now)}`;

export const getWhonetMappingRepairUrl = (resource, resourceId) => {
  if (!["organisms", "antibiotics"].includes(resource) || !resourceId) {
    return "";
  }
  const params = new URLSearchParams({ edit: resourceId });
  return `/MasterListsPage/MicrobiologyReference/${resource}?${params.toString()}`;
};

export const toWhonetRequest = (state) => ({
  from: state.from,
  to: state.to,
  significance: state.significance,
  dedup: state.dedup,
  page: state.page,
  pageSize: state.pageSize,
});
