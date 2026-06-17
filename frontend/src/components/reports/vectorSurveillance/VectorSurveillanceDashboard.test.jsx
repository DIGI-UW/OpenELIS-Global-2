import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
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
  LineChart: ({ data }) => (
    <div data-testid="line-chart">{JSON.stringify(data)}</div>
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

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
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
      pathogen: "DENV",
      mirClassic: 4.17,
      infectionRateObserved: 3.9,
      positiveResolutionPct: 80.0,
      sporozoiteRatePct: null,
      positivePools: 2,
      totalSpecimens: 480,
    },
  ],
  pathogenPositivity: [
    { pathogen: "DENV", poolsPositive: 2, poolsTested: 12, positivityPct: 16.7 },
  ],
  qcPassRate: { analysesPassed: 47, analysesTotal: 50, passRatePct: 94.0 },
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

    // Line chart fed with the density specimenCount (480).
    expect(screen.getByTestId("line-chart")).toHaveTextContent("480");

    // Bar chart fed with the positivity pct (16.7).
    expect(screen.getByTestId("bar-chart")).toHaveTextContent("16.7");

    // QC KPI from payload.
    expect(screen.getByText("94.0%")).toBeInTheDocument();

    // MIR table cells derived from payload.
    const mirPanel = screen.getByTestId("panel-mir");
    expect(within(mirPanel).getByText("DENV")).toBeInTheDocument();
    expect(within(mirPanel).getByText("4.17")).toBeInTheDocument(); // mirClassic
    expect(within(mirPanel).getByText("80.0%")).toBeInTheDocument(); // resolution
    // sporozoiteRatePct is null -> withheld (i18n key)
    expect(
      within(mirPanel).getByText(messages["vectorReport.mir.withheld"]),
    ).toBeInTheDocument();
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
});
