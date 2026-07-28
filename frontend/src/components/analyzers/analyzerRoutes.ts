export interface AnalyzerListRouteState {
  search?: string;
  status?: string;
  testUnit?: string;
  analyzerType?: string;
}

export interface ProfileCatalogRouteState {
  search?: string;
  protocol?: string;
  readiness?: string;
}

export type AnalyzerSetupStep = "instrument" | "verify" | "connect" | "review";

interface AnalyzerSetupContext {
  analyzerId?: string;
  profileId?: string;
  returnTo?: string;
}

const LIST_QUERY_KEYS: Array<keyof AnalyzerListRouteState> = [
  "search",
  "status",
  "testUnit",
  "analyzerType",
];

const PROFILE_QUERY_KEYS: Array<keyof ProfileCatalogRouteState> = [
  "search",
  "protocol",
  "readiness",
];

export const parseAnalyzerListQuery = (
  search: string,
): Required<AnalyzerListRouteState> => {
  const params = new URLSearchParams(search);

  return {
    search: params.get("search") || "",
    status: params.get("status") || "",
    testUnit: params.get("testUnit") || "",
    analyzerType: params.get("analyzerType") || "",
  };
};

export const buildAnalyzerListUrl = (state: AnalyzerListRouteState): string => {
  const params = new URLSearchParams();

  LIST_QUERY_KEYS.forEach((key) => {
    const value = state[key]?.trim();
    if (value) {
      params.set(key, value);
    }
  });

  const query = params.toString();
  return query ? `/analyzers?${query}` : "/analyzers";
};

export const parseProfileCatalogQuery = (
  search: string,
): Required<ProfileCatalogRouteState> => {
  const params = new URLSearchParams(search);
  return {
    search: params.get("search") || "",
    protocol: params.get("protocol") || "",
    readiness: params.get("readiness") || "",
  };
};

export const buildProfileCatalogUrl = (
  state: ProfileCatalogRouteState,
): string => {
  const params = new URLSearchParams();
  PROFILE_QUERY_KEYS.forEach((key) => {
    const value = state[key]?.trim();
    if (value) {
      params.set(key, value);
    }
  });
  const query = params.toString();
  return query ? `/analyzers/types?${query}` : "/analyzers/types";
};

export const resolveAnalyzerReturnTo = (
  candidate: string | null | undefined,
): string => {
  if (
    !candidate ||
    !candidate.startsWith("/analyzers") ||
    candidate.startsWith("//")
  ) {
    return "/analyzers";
  }

  try {
    const url = new URL(candidate, "https://openelis.invalid");
    if (
      url.origin !== "https://openelis.invalid" ||
      !url.pathname.startsWith("/analyzers")
    ) {
      return "/analyzers";
    }
    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return "/analyzers";
  }
};

export const buildAnalyzerSetupUrl = (
  step: AnalyzerSetupStep,
  context: AnalyzerSetupContext = {},
): string => {
  const params = new URLSearchParams();
  const analyzerId = context.analyzerId;

  if (step === "instrument") {
    params.set("add", "1");
  } else {
    if (!analyzerId) {
      throw new Error(`Analyzer ID is required for the ${step} setup step`);
    }
    params.set("setup", "1");
  }

  params.set("step", step);
  if (context.profileId) {
    params.set("profile", context.profileId);
  }
  params.set("returnTo", resolveAnalyzerReturnTo(context.returnTo));

  const paths: Record<AnalyzerSetupStep, string> = {
    instrument: "/analyzers",
    verify: `/analyzers/${analyzerId}/mappings`,
    connect: `/analyzers/${analyzerId}/edit`,
    review: `/analyzers/${analyzerId}/review`,
  };

  return `${paths[step]}?${params.toString()}`;
};

export const buildAnalyzerQcRuleUrl = (
  analyzerId: string,
  returnTo: string,
): string => {
  const params = new URLSearchParams();
  params.set("returnTo", resolveAnalyzerReturnTo(returnTo));
  return `/analyzers/${analyzerId}/qc-rules?${params.toString()}`;
};

export const buildAnalyzerControlLotUrl = (
  analyzerId: string,
  returnTo: string,
): string => {
  const params = new URLSearchParams();
  params.set("analyzerId", analyzerId);
  params.set("returnTo", resolveAnalyzerReturnTo(returnTo));
  return `/analyzers/qc/control-lots/new?${params.toString()}`;
};
