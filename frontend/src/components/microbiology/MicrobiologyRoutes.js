export const MICROBIOLOGY_WORKLIST_PATH = "/Microbiology/worklist";
export const MICROBIOLOGY_CASE_PATH = "/Microbiology/cases";
export const MICROBIOLOGY_WORKLIST_PAGE_SIZES = [10, 20, 50, 100];

export const MICROBIOLOGY_CASE_SECTIONS = [
  "case-info",
  "order-detail",
  "setup",
  "timeline",
  "isolates",
  "ast",
  "critical-communication",
  "reports",
];

const DEFAULT_WORKLIST_STATE = {
  workflow: "",
  stage: "",
  urgency: "",
  due: "",
  q: "",
  sort: "priority",
  page: 1,
  pageSize: 20,
};

const textValue = (value) => (typeof value === "string" ? value.trim() : "");

const positiveInteger = (value, fallback) => {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
};

const normalizeWorklistState = (state = {}) => ({
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
});

const toSearch = (state, section = "") => {
  const params = new URLSearchParams();
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
  if (MICROBIOLOGY_CASE_SECTIONS.includes(section)) {
    params.set("section", section);
  }
  const query = params.toString();
  return query ? `?${query}` : "";
};

export const parseMicrobiologyWorklistSearch = (search = "") => {
  const params = new URLSearchParams(search);
  return normalizeWorklistState({
    workflow: params.get("workflow"),
    stage: params.get("stage"),
    urgency: params.get("urgency"),
    due: params.get("due"),
    q: params.get("q"),
    sort: params.get("sort") || "priority",
    page: params.get("page"),
    pageSize: params.get("pageSize"),
  });
};

export const parseMicrobiologyCaseSearch = (search = "") => {
  const params = new URLSearchParams(search);
  return {
    ...parseMicrobiologyWorklistSearch(search),
    section: MICROBIOLOGY_CASE_SECTIONS.includes(params.get("section"))
      ? params.get("section")
      : "",
  };
};

export const getMicrobiologyWorklistUrl = (state = {}) =>
  `${MICROBIOLOGY_WORKLIST_PATH}${toSearch(normalizeWorklistState(state))}`;

export const getMicrobiologyCaseUrl = (caseId, state = {}) => {
  const normalized = normalizeWorklistState(state);
  return `${MICROBIOLOGY_CASE_PATH}/${encodeURIComponent(caseId)}${toSearch(
    normalized,
    state.section,
  )}`;
};
