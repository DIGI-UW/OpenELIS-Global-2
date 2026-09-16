import { useCallback, useEffect, useMemo } from "react";
import { useHistory, useLocation } from "react-router-dom";

const STEPS = ["columns", "filters", "review"];
const VIEWS = ["overview", "builder", "saved", "queue"];
const LAYOUTS = ["SPREADSHEET", "RESULT_LIST", "TABLE"];

function readRoute(search) {
  const params = new URLSearchParams(search);
  const view = params.get("view");
  const page = Number(params.get("page"));
  return {
    panel: VIEWS.includes(view) ? view : "overview",
    step: Math.max(0, STEPS.indexOf(params.get("step"))) + 1,
    reportType: params.get("type") || null,
    layout: LAYOUTS.includes(params.get("layout"))
      ? params.get("layout")
      : "SPREADSHEET",
    savedId: params.get("saved") || null,
    jobId: params.get("job") || null,
    page:
      Number.isSafeInteger(page) && page >= 0 && page <= 2147483647 ? page : 0,
    search: params.get("q") || "",
  };
}

function writeRoute(search, route) {
  // Preserve parameters owned by the review widget or other application tools.
  const params = new URLSearchParams(search);
  const builder = route.panel === "builder";
  const values = {
    view: route.panel === "overview" ? null : route.panel,
    step: builder ? STEPS[route.step - 1] : null,
    type: builder ? route.reportType : null,
    layout: builder ? route.layout : null,
    saved: builder ? route.savedId : null,
    page: route.panel === "queue" && route.page ? route.page : null,
    job: route.panel === "queue" || builder ? route.jobId : null,
    q: route.panel === "saved" ? route.search : null,
  };
  Object.entries(values).forEach(([key, value]) => {
    if (value == null || value === "") params.delete(key);
    else params.set(key, String(value));
  });
  return params.size ? `?${params.toString()}` : "";
}

export default function useReportingRoute() {
  const history = useHistory();
  const location = useLocation();
  const route = useMemo(() => readRoute(location.search), [location.search]);
  const navigate = useCallback(
    (changes, replace = false) => {
      const search = writeRoute(location.search, { ...route, ...changes });
      if (search !== location.search)
        history[replace ? "replace" : "push"]({ ...location, search });
    },
    [history, location, route],
  );

  useEffect(() => {
    navigate({}, true);
  }, [navigate]);

  return { ...route, navigate };
}
