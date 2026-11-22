import React from "react";
import { render, screen } from "@testing-library/react";
import { Router, Route, Switch, useLocation } from "react-router-dom";
import { createMemoryHistory } from "history";
import { vi } from "vitest";
import ReportingRoute from "./ReportingRoute";
import { REPORTING_ROUTE_PATHS, canonicalReportingUrl } from "./routes";

vi.mock("./CustomDataExport", () => ({ default: function Workspace() {
  const location = useLocation();
  return <div data-testid="workspace-location">{location.pathname + location.search + location.hash}</div>;
} }));

test.each(["/CustomDataExport", "/reports", "/reports/custom-data-export"])(
  "opening %s reaches one canonical workspace while retaining deep-link state", (path) => {
    const suffix = "?view=queue&job=example&page=2&review=review-1#report";
    const history = createMemoryHistory({ initialEntries: ["/Dashboard", path + suffix], initialIndex: 1 });
    render(<Router history={history}><Switch><Route path={REPORTING_ROUTE_PATHS} exact><ReportingRoute /></Route></Switch></Router>);
    expect(screen.getByTestId("workspace-location")).toHaveTextContent("/reports/custom-data-export" + suffix);
    expect(history.entries.map(({ pathname }) => pathname)).toEqual(["/Dashboard", "/reports/custom-data-export"]);
    expect(history.index).toBe(1);
    expect(canonicalReportingUrl(path + suffix)).toBe("/reports/custom-data-export" + suffix);
  },
);
