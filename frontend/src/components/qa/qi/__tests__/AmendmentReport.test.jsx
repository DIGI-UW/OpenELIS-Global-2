import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import AmendmentReport from "../AmendmentReport";

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
  LineChart: () => <div data-testid="amendment-trend-chart" />,
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
  await act(async () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <MemoryRouter>
          <AmendmentReport />
        </MemoryRouter>
      </IntlProvider>,
    ),
  );
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AmendmentReport", () => {
  test("renders amendment rows with prior and current values", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/reports/amendment/detail")) {
        callback(DETAIL);
      }
    });
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
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/reports/amendment/detail?fromDate="),
      expect.any(Function),
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("page=0&pageSize=25"),
      expect.any(Function),
    );
  });

  test("renders calm empty state when there are no amendments", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ totalCount: 0, page: 0, pageSize: 25, items: [] }),
    );
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
    getFromOpenElisServer.mockImplementation((url, callback) => callback());
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
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/reports/amendment/detail")) {
        callback(DETAIL);
      } else if (url.includes("/rest/reports/amendment/trend")) {
        callback(TREND);
      } else if (url.includes("/rest/reports/amendment/breakdown")) {
        callback(BREAKDOWN);
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
    expect(screen.getByText("1 amended of 40 released")).toBeInTheDocument();

    // trend chart rendered (stubbed)
    expect(screen.getByTestId("amendment-trend-chart")).toBeInTheDocument();

    // breakdown table
    expect(screen.getByText("By test")).toBeInTheDocument();
    expect(screen.getByText("40")).toBeInTheDocument();
  });

  test("rate tag stays gray when the indicator has no thresholds", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/reports/amendment/trend")) {
        callback({
          points: [
            {
              period: "2026-07-01",
              amendedCount: 1,
              releasedCount: 40,
              ratePercent: 2.5,
            },
          ],
        });
      } else if (url.includes("/rest/qi-config/resolve")) {
        callback({ indicatorKey: "AMENDMENT", enabled: true });
      }
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
