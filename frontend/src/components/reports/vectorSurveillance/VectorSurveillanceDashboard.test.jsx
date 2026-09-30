import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import VectorSurveillanceDashboard from "./VectorSurveillanceDashboard";
import {
  getSurveillanceIndices,
  getSurveillanceSites,
} from "./VectorSurveillanceService";

// Mock the REST client so we control the /indices + /sites payloads.
vi.mock("./VectorSurveillanceService", () => ({
  getSurveillanceIndices: vi.fn(),
  getSurveillanceSites: vi.fn(),
}));

// @carbon/charts-react renders a heavy D3 chart that is brittle under jsdom and
// irrelevant to these assertions — replace each chart with a marker that echoes
// its data so we can assert the figures fed into it come from the payload.
vi.mock("@carbon/charts-react", () => ({
  StackedAreaChart: ({ data }) => (
    <div data-testid="area-chart">{JSON.stringify(data)}</div>
  ),
  DonutChart: ({ data }) => (
    <div data-testid="donut-chart">{JSON.stringify(data)}</div>
  ),
  SimpleBarChart: ({ data }) => (
    <div data-testid="bar-chart">{JSON.stringify(data)}</div>
  ),
}));

// CustomDatePicker -> a plain input that forwards onChange with a date string.
vi.mock("../../common/CustomDatePicker", () => ({
  default: ({ id, value, onChange }) => (
    <input
      data-testid={id}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  ),
}));

vi.mock("../../common/PageBreadCrumb", () => ({
  default: () => <nav data-testid="breadcrumb" />,
}));

// The dashboard is a routed page and now reads filters from the URL, so it
// needs Router context just as it has in the running app.
const renderWithIntl = (component) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </MemoryRouter>,
  );

const mockIndices = {
  freshness: "2026-06-10T08:30:00Z",
  collectionDensity: [
    {
      periodLabel: "2026-W23",
      siteId: 7,
      siteName: "Denpasar",
      poolCount: 12,
      specimenCount: 480,
    },
  ],
  speciesDistribution: [
    {
      speciesId: 1,
      genus: "Aedes",
      species: "aegypti",
      specimenCount: 300,
      pct: 62.5,
    },
  ],
  mirBySpecies: [
    {
      speciesId: 1,
      speciesLabel: "Aedes aegypti",
      pathogen: "Dengue Virus Detection",
      mirClassic: 4.17,
      infectionRateObserved: 3.9,
      positiveResolutionPct: 80.0,
      positivePools: 2,
      totalSpecimens: 480,
    },
    {
      speciesId: 2,
      speciesLabel: "Anopheles gambiae",
      pathogen: "Malaria Parasite Detection",
      mirClassic: 7.5,
      infectionRateObserved: 6.25,
      positiveResolutionPct: 50.0,
      positivePools: 1,
      totalSpecimens: 133,
    },
  ],
  pathogenPositivity: [
    {
      pathogen: "Dengue Virus Detection",
      poolsPositive: 2,
      poolsTested: 12,
      positivityPct: 16.7,
    },
  ],
  qcPassRate: { analysesPassed: 47, analysesTotal: 50, passRatePct: 94.0 },
  sporozoiteRatePct: 1.5,
  positivityConfigured: true,
};

// Positivity classification absent: data exists (incl. NON-zero positivity) but
// the catalog has no significance tags. Proves the UI keys the "not configured"
// state off the `positivityConfigured` flag, not off a 0% value.
const notConfiguredIndices = {
  ...mockIndices,
  positivityConfigured: false,
};

const emptyIndices = {
  freshness: "2026-06-10T08:30:00Z",
  collectionDensity: [],
  speciesDistribution: [],
  mirBySpecies: [],
  pathogenPositivity: [],
  qcPassRate: { analysesPassed: 0, analysesTotal: 0, passRatePct: 0 },
};

const mockSites = [
  { id: 7, code: "DPS", name: "Denpasar" },
  { id: 9, code: "SBY", name: "Surabaya" },
];

describe("VectorSurveillanceDashboard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getSurveillanceSites.mockImplementation((cb) => cb(mockSites));
  });

  const applyFilters = () => {
    fireEvent.change(screen.getByTestId("vector-date-from"), {
      target: { value: "01/06/2026" },
    });
    fireEvent.change(screen.getByTestId("vector-date-to"), {
      target: { value: "07/06/2026" },
    });
    // Carbon Select renders a real <select>; pick site id 7.
    fireEvent.change(screen.getByLabelText(messages["vectorReport.filter.site"]), {
      target: { value: "7" },
    });
    fireEvent.click(screen.getByTestId("vector-apply"));
  };

  test("Apply requests /indices with dateFrom, dateTo and siteId", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(mockIndices));

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    await waitFor(() => {
      expect(getSurveillanceIndices).toHaveBeenCalledTimes(1);
    });
    const scope = getSurveillanceIndices.mock.calls[0][0];
    // encodeDate replaces "/" with "%2F"
    expect(scope.dateFrom).toBe("01%2F06%2F2026");
    expect(scope.dateTo).toBe("07%2F06%2F2026");
    expect(scope.siteId).toBe("7");
  });

  test("renders figures from the payload (not a render-only test)", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(mockIndices));

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    // Species donut fed with the payload's genus/species + count.
    await waitFor(() => {
      expect(screen.getByTestId("donut-chart")).toHaveTextContent("Aedes aegypti");
    });
    expect(screen.getByTestId("donut-chart")).toHaveTextContent("300");

    // Density stacked-area fed with the specimenCount (480).
    expect(screen.getByTestId("area-chart")).toHaveTextContent("480");

    // Bar chart fed with the positivity pct (16.7).
    expect(screen.getByTestId("bar-chart")).toHaveTextContent("16.7");

    // QC KPI from payload.
    expect(screen.getByText("94.0%")).toBeInTheDocument();

    // MIR table: per species × pathogen. Two rows; scope assertions to each
    // mir-row so the classic/observed cells don't produce ambiguous matches.
    const mirPanel = screen.getByTestId("panel-mir");
    const mirRows = within(mirPanel).getAllByTestId("mir-row");
    expect(mirRows).toHaveLength(2);
    expect(within(mirRows[0]).getByText("Aedes aegypti")).toBeInTheDocument();
    expect(
      within(mirRows[0]).getByText("Dengue Virus Detection"),
    ).toBeInTheDocument();
    expect(within(mirRows[0]).getByText("4.17")).toBeInTheDocument(); // mirClassic
    expect(within(mirRows[0]).getByText("3.90")).toBeInTheDocument(); // observed (distinct)
    expect(
      within(mirRows[1]).getByText("Anopheles gambiae"),
    ).toBeInTheDocument();

    // Sporozoite KPI is a top-level computed figure (not a per-row column).
    expect(screen.getByTestId("vector-sporozoite")).toHaveTextContent("1.50%");
  });

  test("positivity not-configured: panels hidden + notice shown (keyed off the flag, not 0%)", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) =>
      cb(notConfiguredIndices),
    );

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    await waitFor(() => {
      expect(
        screen.getByTestId("vector-positivity-not-configured"),
      ).toBeInTheDocument();
    });
    // Non-zero positivity data is present, yet the flag drives the notice.
    expect(
      screen.getByTestId("vector-positivity-not-configured"),
    ).toHaveTextContent(messages["vectorReport.positivity.notConfigured"]);
    // Positivity-dependent panels are hidden.
    expect(screen.queryByTestId("panel-mir")).not.toBeInTheDocument();
    expect(screen.queryByTestId("panel-positivity")).not.toBeInTheDocument();
    expect(screen.queryByTestId("vector-sporozoite")).not.toBeInTheDocument();
    // Non-positivity panels still render.
    expect(screen.getByTestId("panel-density")).toBeInTheDocument();
    expect(screen.getByTestId("panel-species")).toBeInTheDocument();
    expect(screen.getByTestId("panel-qc")).toBeInTheDocument();
  });

  test("empty payload shows the empty state, not an error", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(emptyIndices));

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    await waitFor(() => {
      expect(screen.getByTestId("vector-empty")).toBeInTheDocument();
    });
    expect(screen.getByTestId("vector-empty")).toHaveTextContent(
      messages["vectorReport.empty"],
    );
    expect(screen.queryByTestId("vector-error")).not.toBeInTheDocument();
    expect(screen.queryByTestId("panel-mir")).not.toBeInTheDocument();
  });

  test("uses i18n keys for visible chrome (title, apply, export)", () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(mockIndices));

    renderWithIntl(<VectorSurveillanceDashboard />);

    expect(
      screen.getByText(messages["vectorReport.title"]),
    ).toBeInTheDocument();
    expect(
      screen.getByText(messages["vectorReport.exportPdf"]),
    ).toBeInTheDocument();
    expect(
      screen.getByText(messages["vectorReport.filter.apply"]),
    ).toBeInTheDocument();
  });

  test("freshness indicator reflects the payload timestamp", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(mockIndices));

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    await waitFor(() => {
      expect(screen.getByTestId("vector-freshness")).toBeInTheDocument();
    });
    // Rendered freshness contains the localized payload timestamp's year.
    expect(screen.getByTestId("vector-freshness")).toHaveTextContent("2026");
  });

  test("shared link with URL filters auto-runs the report once on load (no Apply click)", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(mockIndices));

    render(
      <MemoryRouter
        initialEntries={[
          "/VectorSurveillanceReport?dateFrom=01%2F06%2F2026&dateTo=07%2F06%2F2026&siteId=7",
        ]}
      >
        <IntlProvider locale="en" messages={messages}>
          <VectorSurveillanceDashboard />
        </IntlProvider>
      </MemoryRouter>,
    );

    // The report runs because the link carried filters — exactly once (pushing
    // the same filters back to the URL must not trigger a second query).
    await waitFor(() => {
      expect(getSurveillanceIndices).toHaveBeenCalledTimes(1);
    });
    const scope = getSurveillanceIndices.mock.calls[0][0];
    expect(scope.dateFrom).toBe("01%2F06%2F2026");
    expect(scope.dateTo).toBe("07%2F06%2F2026");
    expect(scope.siteId).toBe("7");
    // The payload rendered without any interaction.
    expect(screen.getByTestId("donut-chart")).toBeInTheDocument();
  });

  test("density panel shows the honest 'specimen counts' label, not 'collection density'", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(mockIndices));

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    // The title must render the current honest key. Reverting to a hardcoded
    // "Collection density" (which overstates raw counts as effort-normalized) fails here.
    const density = await screen.findByTestId("panel-density");
    expect(
      within(density).getByText(messages["vectorReport.density.title"]),
    ).toBeInTheDocument();
  });

  test("density panel shows both abundance and per-trap-night density, with an effort-not-recorded fallback", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) =>
      cb({
        ...mockIndices,
        collectionDensity: [
          {
            periodLabel: "2026-W23",
            siteId: 7,
            siteName: "Denpasar",
            poolCount: 12,
            specimenCount: 480,
            trapNights: 96,
            density: 5.0,
          },
          {
            periodLabel: "2026-W23",
            siteId: 9,
            siteName: "Surabaya",
            poolCount: 4,
            specimenCount: 60,
            trapNights: null,
            density: null,
          },
        ],
      }),
    );

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    const panel = await screen.findByTestId("panel-density");
    // Abundance (raw specimen counts) is always present.
    expect(within(panel).getByText("480")).toBeInTheDocument();
    expect(within(panel).getByText("60")).toBeInTheDocument();
    // Density (organisms per trap-night) shown when effort was recorded: 480 / 96 = 5.00.
    expect(within(panel).getByText("5.00")).toBeInTheDocument();
    // Degrade: the site with no recorded effort shows the fallback, not a fabricated rate.
    expect(
      within(panel).getByText(messages["vectorReport.density.effortNotRecorded"]),
    ).toBeInTheDocument();
  });

  test("sporozoite tile carries the deconvolution-confidence caveat", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(mockIndices));

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    // The caveat must accompany the sporozoite figure (superseding the old
    // "withheld until 95%" wording). Dropping the note fails this test.
    const spo = await screen.findByTestId("vector-sporozoite");
    expect(
      within(spo).getByText(messages["vectorReport.mir.sporozoiteNote"]),
    ).toBeInTheDocument();
  });

  test("warns when a result carries an unrecognized significance classification", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) =>
      cb({ ...mockIndices, positivityClassificationUnrecognized: true }),
    );

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    const warning = await screen.findByTestId("vector-positivity-unrecognized");
    expect(warning).toHaveTextContent(
      messages["vectorReport.positivity.unrecognized"],
    );
  });

  test("no unrecognized-classification warning when every significance is recognized", async () => {
    getSurveillanceIndices.mockImplementation((scope, cb) => cb(mockIndices));

    renderWithIntl(<VectorSurveillanceDashboard />);
    applyFilters();

    await waitFor(() =>
      expect(screen.getByTestId("donut-chart")).toBeInTheDocument(),
    );
    expect(
      screen.queryByTestId("vector-positivity-unrecognized"),
    ).not.toBeInTheDocument();
  });
});
