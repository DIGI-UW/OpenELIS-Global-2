import React from "react";
import { act, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import AmendmentReport from "../AmendmentReport";
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
  LineChart: () => <div data-testid="amendment-trend-chart" />,
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
      priorValue: "85.0",
      currentValue: "92.0",
      amendedBy: "John Doe",
      amendedAt: "2026-07-08T10:15:00Z",
      releasedAt: "2026-07-07T13:00:00Z",
      minutesToAmend: 1275, // 21h 15m
    },
    {
      analysisId: "2",
      labNumber: "13333",
      testName: "Urinalysis",
      priorValue: "Positive",
      currentValue: "Negative",
      amendedBy: "Jane Roe",
      amendedAt: "2026-07-08T11:00:00Z",
      releasedAt: null,
      minutesToAmend: null,
    },
  ],
};

const renderPage = async () => {
  await act(async () => {
    renderQa(<AmendmentReport />, { entries: ["/qa/qi/amendment"] });
  });
  await act(async () => {});
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AmendmentReport", () => {
  test("renders amendment rows with prior and current values", async () => {
    mockServer({ "/rest/reports/amendment/detail": DETAIL });
    await renderPage();

    expect(
      screen.getByRole("heading", { name: "Amendment Rate — Detail" }),
    ).toBeInTheDocument();

    // row 1: full data
    expect(screen.getByText("12345")).toBeInTheDocument();
    expect(screen.getByText("Complete Blood Count")).toBeInTheDocument();
    expect(screen.getByText("85.0")).toBeInTheDocument();
    expect(screen.getByText("92.0")).toBeInTheDocument();
    expect(screen.getByText("John Doe")).toBeInTheDocument();
    expect(screen.getByText("21h 15m")).toBeInTheDocument();

    // row 2: missing released/minutes render as em dashes
    expect(screen.getByText("13333")).toBeInTheDocument();
    expect(screen.getByText("Jane Roe")).toBeInTheDocument();
    expect(screen.getAllByText("—").length).toBeGreaterThanOrEqual(2);

    // paged fetch with default window
    expect(fetchFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/reports/amendment/detail?fromDate="),
      expect.anything(),
    );
    expect(fetchFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("page=0&pageSize=25"),
      expect.anything(),
    );
  });

  test("renders calm empty state when there are no amendments", async () => {
    mockServer({
      "/rest/reports/amendment/": {
        totalCount: 0,
        page: 0,
        pageSize: 25,
        items: [],
      },
    });
    await renderPage();

    expect(
      screen.getByText("No amendments in this window"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        /Amendments appear here when a released result is corrected/,
      ),
    ).toBeInTheDocument();
  });

  test("renders error state when the endpoint returns no data", async () => {
    mockServer({});
    await renderPage();

    // trend section and detail table each surface the error independently
    expect(
      screen.getAllByText("Amendment data is unavailable.").length,
    ).toBeGreaterThanOrEqual(1);
  });

  test("renders rate header, trend chart and per-test breakdown (OGC-710)", async () => {
    const TREND = {
      points: [
        {
          period: "2026-07-01",
          amendedCount: 1,
          releasedCount: 40,
          ratePercent: 2.5,
        },
      ],
    };
    const BREAKDOWN = {
      rows: [
        {
          testName: "Complete Blood Count",
          amendedCount: 1,
          releasedCount: 40,
          ratePercent: 2.5,
        },
      ],
    };
    // 2.5% sits between target (1) and action (5) -> amber band
    const CONFIG = {
      indicatorKey: "AMENDMENT",
      enabled: true,
      target: 1,
      action: 5,
      direction: "LOWER_BETTER",
    };
    mockServer({
      "/rest/reports/amendment/detail": DETAIL,
      "/rest/reports/amendment/trend": TREND,
      "/rest/reports/amendment/breakdown": BREAKDOWN,
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
    expect(screen.getByText("1 amended of 40 released")).toBeInTheDocument();

    // trend chart rendered (stubbed)
    expect(screen.getByTestId("amendment-trend-chart")).toBeInTheDocument();

    // breakdown table
    expect(screen.getByText("By test")).toBeInTheDocument();
    expect(screen.getByText("40")).toBeInTheDocument();
  });

  test("rate tag stays gray when the indicator has no thresholds", async () => {
    mockServer({
      "/rest/reports/amendment/trend": {
        points: [
          {
            period: "2026-07-01",
            amendedCount: 1,
            releasedCount: 40,
            ratePercent: 2.5,
          },
        ],
      },
      "/rest/qi-config/resolve": { indicatorKey: "AMENDMENT", enabled: true },
    });
    await renderPage();

    const tag = screen
      .getAllByText("2.50%")
      .map((el) => el.closest(".amendment-rate-tag"))
      .find(Boolean);
    expect(tag).toBeTruthy();
    expect(tag.className).not.toContain("qi-rate-tag--amber");
  });
});
