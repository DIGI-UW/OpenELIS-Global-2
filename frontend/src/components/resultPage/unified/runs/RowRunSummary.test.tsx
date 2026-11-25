import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: { getFromOpenElisServer: vi.fn() },
}));

vi.mock("../../../utils/Utils", () => utilsMock);
vi.mock("../../../common/useSiteDateFormat", () => ({
  default: () => ({ pattern: "MM/dd/yyyy" }),
}));

import RowRunSummary from "./RowRunSummary";

const header = (state: string) => ({
  id: 248,
  label: "RUN-248",
  source: "RESULT_ENTRY",
  state,
  reagentLot: null,
  qc: {
    overall: "DECIDED",
    tests: [
      {
        testId: "5",
        testName: "Amylase",
        verdict: "FAIL",
        decision: "ACCEPT_DESPITE",
        controls: [],
      },
    ],
  },
});

const renderSummary = (state: string, variant: "reagents" | "history") => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (url: string, callback: (r: unknown) => void) => {
      if (url === "/rest/result-runs/248" && typeof callback === "function") {
        callback(header(state));
      }
    },
  );
  return render(
    <IntlProvider locale="en" messages={messages}>
      <RowRunSummary runId={248} testId="5" variant={variant} />
    </IntlProvider>,
  );
};

describe("OGC-1200 FR-D6, FR-D7 a row's run", () => {
  beforeEach(() => utilsMock.getFromOpenElisServer.mockReset());

  it("puts the run reference in History once the run is complete", () => {
    renderSummary("COMPLETE", "history");

    const reference = screen.getByTestId("row-run-reference");
    expect(reference).toHaveTextContent("Recorded in RUN-248, Manual entry.");
    expect(reference).toHaveTextContent("Fail");
    expect(screen.getByTestId("row-run-link")).toHaveAttribute(
      "href",
      "/Results?run=248",
    );
  });

  it("keeps History quiet while the run is still open", () => {
    renderSummary("HELD", "history");
    expect(screen.queryByTestId("row-run-reference")).not.toBeInTheDocument();
  });

  it("drops a run that answers after the row was closed, without a state update on an unmounted summary", async () => {
    let answer: ((r: unknown) => void) | undefined;
    let signal: AbortSignal | undefined;
    utilsMock.getFromOpenElisServer.mockImplementation(
      (_url: string, callback: (r: unknown) => void, abort?: AbortSignal) => {
        answer = callback;
        signal = abort;
      },
    );
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = render(
      <IntlProvider locale="en" messages={messages}>
        <RowRunSummary runId={248} testId="5" variant="reagents" />
      </IntlProvider>,
    );
    unmount();
    await act(async () => {});
    answer?.(header("HELD"));

    expect(signal?.aborted).toBe(true);
    expect(errors).not.toHaveBeenCalled();
    errors.mockRestore();
  });

  it("says when a run has no control for the row's test and no lot", () => {
    renderSummary("OPEN", "reagents");

    expect(screen.getByTestId("row-run-summary")).toHaveTextContent(
      "In RUN-248, Manual entry.",
    );
    expect(screen.getByTestId("row-run-lot")).toHaveTextContent(
      "No reagent lot is recorded on this run.",
    );
    expect(screen.getByTestId("row-run-controls")).toHaveTextContent(
      "No control is recorded on the run for this test.",
    );
  });
});
