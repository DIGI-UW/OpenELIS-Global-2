const TERMINAL_STAGES = new Set([
  "REJECTED",
  "LOST_SPECIMEN",
  "LOST_SPECIMEN_POSITIVE",
]);

const STEP_BY_STAGE = {
  RECEIVED: {
    section: "setup",
    labelId: "microbiology.progress.inoculation",
  },
  SETUP_RECORDED: {
    section: "setup",
    labelId: "microbiology.progress.inoculation",
  },
  INCUBATING: {
    section: "setup",
    labelId: "microbiology.progress.inoculation",
  },
  GROWTH_DETECTED: {
    section: "isolates",
    labelId: "microbiology.progress.isolates",
  },
  IDENTIFICATION: {
    section: "isolates",
    labelId: "microbiology.progress.isolates",
  },
  AST_READY: { section: "ast", labelId: "microbiology.ast.title" },
  AST_IN_PROGRESS: { section: "ast", labelId: "microbiology.ast.title" },
  NO_GROWTH_READY: {
    section: "reports",
    labelId: "microbiology.progress.reports",
  },
  REVIEW_READY: {
    section: "reports",
    labelId: "microbiology.progress.reports",
  },
  PRELIM_RELEASED: {
    section: "reports",
    labelId: "microbiology.progress.reports",
  },
  FINAL_RELEASED: {
    section: "reports",
    labelId: "microbiology.progress.reports",
  },
  AMENDED: {
    section: "reports",
    labelId: "microbiology.progress.reports",
  },
};

const CASE_INFO_STEP = {
  section: "case-info",
  labelId: "microbiology.progress.caseInfo",
};

export const getMicrobiologyCurrentStep = (caseDetail = {}) => {
  if (caseDetail.workflowType === "UNASSIGNED") {
    return CASE_INFO_STEP;
  }
  if (caseDetail.finalReleaseState === "AMENDMENT_IN_PROGRESS") {
    return {
      section: "amendment",
      labelId: "microbiology.amendment.title",
    };
  }
  if (TERMINAL_STAGES.has(caseDetail.stage)) {
    return CASE_INFO_STEP;
  }
  return STEP_BY_STAGE[caseDetail.stage] || CASE_INFO_STEP;
};

export const getMicrobiologyCurrentStepSection = (caseDetail) =>
  getMicrobiologyCurrentStep(caseDetail).section;
