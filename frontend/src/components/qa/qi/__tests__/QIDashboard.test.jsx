import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import QIDashboard from "../QIDashboard";
import { NotificationContext } from "../../../layout/Layout";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

import { getFromOpenElisServer } from "../../../utils/Utils";

// The dashboard mounts <AlertDialog/> (for the OGC-711 disabled-route redirect
// toast), which reads NotificationContext — provided by Layout in the app.
const notificationValue = {
  notifications: [],
  addNotification: vi.fn(),
  removeNotification: vi.fn(),
  setNotificationVisible: vi.fn(),
  notificationVisible: false,
};

// Rendering with the real en.json fails loudly if a referenced i18n key
// is missing (react-intl falls back to the raw key, breaking assertions).
const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={notificationValue}>
        <MemoryRouter>
          <QIDashboard />
        </MemoryRouter>
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const currentSummary = {
  totalCount: 2471,
  mean: 18.78, // 18h 47m
  breakdown: [
    { dimensionValue: "Chemistry", mean: 12.5, count: 2371 },
    { dimensionValue: "Microbiology", mean: 61, count: 100 },
  ],
};

const priorSummary = {
  totalCount: 2298,
  mean: 19.31, // delta = -32m => faster, "good"
  breakdown: [],
};

const currentAmendment = {
  amendedCount: 8,
  releasedCount: 2580,
  ratePercent: 0.31,
};

const priorAmendment = {
  amendedCount: 10,
  releasedCount: 2400,
  ratePercent: 0.42, // delta = -0.11% => fewer amendments, "good"
};

const currentCallback = {
  enabled: true,
  criticalCount: 4,
  confirmedCount: 4,
  compliancePercent: 100.0,
  target: 100,
};

const priorCallback = {
  enabled: true,
  criticalCount: 2,
  confirmedCount: 1,
  compliancePercent: 50.0, // delta = +50% => higher compliance, "good"
  target: 100,
};

// 3 critical pending (amber band) + 2 in corrective action; the rest is noise
// the predicates must ignore.
const nceList = [
  { id: "1", severity: "CRITICAL", status: "Pending" },
  { id: "2", severity: "CRITICAL", status: "Pending" },
  { id: "3", severity: "CRITICAL", status: "Pending" },
  { id: "4", severity: "CRITICAL", status: "Closed" },
  { id: "5", severity: "MAJOR", status: "Pending" },
  { id: "6", severity: "CRITICAL", status: "Corrective Action" },
  { id: "7", severity: "MINOR", status: "Corrective Action" },
];

// Each windowed endpoint fires current-window before prior-window.
const mockApis = ({
  tatCurrent = currentSummary,
  tatPrior = priorSummary,
  amendCurrent = currentAmendment,
  amendPrior = priorAmendment,
  callbackCurrent = currentCallback,
  callbackPrior = priorCallback,
  nce = nceList,
} = {}) => {
  let tatCall = 0;
  let amendCall = 0;
  let callbackCall = 0;
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/reports/tat/summary")) {
      callback(tatCall++ === 0 ? tatCurrent : tatPrior);
    } else if (url.includes("/rest/reports/amendment/summary")) {
      callback(amendCall++ === 0 ? amendCurrent : amendPrior);
    } else if (url.includes("/rest/critical-callback/summary")) {
      callback(callbackCall++ === 0 ? callbackCurrent : callbackPrior);
    } else if (url.includes("/rest/nce/dashboard")) {
      callback(nce === null ? undefined : { nceList: nce });
    } else if (url.includes("/rest/qi-config/resolve")) {
      callback({ enabled: true });
    }
  });
};

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
});

describe("QIDashboard", () => {
  test("renders five tiles in fixed order with a live TAT tile", async () => {
    mockApis();
    renderPage();

    const tiles = [
      "tat",
      "rejection",
      "amendment",
      "nce-pulse",
      "callback",
    ].map((id) => screen.getByTestId(`qi-tile-${id}`));
    expect(tiles[0]).toHaveTextContent("Average TAT");
    expect(tiles[1]).toHaveTextContent("Rejection Rate");
    expect(tiles[2]).toHaveTextContent("Amendment Rate");
    expect(tiles[3]).toHaveTextContent("NCE Pulse");
    expect(tiles[4]).toHaveTextContent("Critical Callback Compliance");

    await waitFor(() =>
      expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent("18h 47m"),
    );
    expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent("↓ 32m");
    expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent(
      "Across 2 lab units · Slowest: Microbiology (61h)",
    );
    const detailLinks = screen.getAllByRole("link", { name: /View detail/ });
    expect(detailLinks.map((l) => l.getAttribute("href"))).toEqual([
      "/qa/qi/tat",
      "/qa/qi/amendment",
      "/NceDashboard?severity=CRITICAL&status=Pending",
      "/qa/qi/callback",
    ]);
  });

  test("amendment tile shows rate, improving delta, and counts", async () => {
    mockApis();
    renderPage();

    const tile = screen.getByTestId("qi-tile-amendment");
    await waitFor(() => expect(tile).toHaveTextContent("0.31%"));
    expect(tile).toHaveTextContent("↓ 0.11%");
    expect(tile).toHaveTextContent("8 amended of 2580 released");
    expect(tile).toHaveTextContent("vs prior 30 days");
    expect(tile).not.toHaveTextContent("Coming soon");
  });

  test("NCE Pulse tile shows the critical-pending count and corrective-action line", async () => {
    mockApis();
    renderPage();

    const tile = screen.getByTestId("qi-tile-nce-pulse");
    await waitFor(() => expect(tile).toHaveTextContent("3"));
    expect(tile).toHaveTextContent("critical pending");
    expect(tile).toHaveTextContent("2 in corrective action");
    expect(tile).not.toHaveTextContent("Coming soon");
    // amber band (1-4 pending) drives the tile accent
    expect(tile.className).toContain("qi-tile--amber");
  });

  test("rejection tile remains a ticket-annotated placeholder", () => {
    mockApis();
    renderPage();

    expect(screen.getByTestId("qi-tile-rejection")).toHaveTextContent(
      "Coming soon — OGC-697",
    );
  });

  test("shows empty states when the window has no activity", async () => {
    mockApis({
      tatCurrent: { totalCount: 0, mean: null, breakdown: [] },
      amendCurrent: { amendedCount: 0, releasedCount: 0, ratePercent: null },
    });
    renderPage();

    await waitFor(() =>
      expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent(
        "No completed test orders in this window.",
      ),
    );
    expect(screen.getByTestId("qi-tile-amendment")).toHaveTextContent(
      "No results released in this window.",
    );
  });

  test("shows error states when the APIs are unavailable", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(undefined),
    );
    renderPage();

    await waitFor(() =>
      expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent(
        "Turnaround data is unavailable.",
      ),
    );
    expect(screen.getByTestId("qi-tile-amendment")).toHaveTextContent(
      "Amendment data is unavailable.",
    );
    expect(screen.getByTestId("qi-tile-nce-pulse")).toHaveTextContent(
      "NCE data is unavailable.",
    );
  });

  test("uses the persisted reporting window for both summary queries", () => {
    localStorage.setItem("qa.qi.dashboard.window", "7d");
    mockApis();
    renderPage();

    const from = new Date();
    from.setDate(from.getDate() - 7);
    const expectedFrom = from.toISOString().split("T")[0];
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining(`fromDate=${expectedFrom}`),
      expect.any(Function),
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("segment=RECEIPT_TO_VALIDATION"),
      expect.any(Function),
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining(
        `/rest/reports/amendment/summary?fromDate=${expectedFrom}`,
      ),
      expect.any(Function),
    );
  });

  test("refresh refetches every indicator and rate-limits the button", async () => {
    mockApis();
    renderPage();
    // TAT + Amendment + Callback fire current+prior (2 each); NCE Pulse once;
    // plus the OGC-711 enabled-config resolve for TAT/AMENDMENT/NCE/CALLBACK
    // (4) = 11
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(11);

    const refresh = screen.getByTestId("qi-dashboard-refresh");
    fireEvent.click(refresh);
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(22);
    expect(refresh).toBeDisabled();
  });
});
