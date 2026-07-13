import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import AmendmentReport from "../AmendmentReport";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
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

    expect(
      screen.getByText("Amendment data is unavailable."),
    ).toBeInTheDocument();
  });
});
