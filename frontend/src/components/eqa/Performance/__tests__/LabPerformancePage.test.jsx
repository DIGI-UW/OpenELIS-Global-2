import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import LabPerformancePage from "../LabPerformancePage";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const ROLLUP = {
  kpis: {
    acceptanceRate: 84,
    priorAcceptanceRate: 78,
    acceptanceDelta: 6,
    scoredCount: 25,
    acceptableCount: 21,
    onTimeRate: 92,
    submittedCount: 24,
    lateCount: 2,
    eqaNceCount: 3,
    eqaNceOpenCount: 1,
    uncoveredTestCount: 1,
  },
  coverage: [
    {
      section: "Serology",
      schemeId: 1,
      schemeName: "National HIV PT",
      acceptanceRate: 75,
      cells: [
        { cycleId: 1, cycleLabel: "2025 R1", verdict: "acceptable" },
        { cycleId: 2, cycleLabel: "2025 R2", verdict: "questionable" },
        { cycleId: 3, cycleLabel: "2026 R1", verdict: "acceptable" },
        { cycleId: 4, cycleLabel: "2026 R2", verdict: "acceptable" },
      ],
    },
    {
      section: "Haematology",
      schemeId: 2,
      schemeName: "Regional FBC PT",
      acceptanceRate: 50,
      cells: [
        { cycleId: 9, cycleLabel: "2026 R1", verdict: "unacceptable" },
        { cycleId: 10, cycleLabel: "2026 R2", verdict: "acceptable" },
      ],
    },
  ],
  gaps: [
    { testId: "77", testName: "TB smear microscopy", bodyCodes: ["SANAS"] },
  ],
  recentCycles: [
    {
      cycleId: 4,
      cycleLabel: "2026 R2",
      schemeName: "National HIV PT",
      status: "SCORED",
      scoredCount: 4,
      acceptableCount: 3,
      performance: "questionable",
      submittedAt: "2026-07-14",
    },
    {
      cycleId: 10,
      cycleLabel: "2026 R2",
      schemeName: "Regional FBC PT",
      status: "SUBMITTED",
      scoredCount: 0,
      acceptableCount: 0,
      performance: null,
      submittedAt: null,
    },
  ],
};

const renderPage = (view) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <LabPerformancePage view={view} />
      </MemoryRouter>
    </IntlProvider>,
  );

describe("LabPerformancePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback(ROLLUP),
    );
  });

  it("renders the twelve-month KPI row", async () => {
    renderPage("coverage");

    expect(await screen.findByTestId("kpi-acceptance")).toHaveTextContent(
      "84%",
    );
    expect(screen.getByTestId("kpi-acceptance")).toHaveTextContent(
      "+6% vs. prior year",
    );
    expect(screen.getByTestId("kpi-ontime")).toHaveTextContent("2 late of 24");
    expect(screen.getByTestId("kpi-uncovered")).toHaveTextContent("1");
  });

  it("deep-links the NCE tile to the register's EQA source filter", async () => {
    renderPage("coverage");

    const tile = await screen.findByTestId("kpi-nce");
    expect(tile).toHaveAttribute("href", "/NceDashboard?source=eqa");
    expect(tile).toHaveTextContent("1 open");
  });

  it("pads a short scheme on the left so Most recent stays the last column", async () => {
    const { container } = renderPage("coverage");

    await screen.findByText("Regional FBC PT");
    const rows = container.querySelectorAll("tbody tr");
    const shortRow = [...rows].find((row) =>
      row.textContent.includes("Regional FBC PT"),
    );
    const glyphs = [...shortRow.querySelectorAll("td")]
      .slice(2, 6)
      .map((cell) => cell.textContent);
    // Two cycles, four columns: the pair sits at the right-hand end.
    expect(glyphs).toEqual(["—", "—", "!", "A"]);
  });

  it("calls out the accredited tests with no EQA cover", async () => {
    renderPage("coverage");

    expect(await screen.findByTestId("coverage-gap-callout")).toHaveTextContent(
      "TB smear microscopy",
    );
  });

  it("shows recent cycles with a pending verdict where nothing is scored", async () => {
    renderPage("recent");

    expect(await screen.findByText("National HIV PT")).toBeInTheDocument();
    expect(screen.getByText("3 of 4")).toBeInTheDocument();
    expect(screen.getByText("14/07/2026")).toBeInTheDocument();
    expect(screen.getByText("pending")).toBeInTheDocument();
  });

  it("reads both views from one rollup call", async () => {
    renderPage("recent");

    await screen.findByText("National HIV PT");
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/eqa/lab-performance",
      expect.any(Function),
    );
  });
});
