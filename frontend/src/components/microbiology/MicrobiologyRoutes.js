export const MICROBIOLOGY_WORKLIST_PATH = "/Microbiology/worklist";
export const MICROBIOLOGY_CASE_PATH = "/Microbiology/cases";

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
  urgency: "",
  due: "",
  sort: "priority",
};

const textValue = (value) => (typeof value === "string" ? value.trim() : "");

const normalizeWorklistState = (state = {}) => ({
  workflow: textValue(state.workflow),
  urgency: textValue(state.urgency),
  due: textValue(state.due),
  sort: ["priority", "newest", "workflow"].includes(state.sort)
    ? state.sort
    : DEFAULT_WORKLIST_STATE.sort,
});

const toSearch = (state, section = "") => {
  const params = new URLSearchParams();
  if (state.workflow) {
    params.set("workflow", state.workflow);
  }
  if (state.urgency) {
    params.set("urgency", state.urgency);
  }
  if (state.due) {
    params.set("due", state.due);
  }
  if (state.sort !== DEFAULT_WORKLIST_STATE.sort) {
    params.set("sort", state.sort);
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
    urgency: params.get("urgency"),
    due: params.get("due"),
    sort: params.get("sort") || "priority",
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
