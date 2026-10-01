import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import LabPerformancePage, {
  coverageCsv,
  recentCyclesCsv,
} from "../LabPerformancePage";
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

  // F-25: the two sibling pages in this lane already export, and these two
  // views -- the section x scheme matrix and the cycles behind it -- are the
  // laboratory's ISO 15189 evidence, which until now left the screen only as a
  // screenshot.
  it("exports the coverage matrix cell for cell, with the attested numbers above it", async () => {
    renderPage("coverage");
    await screen.findByText("National HIV PT");

    const csv = coverageCsv(
      ROLLUP.kpis,
      ROLLUP.coverage,
      ROLLUP.gaps,
      4,
      (_key, fallback) => fallback,
      (_key, fallback) => fallback,
    );
    const lines = csv.split("\n");

    // The KPI block: the numbers being attested, not just the grid.
    expect(lines[0]).toBe('"Acceptance rate (12 mo)","84%"');
    expect(lines).toContain('"On-time submission rate","92%"');
    expect(lines).toContain('"Late submissions","2"');
    expect(lines).toContain('"EQA-triggered NCEs (12 mo)","3"');
    expect(lines).toContain('"Accredited tests without EQA","1"');

    const header = lines.find((line) => line.startsWith('"Section"'));
    expect(header).toBe(
      '"Section","Scheme","Cycle -3","Cycle -2","Cycle -1","Most recent","Acceptance rate"',
    );
    // Four cycles: every column filled, each carrying its cycle and verdict.
    expect(lines).toContain(
      '"Serology","National HIV PT","2025 R1: acceptable","2025 R2: questionable","2026 R1: acceptable","2026 R2: acceptable","75%"',
    );
    // Two cycles: padded on the LEFT, exactly as the table pads them, so the
    // newest cycle stays under "Most recent".
    expect(lines).toContain(
      '"Haematology","Regional FBC PT","","","2026 R1: unacceptable","2026 R2: acceptable","50%"',
    );
    // The uncovered tests travel with the file they justify.
    expect(lines).toContain(
      '"Accredited without EQA cover:","TB smear microscopy"',
    );
  });

  it("exports recent cycles honouring the scheme filter on screen", async () => {
    renderPage("recent");
    await screen.findByText("National HIV PT");

    const all = recentCyclesCsv(
      ROLLUP.kpis,
      ROLLUP.recentCycles,
      (_key, fallback) => fallback,
      (_key, fallback) => fallback,
    ).split("\n");
    expect(all).toContain(
      '"National HIV PT","2026 R2","SCORED","3 of 4","questionable","14/07/2026"',
    );
    // Nothing scored and nothing submitted reads as empty, not as "pending"
    // or an em dash -- a spreadsheet cell is not a screen.
    expect(all).toContain('"Regional FBC PT","2026 R2","SUBMITTED","","",""');

    const filtered = recentCyclesCsv(
      ROLLUP.kpis,
      ROLLUP.recentCycles.filter((cycle) =>
        cycle.schemeName.toLowerCase().includes("hiv"),
      ),
      (_key, fallback) => fallback,
      (_key, fallback) => fallback,
    ).split("\n");
    expect(filtered.some((line) => line.includes("National HIV PT"))).toBe(
      true,
    );
    expect(filtered.some((line) => line.includes("Regional FBC PT"))).toBe(
      false,
    );
  });

  it("offers the export on both views", async () => {
    renderPage("coverage");
    await screen.findByText("National HIV PT");
    expect(
      screen.getByRole("button", { name: /Export CSV/ }),
    ).toBeInTheDocument();

    renderPage("recent");
    expect(
      screen.getAllByRole("button", { name: /Export CSV/ }).length,
    ).toBeGreaterThan(0);
  });

  it("hands the browser a file when the export is clicked", async () => {
    const createObjectURL = vi.fn(() => "blob:eqa");
    const revokeObjectURL = vi.fn();
    global.URL.createObjectURL = createObjectURL;
    global.URL.revokeObjectURL = revokeObjectURL;
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => {});

    renderPage("coverage");
    await screen.findByText("National HIV PT");
    fireEvent.click(screen.getByRole("button", { name: /Export CSV/ }));

    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(click).toHaveBeenCalledTimes(1);
    click.mockRestore();
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
