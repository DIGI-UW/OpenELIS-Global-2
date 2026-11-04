import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: { getFromOpenElisServer: vi.fn() },
}));

vi.mock("../../../utils/Utils", () => utilsMock);
vi.mock("../../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import RunView, { prettyJson } from "./RunView";

const HEADER = {
  id: 7,
  label: "RUN-7",
  source: "ANALYZER",
  state: "OPEN",
  analyzerId: "3",
  analyzerName: "Mindray BS-200",
  method: "mindray-bs200",
  labUnits: ["Biochemistry"],
  operatorName: "op-7",
  openedAt: "2026-11-04T08:00:00Z",
  lastActivityAt: "2026-11-04T09:00:00Z",
  dispositioned: 3,
  total: 12,
  rawAvailable: true,
};

const ROWS = {
  page: 1,
  pageSize: 50,
  totalRows: 2,
  rows: [
    {
      id: "41",
      accessionNumber: "DEV0126000000000400",
      testName: "HIVVL",
      part: "Log viral load",
      value: "5.4",
      units: "mmol/L",
      control: false,
      duplicate: false,
    },
    {
      id: "42",
      accessionNumber: "QC-L1",
      testName: "Glucose",
      value: "4.9",
      control: true,
      duplicate: false,
    },
  ],
};

const wire = (header: unknown) =>
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: (response: unknown) => void) => {
      if (endPoint === "/rest/result-runs/7") return callback(header);
      if (endPoint.startsWith("/rest/result-runs/7/rows"))
        return callback(header ? ROWS : undefined);
      if (endPoint === "/rest/result-runs/7/raw")
        return callback({ resourceType: "Bundle" });
      if (endPoint.startsWith("/rest/result-runs"))
        return callback({ open: 1, held: 0, runs: [] });
      return callback(undefined);
    },
  );

const renderRun = (filtersReplaced = false) => {
  const onBack = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <RunView
        runId={7}
        filtersReplaced={filtersReplaced}
        onBack={onBack}
        onOpenRun={vi.fn()}
      />
    </IntlProvider>,
  );
  return onBack;
};

describe("OGC-1200 run view", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
  });

  it("shows the run's settings, progress and values", () => {
    wire(HEADER);
    renderRun();

    expect(screen.getByTestId("run-subtitle")).toHaveTextContent("RUN-7");
    expect(screen.getByTestId("run-settings")).toHaveTextContent(
      "Analyzer message",
    );
    expect(screen.getByTestId("run-settings")).toHaveTextContent(
      "Mindray BS-200",
    );
    expect(screen.getByTestId("run-progress")).toHaveTextContent(
      "3 of 12 rows dispositioned",
    );
    expect(screen.getByTestId("run-row-41")).toHaveTextContent("5.4");
    expect(screen.getByTestId("run-row-41")).toHaveTextContent(
      "HIVVL — Log viral load",
    );
    expect(screen.getByTestId("run-row-42")).toHaveTextContent("Control");
    expect(
      screen.getByRole("link", { name: /analyzer import page/i }),
    ).toHaveAttribute("href", "/AnalyzerResults?id=3");
  });

  it("shows the raw message inline, not in a dialog", () => {
    wire(HEADER);
    renderRun();

    fireEvent.click(screen.getByRole("button", { name: "View raw" }));

    expect(screen.getByTestId("run-raw")).toHaveTextContent("Bundle");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("says when the run cannot be seen", () => {
    wire(undefined);
    renderRun();

    expect(
      screen.getByText(/does not exist, or it holds no results you can see/),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("run-settings")).not.toBeInTheDocument();
  });

  it("tells the user the run replaced their filters, and goes back", () => {
    wire(HEADER);
    const onBack = renderRun(true);

    expect(screen.getByText("The run is the filter")).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Back to all results" }),
    );
    expect(onBack).toHaveBeenCalled();
  });

  it("indents a message that arrives as JSON text", () => {
    expect(prettyJson('{"resourceType":"Bundle"}')).toBe(
      '{\n  "resourceType": "Bundle"\n}',
    );
    expect(prettyJson({ resourceType: "Bundle" })).toBe(
      '{\n  "resourceType": "Bundle"\n}',
    );
    expect(prettyJson("not json")).toBe("not json");
  });
});
