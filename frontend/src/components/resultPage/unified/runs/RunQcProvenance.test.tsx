import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";

vi.mock("../../../common/useSiteDateFormat", () => ({
  default: () => ({ pattern: "MM/dd/yyyy" }),
}));

import RunQcProvenance from "./RunQcProvenance";

const renderFact = (fact: Record<string, unknown>) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <RunQcProvenance
          runQc={{
            runId: 386,
            runLabel: "RUN-386",
            controls: [],
            ...fact,
          }}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("OGC-1200 FR-G3 the run's QC in Validation", () => {
  it("shows the duplicate that failed under a Warn policy beside the run's verdict", () => {
    renderFact({
      verdict: "PASS_WITH_WARNING",
      items: [
        {
          id: 9,
          kind: "DUPLICATE",
          observed: "100",
          parentAccession: "DEV01260000000000444",
          evaluation: "FAIL",
          detail: "RPD = 26.1% (threshold 10%)",
        },
      ],
    });

    expect(screen.getByTestId("run-qc-provenance-verdict")).toHaveTextContent(
      "Pass with warning",
    );
    expect(screen.getByTestId("run-qc-provenance-item-9")).toHaveTextContent(
      "Fail Duplicate of DEV01260000000000444 · 100 · RPD = 26.1% (threshold 10%)",
    );
  });

  it("names the blank a test lacked, not a control", () => {
    renderFact({
      verdict: "NO_CONTROL",
      decision: "ACCEPT_DESPITE",
      missing: ["BLANK"],
    });

    expect(screen.getByTestId("run-qc-provenance-verdict")).toHaveTextContent(
      "No blank",
    );
  });
});
