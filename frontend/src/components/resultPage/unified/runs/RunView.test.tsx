import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  },
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

describe("OGC-1200 slice 2: chips, paging and selection", () => {
  const COUNTED = {
    ...HEADER,
    chipCounts: {
      all: 120,
      needsReview: 9,
      critical: 2,
      abnormal: 4,
      exceptions: 3,
      held: 0,
      normal: 108,
    },
  };
  const pageOf = (ids: string[], category: string, flag?: string) => ({
    page: 1,
    pageSize: 50,
    totalRows: 120,
    rows: ids.map((id) => ({
      id,
      accessionNumber: `LAB-${id}`,
      testName: "Glucose",
      value: "9",
      control: false,
      duplicate: false,
      flag,
      category,
    })),
  });
  const rowRequests: string[] = [];
  const summaries: unknown[] = [];

  beforeEach(() => {
    cleanup();
    rowRequests.length = 0;
    summaries.length = 0;
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(
      (endPoint: string, callback: (response: unknown) => void) => {
        if (endPoint === "/rest/result-runs/7") return callback(COUNTED);
        if (endPoint.startsWith("/rest/result-runs/7/rows")) {
          rowRequests.push(endPoint);
          if (endPoint.includes("chip=CRITICAL")) {
            return callback(pageOf(["90", "91"], "CRITICAL", "CRITICAL"));
          }
          return callback(
            endPoint.includes("page=2")
              ? pageOf(["60"], "NORMAL")
              : pageOf(["10", "11"], "NORMAL"),
          );
        }
        if (endPoint.startsWith("/rest/result-runs"))
          return callback({ open: 1, held: 0, runs: [] });
        return callback(undefined);
      },
    );
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (endPoint: string, body: string, callback: (r: unknown) => void) => {
        summaries.push(JSON.parse(body));
        callback({
          selected: 2,
          critical: 0,
          abnormal: 0,
          normal: 1,
          skipped: [
            {
              id: "11",
              accessionNumber: "LAB-11",
              testName: "Glucose",
              reason: "EXCEPTION",
            },
          ],
        });
      },
    );
  });

  it("shows each chip with its count over the whole run and asks the server for the chip's rows", () => {
    renderRun();

    expect(screen.getByTestId("run-chip-ALL")).toHaveTextContent("All (120)");
    expect(screen.getByTestId("run-chip-NEEDS_REVIEW")).toHaveTextContent(
      "Needs review (9)",
    );
    expect(rowRequests.at(-1)).toContain("chip=ALL&page=1&pageSize=50");

    fireEvent.click(screen.getByTestId("run-chip-CRITICAL"));

    expect(rowRequests.at(-1)).toContain("chip=CRITICAL&page=1");
    expect(screen.getByTestId("run-row-90")).toBeInTheDocument();
    expect(screen.getByTestId("run-chip-CRITICAL")).toHaveAttribute(
      "aria-pressed",
      "true",
    );
  });

  it("keeps a selection across pages and summarises it from the server, naming skipped rows", () => {
    renderRun();

    fireEvent.click(screen.getByLabelText("Select the value of LAB-10"));
    fireEvent.click(screen.getByLabelText("Select the value of LAB-11"));
    fireEvent.click(screen.getByRole("button", { name: /next page/i }));
    expect(rowRequests.at(-1)).toContain("page=2");
    fireEvent.click(screen.getByRole("button", { name: /previous page/i }));

    expect(screen.getByLabelText("Select the value of LAB-10")).toBeChecked();
    expect(summaries.at(-1)).toEqual({
      all: false,
      chip: "ALL",
      ids: ["10", "11"],
      excluded: [],
    });
    expect(screen.getByTestId("run-selection")).toHaveTextContent(
      "Selected values: 2 (0 critical, 0 abnormal, 1 normal).",
    );
    expect(screen.getByTestId("run-selection-skipped")).toHaveTextContent(
      "LAB-11, Glucose: exception to resolve first",
    );
  });

  it("selects all in the filter as the chip less exclusions, never as a list of rows", () => {
    renderRun();
    fireEvent.click(screen.getByTestId("run-chip-CRITICAL"));
    fireEvent.click(screen.getByLabelText("Select the value of LAB-90"));

    fireEvent.click(screen.getByTestId("run-select-all"));
    fireEvent.click(screen.getByLabelText("Select the value of LAB-91"));

    expect(summaries.at(-1)).toEqual({
      all: true,
      chip: "CRITICAL",
      ids: [],
      excluded: ["91"],
    });
    expect(screen.getByLabelText("Select the value of LAB-90")).toBeChecked();

    fireEvent.click(screen.getByTestId("run-clear-selection"));
    expect(screen.queryByTestId("run-selection")).not.toBeInTheDocument();
  });

  it("adds a row outside the filter chip to a select-all-in-filter selection", () => {
    renderRun();
    fireEvent.click(screen.getByTestId("run-chip-CRITICAL"));
    fireEvent.click(screen.getByLabelText("Select the value of LAB-90"));
    fireEvent.click(screen.getByTestId("run-select-all"));
    fireEvent.click(screen.getByTestId("run-chip-ALL"));

    const outside = screen.getByLabelText("Select the value of LAB-10");
    expect(outside).not.toBeChecked();
    fireEvent.click(outside);

    expect(screen.getByLabelText("Select the value of LAB-10")).toBeChecked();
    expect(summaries.at(-1)).toEqual({
      all: true,
      chip: "CRITICAL",
      ids: ["10"],
      excluded: [],
    });

    fireEvent.click(screen.getByLabelText("Select the value of LAB-10"));
    expect(
      screen.getByLabelText("Select the value of LAB-10"),
    ).not.toBeChecked();
    expect(summaries.at(-1)).toEqual({
      all: true,
      chip: "CRITICAL",
      ids: [],
      excluded: [],
    });
  });

  it("says when the selection cannot be counted and keeps the run on screen", () => {
    let fail = true;
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (endPoint: string, body: string, callback: (r: unknown) => void) =>
        callback(
          fail
            ? { error: "Service Unavailable", status: 503 }
            : {
                selected: 1,
                critical: 0,
                abnormal: 0,
                normal: 1,
                skipped: [],
              },
        ),
    );
    renderRun();

    fireEvent.click(screen.getByLabelText("Select the value of LAB-10"));

    expect(screen.getByTestId("run-selection-failed")).toHaveTextContent(
      "The selection could not be counted.",
    );
    expect(screen.getByTestId("run-row-10")).toBeInTheDocument();

    fail = false;
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(
      screen.queryByTestId("run-selection-failed"),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("run-selection")).toHaveTextContent(
      "Selected values: 1 (0 critical, 0 abnormal, 1 normal).",
    );
  });
});
