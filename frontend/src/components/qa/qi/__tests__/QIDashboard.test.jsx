import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import QIDashboard from "../QIDashboard";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

import { getFromOpenElisServer } from "../../../utils/Utils";

// Rendering with the real en.json fails loudly if a referenced i18n key
// is missing (react-intl falls back to the raw key, breaking assertions).
const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <QIDashboard />
      </MemoryRouter>
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

// The dashboard fires the current-window call before the prior-window call.
const mockSummaries = (current, prior) => {
  let call = 0;
  getFromOpenElisServer.mockImplementation((url, callback) => {
    callback(call++ === 0 ? current : prior);
  });
};

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
});

describe("QIDashboard", () => {
  test("renders four tiles in fixed order with a live TAT tile", async () => {
    mockSummaries(currentSummary, priorSummary);
    renderPage();

    const tiles = ["tat", "rejection", "amendment", "nce-pulse"].map((id) =>
      screen.getByTestId(`qi-tile-${id}`),
    );
    expect(tiles[0]).toHaveTextContent("Average TAT");
    expect(tiles[1]).toHaveTextContent("Rejection Rate");
    expect(tiles[2]).toHaveTextContent("Amendment Rate");
    expect(tiles[3]).toHaveTextContent("NCE Pulse");

    await waitFor(() =>
      expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent("18h 47m"),
    );
    expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent("↓ 32m");
    expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent(
      "Across 2 lab units · Slowest: Microbiology (61h)",
    );
    expect(screen.getByRole("link", { name: /View detail/ })).toHaveAttribute(
      "href",
      "/qa/qi/tat",
    );
  });

  test("coming-soon tiles are annotated with their tickets", () => {
    mockSummaries(currentSummary, priorSummary);
    renderPage();

    expect(screen.getByTestId("qi-tile-rejection")).toHaveTextContent(
      "Coming soon — OGC-697",
    );
    expect(screen.getByTestId("qi-tile-amendment")).toHaveTextContent(
      "Coming soon — OGC-698",
    );
    expect(screen.getByTestId("qi-tile-nce-pulse")).toHaveTextContent(
      "Coming soon — OGC-699",
    );
  });

  test("shows empty state when the window has no completed test orders", async () => {
    mockSummaries({ totalCount: 0, mean: null, breakdown: [] }, priorSummary);
    renderPage();

    await waitFor(() =>
      expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent(
        "No completed test orders in this window.",
      ),
    );
  });

  test("shows error state when the TAT API is unavailable", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback(undefined),
    );
    renderPage();

    await waitFor(() =>
      expect(screen.getByTestId("qi-tile-tat")).toHaveTextContent(
        "Turnaround data is unavailable.",
      ),
    );
  });

  test("uses the persisted reporting window for the summary query", () => {
    localStorage.setItem("qa.qi.dashboard.window", "7d");
    mockSummaries(currentSummary, priorSummary);
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
  });

  test("refresh refetches and rate-limits the button", async () => {
    mockSummaries(currentSummary, priorSummary);
    renderPage();
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(2);

    const refresh = screen.getByTestId("qi-dashboard-refresh");
    fireEvent.click(refresh);
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(4);
    expect(refresh).toBeDisabled();
  });
});
