import { getWhonetDateRange } from "./WhonetRoutes";

export const MICROBIOLOGY_WORKLIST_PATH = "/Microbiology/worklist";
export const MICROBIOLOGY_CASE_PATH = "/Microbiology/cases";
export const MICROBIOLOGY_WORKLIST_PAGE_SIZES = [10, 20, 50, 100];

export const MICROBIOLOGY_CASE_SECTIONS = [
  "case-info",
  "order-detail",
  "setup",
  "timeline",
  "nonconformance",
  "isolates",
  "ast",
  "critical-communication",
  "reports",
  "amendment",
];
export const MICROBIOLOGY_CASE_ACTIONS = [
  "start-inoculation",
  "add-subculture",
  "log-critical",
  "report-nce",
  "mark-lost",
  "mark-positive",
  "mark-no-growth",
  "new-ast-attempt",
  "set-protocol",
  "change-protocol",
];
export const MICROBIOLOGY_CRITICAL_TARGET_TYPES = ["CASE", "ISOLATE"];

const DEFAULT_WORKLIST_STATE = {
  grain: "cultures",
  status: "",
  from: "",
  to: "",
  specimen: [],
  organism: [],
  origin: [],
  significance: [],
  workflow: "",
  stage: "",
  urgency: "",
  due: "",
  q: "",
  sort: "priority",
  page: 1,
  pageSize: 20,
};

const WORKLIST_STATUSES = {
  cultures: ["incubating", "positive", "growth", "ready"],
  ast: ["pending-setup", "in-progress", "results-in", "reviewed"],
};

const textValue = (value) => (typeof value === "string" ? value.trim() : "");

const validIsoDate = (value) =>
  typeof value === "string" &&
  /^\d{4}-\d{2}-\d{2}$/.test(value) &&
  !Number.isNaN(new Date(`${value}T00:00:00Z`).getTime()) &&
  new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value;

const values = (value) =>
  [
    ...new Set(
      (Array.isArray(value) ? value : value == null ? [] : [value])
        .map((item) => textValue(String(item)))
        .filter(Boolean),
    ),
  ].sort();

const positiveInteger = (value, fallback) => {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
};

const normalizeWorklistState = (state = {}, now = new Date()) => {
  const grain = state.grain === "ast" ? "ast" : DEFAULT_WORKLIST_STATE.grain;
  const status = WORKLIST_STATUSES[grain].includes(textValue(state.status))
    ? textValue(state.status)
    : "";
  const currentMonth = getWhonetDateRange("THIS_MONTH", now);
  return {
    grain,
    status,
    from:
      grain === "ast"
        ? validIsoDate(state.from)
          ? state.from
          : currentMonth.from
        : "",
    to:
      grain === "ast"
        ? validIsoDate(state.to)
          ? state.to
          : currentMonth.to
        : "",
    specimen: grain === "ast" ? values(state.specimen) : [],
    organism: grain === "ast" ? values(state.organism) : [],
    origin: grain === "ast" ? values(state.origin) : [],
    significance: grain === "ast" ? values(state.significance) : [],
    workflow: textValue(state.workflow),
    stage: textValue(state.stage),
    urgency: textValue(state.urgency),
    due: textValue(state.due),
    q: textValue(state.q),
    sort: ["priority", "newest", "workflow"].includes(state.sort)
      ? state.sort
      : DEFAULT_WORKLIST_STATE.sort,
    page: positiveInteger(state.page, DEFAULT_WORKLIST_STATE.page),
    pageSize: MICROBIOLOGY_WORKLIST_PAGE_SIZES.includes(
      positiveInteger(state.pageSize, DEFAULT_WORKLIST_STATE.pageSize),
    )
      ? positiveInteger(state.pageSize, DEFAULT_WORKLIST_STATE.pageSize)
      : DEFAULT_WORKLIST_STATE.pageSize,
  };
};

const toSearch = (state, caseState = {}) => {
  const params = new URLSearchParams();
  if (state.grain !== DEFAULT_WORKLIST_STATE.grain) {
    params.set("grain", state.grain);
  }
  if (state.status) {
    params.set("status", state.status);
  }
  if (state.grain === "ast") {
    params.set("from", state.from);
    params.set("to", state.to);
    ["specimen", "organism", "origin", "significance"].forEach((key) =>
      state[key].forEach((value) => params.append(key, value)),
    );
  }
  if (state.workflow) {
    params.set("workflow", state.workflow);
  }
  if (state.stage) {
    params.set("stage", state.stage);
  }
  if (state.urgency) {
    params.set("urgency", state.urgency);
  }
  if (state.due) {
    params.set("due", state.due);
  }
  if (state.q) {
    params.set("q", state.q);
  }
  if (state.sort !== DEFAULT_WORKLIST_STATE.sort) {
    params.set("sort", state.sort);
  }
  if (state.page !== DEFAULT_WORKLIST_STATE.page) {
    params.set("page", String(state.page));
  }
  if (state.pageSize !== DEFAULT_WORKLIST_STATE.pageSize) {
    params.set("pageSize", String(state.pageSize));
  }
  if (MICROBIOLOGY_CASE_SECTIONS.includes(caseState.section)) {
    params.set("section", caseState.section);
  }
  if (caseState.section === "ast") {
    if (textValue(caseState.astIsolateId)) {
      params.set("astIsolateId", textValue(caseState.astIsolateId));
    }
    if (textValue(caseState.astRunId)) {
      params.set("astRunId", textValue(caseState.astRunId));
    }
    if (caseState.astView === "reviewed") {
      params.set("astView", "reviewed");
    }
  }
  if (MICROBIOLOGY_CASE_ACTIONS.includes(caseState.action)) {
    params.set("action", caseState.action);
    if (
      caseState.action === "log-critical" &&
      MICROBIOLOGY_CRITICAL_TARGET_TYPES.includes(caseState.targetType) &&
      textValue(caseState.targetId)
    ) {
      params.set("targetType", caseState.targetType);
      params.set("targetId", textValue(caseState.targetId));
    }
  }
  const query = params.toString();
  return query ? `?${query}` : "";
};

export const parseMicrobiologyWorklistSearch = (
  search = "",
  now = new Date(),
) => {
  const params = new URLSearchParams(search);
  return normalizeWorklistState(
    {
      grain: params.get("grain"),
      status: params.get("status"),
      from: params.get("from"),
      to: params.get("to"),
      specimen: params.getAll("specimen"),
      organism: params.getAll("organism"),
      origin: params.getAll("origin"),
      significance: params.getAll("significance"),
      workflow: params.get("workflow"),
      stage: params.get("stage"),
      urgency: params.get("urgency"),
      due: params.get("due"),
      q: params.get("q"),
      sort: params.get("sort") || "priority",
      page: params.get("page"),
      pageSize: params.get("pageSize"),
    },
    now,
  );
};

export const parseMicrobiologyCaseSearch = (search = "") => {
  const params = new URLSearchParams(search);
  const action = MICROBIOLOGY_CASE_ACTIONS.includes(params.get("action"))
    ? params.get("action")
    : "";
  const targetType = MICROBIOLOGY_CRITICAL_TARGET_TYPES.includes(
    params.get("targetType"),
  )
    ? params.get("targetType")
    : "";
  return {
    ...parseMicrobiologyWorklistSearch(search),
    section: MICROBIOLOGY_CASE_SECTIONS.includes(params.get("section"))
      ? params.get("section")
      : "",
    action,
    targetType: action === "log-critical" ? targetType : "",
    targetId:
      action === "log-critical" && targetType
        ? textValue(params.get("targetId"))
        : "",
    astRunId:
      params.get("section") === "ast" ? textValue(params.get("astRunId")) : "",
    astIsolateId:
      params.get("section") === "ast"
        ? textValue(params.get("astIsolateId"))
        : "",
    astView:
      params.get("section") === "ast" && params.get("astView") === "reviewed"
        ? "reviewed"
        : "",
  };
};

export const getMicrobiologyWorklistUrl = (state = {}, now = new Date()) =>
  `${MICROBIOLOGY_WORKLIST_PATH}${toSearch(normalizeWorklistState(state, now))}`;

export const getMicrobiologyCaseUrl = (
  caseId,
  state = {},
  now = new Date(),
) => {
  const normalized = normalizeWorklistState(state, now);
  return `${MICROBIOLOGY_CASE_PATH}/${encodeURIComponent(caseId)}${toSearch(
    normalized,
    state,
  )}`;
};
