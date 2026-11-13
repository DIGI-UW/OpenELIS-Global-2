import React from "react";
import { act, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import RejectionReport from "../RejectionReport";
import { renderQa } from "../../testUtils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    fetchFromOpenElisServer: vi.fn(),
  };
});

// jsdom can't render @carbon/charts (SVG/resize observers) — stub it
vi.mock("@carbon/charts-react", () => ({
  LineChart: () => <div data-testid="rejection-trend-chart" />,
  DonutChart: () => <div data-testid="rejection-reason-donut" />,
  SimpleBarChart: () => <div data-testid="rejection-test-bars" />,
}));

import { fetchFromOpenElisServer } from "../../../utils/Utils";

/** Serve each endpoint from `routes`; anything unlisted reads as unavailable. */
const mockServer = (routes) =>
  fetchFromOpenElisServer.mockImplementation((url) => {
    const match = Object.keys(routes).find((fragment) =>
      url.includes(fragment),
    );
    return match
      ? Promise.resolve(routes[match])
      : Promise.reject(new Error(`no data: ${url}`));
  });

const DETAIL = {
  totalCount: 2,
  page: 0,
  pageSize: 25,
  items: [
    {
      analysisId: "1",
      labNumber: "12345",
      testName: "Complete Blood Count",
      reason: "Hemolyzed specimen",
      rejectedBy: "John Doe",
      rejectedAt: "2026-07-08T10:15:00Z",
      location: "Inpatient Ward",
      nceNumber: "NCE-2026-00042",
    },
    {
      analysisId: "2",
      labNumber: "13333",
      testName: "Urinalysis",
      reason: null,
      rejectedBy: null,
      rejectedAt: "2026-07-08T11:00:00Z",
      location: null,
      nceNumber: null,
    },
  ],
};

const HEATMAP = {
  cells: [
    {
      location: "Inpatient Ward",
      section: "Chemistry",
      totalCount: 40,
      rejectedCount: 1,
      ratePercent: 2.5,
    },
    {
      location: null,
      section: "Hematology",
      totalCount: 10,
      rejectedCount: 0,
      ratePercent: 0,
    },
  ],
};

const TREND = {
  points: [
    {
      period: "2026-07-01",
      rejectedCount: 1,
      totalCount: 40,
      ratePercent: 2.5,
    },
  ],
};

const BREAKDOWN = {
  reasons: [
    {
      reason: "Hemolyzed specimen",
      count: 2,
      percentOfRejections: 66.67,
      cumulativePercent: 66.67,
    },
    {
      reason: "Insufficient volume",
      count: 1,
      percentOfRejections: 33.33,
      cumulativePercent: 100.0,
    },
  ],
  tests: [
    {
      testName: "Complete Blood Count",
      rejectedCount: 1,
      totalCount: 40,
      ratePercent: 2.5,
    },
  ],
};

const renderPage = async () => {
  await act(async () => {
    renderQa(<RejectionReport />, { entries: ["/qa/qi/rejection"] });
  });
  await act(async () => {});
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("RejectionReport", () => {
  test("renders rejection rows with reason and user", async () => {
    mockServer({ "/rest/reports/rejection/detail": DETAIL });
    await renderPage();

    expect(
      screen.getByRole("heading", { name: "Rejection Rate — Detail" }),
    ).toBeInTheDocument();

    // row 1: full data
    expect(screen.getByText("12345")).toBeInTheDocument();
    expect(screen.getByText("Complete Blood Count")).toBeInTheDocument();
    expect(screen.getByText("Hemolyzed specimen")).toBeInTheDocument();
    expect(screen.getByText("John Doe")).toBeInTheDocument();

    // row 2: missing reason/user render as em dashes
    expect(screen.getByText("13333")).toBeInTheDocument();
    expect(screen.getAllByText("—").length).toBeGreaterThanOrEqual(2);

    // paged fetch with default window
    expect(fetchFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/reports/rejection/detail?fromDate="),
      expect.anything(),
    );
    expect(fetchFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("page=0&pageSize=25"),
      expect.anything(),
    );
  });

  test("renders calm empty state when there are no rejections", async () => {
    mockServer({
      "/rest/reports/rejection/detail": {
        totalCount: 0,
        page: 0,
        pageSize: 25,
        items: [],
      },
      "/rest/reports/rejection/trend": { points: [] },
      "/rest/reports/rejection/breakdown": { reasons: [], tests: [] },
    });
    await renderPage();

    expect(
      screen.getByText("No rejections in this window"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Rejections appear here when a test is rejected/),
    ).toBeInTheDocument();
  });

  test("renders error state when the endpoint returns no data", async () => {
    mockServer({});
    await renderPage();

    // trend section and detail table each surface the error independently
    expect(
      screen.getAllByText("Rejection data is unavailable.").length,
    ).toBeGreaterThanOrEqual(1);
  });

  test("renders rate header, trend chart, reason Pareto and per-test breakdown", async () => {
    // 2.5% sits between target (2) and action (5) -> amber band
    const CONFIG = {
      indicatorKey: "REJECTION",
      enabled: true,
      target: 2,
      action: 5,
      direction: "LOWER_BETTER",
    };
    mockServer({
      "/rest/reports/rejection/detail": DETAIL,
      "/rest/reports/rejection/trend": TREND,
      "/rest/reports/rejection/breakdown": BREAKDOWN,
      "/rest/reports/rejection/heatmap": HEATMAP,
      "/rest/qi-config/resolve": CONFIG,
    });
    await renderPage();

    // rate header: window rate derived from trend sums, amber-toned tag
    const rateTexts = screen.getAllByText("2.50%");
    const tag = rateTexts
      .map((el) => el.closest(".amendment-rate-tag"))
      .find(Boolean);
    expect(tag).toBeTruthy();
    expect(tag.className).toContain("qi-rate-tag--amber");
    expect(screen.getByText("1 rejected of 40 started")).toBeInTheDocument();

    // trend chart rendered (stubbed)
    expect(screen.getByTestId("rejection-trend-chart")).toBeInTheDocument();

    // reason Pareto: ordered with cumulative share ("Hemolyzed specimen"
    // appears here and in the detail list, hence getAllByText)
    expect(screen.getByText("By reason (Pareto)")).toBeInTheDocument();
    expect(screen.getAllByText("Hemolyzed specimen").length).toBeGreaterThan(1);
    // top row's share and cumulative are both 66.67% by construction
    expect(screen.getAllByText("66.67%")).toHaveLength(2);
    expect(screen.getByText("100.00%")).toBeInTheDocument();

    // by-test breakdown
    expect(screen.getByText("By test")).toBeInTheDocument();
    expect(screen.getByText("40")).toBeInTheDocument();

    // donut + bar visuals rendered (stubbed)
    expect(screen.getByTestId("rejection-reason-donut")).toBeInTheDocument();
    expect(screen.getByTestId("rejection-test-bars")).toBeInTheDocument();

    // Pareto insight sentence: cumulative crosses 80% at the second reason
    expect(
      screen.getByText(/Top 2 of 2 reasons account for 100%/),
    ).toBeInTheDocument();

    // heatmap: location columns (unknown bucket labeled), config-toned cells,
    // config-quoting legend
    expect(
      screen.getByText("Heatmap: ordering location × test section"),
    ).toBeInTheDocument();
    expect(screen.getByText("Unknown location")).toBeInTheDocument();
    expect(screen.getByText("2.5%")).toHaveClass("qi-heatmap__cell--amber");
    expect(screen.getByText("0.0%")).toHaveClass("qi-heatmap__cell--green");
    expect(screen.getByText(/Cells colored by rate/)).toBeInTheDocument();

    // list heading + location column + per-rejection NCE link ("Inpatient
    // Ward" shows in the heatmap header and the list row)
    expect(screen.getByText("Individual rejections")).toBeInTheDocument();
    expect(screen.getAllByText("Inpatient Ward").length).toBeGreaterThanOrEqual(
      2,
    );
    const nceLink = screen.getByRole("link", { name: "NCE-2026-00042" });
    expect(nceLink).toHaveAttribute(
      "href",
      "/ViewNonConformingEvent?nceNumber=NCE-2026-00042",
    );
  });
});
