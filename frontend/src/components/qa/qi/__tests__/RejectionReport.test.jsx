import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import RejectionReport from "../RejectionReport";

vi.mock("../../../utils/Utils", () => ({
  toLocalIsoDate: (d) =>
    d instanceof Date
      ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`
      : d || "",
  toLocalIsoDateTime: (value) => {
    if (!value) return "\u2014";
    const d = new Date(value);
    const hh = String(d.getHours()).padStart(2, "0");
    const mm = String(d.getMinutes()).padStart(2, "0");
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${hh}:${mm}`;
  },
  getFromOpenElisServer: vi.fn(),
}));

// jsdom can't render @carbon/charts (SVG/resize observers) — stub it
vi.mock("@carbon/charts-react", () => ({
  LineChart: () => <div data-testid="rejection-trend-chart" />,
  DonutChart: () => <div data-testid="rejection-reason-donut" />,
  SimpleBarChart: () => <div data-testid="rejection-test-bars" />,
}));

import { getFromOpenElisServer } from "../../../utils/Utils";

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
  await act(async () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <MemoryRouter>
          <RejectionReport />
        </MemoryRouter>
      </IntlProvider>,
    ),
  );
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("RejectionReport", () => {
  test("renders rejection rows with reason and user", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/reports/rejection/detail")) {
        callback(DETAIL);
      }
    });
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
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/reports/rejection/detail?fromDate="),
      expect.any(Function),
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("page=0&pageSize=25"),
      expect.any(Function),
    );
  });

  test("renders calm empty state when there are no rejections", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/reports/rejection/detail")) {
        callback({ totalCount: 0, page: 0, pageSize: 25, items: [] });
      } else if (url.includes("/rest/reports/rejection/trend")) {
        callback({ points: [] });
      } else if (url.includes("/rest/reports/rejection/breakdown")) {
        callback({ reasons: [], tests: [] });
      }
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
    getFromOpenElisServer.mockImplementation((url, callback) => callback());
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
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/reports/rejection/detail")) {
        callback(DETAIL);
      } else if (url.includes("/rest/reports/rejection/trend")) {
        callback(TREND);
      } else if (url.includes("/rest/reports/rejection/breakdown")) {
        callback(BREAKDOWN);
      } else if (url.includes("/rest/reports/rejection/heatmap")) {
        callback(HEATMAP);
      } else if (url.includes("/rest/qi-config/resolve")) {
        callback(CONFIG);
      }
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
