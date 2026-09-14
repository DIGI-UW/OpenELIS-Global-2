/**
 * Accepting analyzer results rereads the worklist in place. The success path
 * used to send the browser back to the URL the page was already on, which
 * downloaded the application again to serve the same search.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import Index from "./Index";
import { ConfigurationContext, NotificationContext } from "../layout/contexts";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../utils/Utils";

const row = (id) => ({
  id: String(id),
  analysisId: String(100 + id),
  accessionNumber: `ACC${id}`,
  sampleGroupingNumber: id,
  testName: `Test ${id}`,
  testId: String(id),
  result: "15",
  resultType: "N",
  completeDate: "01/09/2026",
  isControl: false,
  nonconforming: false,
});

/** What the server would answer the worklist's read right now. */
let worklist;
let hrefWrittenTo;

const renderScreen = () =>
  render(
    <MemoryRouter initialEntries={["/AnalyzerResults?id=22"]}>
      <ConfigurationContext.Provider
        value={{ configurationProperties: { AccessionFormat: "" } }}
      >
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <Index />
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </MemoryRouter>,
  );

describe("Analyzer results worklist", () => {
  beforeEach(() => {
    worklist = { resultList: [row(0)], type: "Demo Analyzer" };
    hrefWrittenTo = null;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: {
        ...window.location,
        pathname: "/AnalyzerResults",
        search: "?id=22",
        get href() {
          return "http://localhost/AnalyzerResults?id=22";
        },
        set href(value) {
          hrefWrittenTo = value;
        },
      },
    });
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/AnalyzerResults")) callback(worklist);
    });
    postToOpenElisServerFullResponse.mockReset();
  });

  it("reads the worklist named in the address bar when the page opens", async () => {
    renderScreen();

    expect(await screen.findByText(/ACC0/)).toBeInTheDocument();
  });

  it("serves the worklist the server holds after accepting, without a page load", async () => {
    renderScreen();
    await screen.findByText(/ACC0/);

    // Only the server knows about ACC7, so seeing it proves a real reread.
    worklist = { resultList: [row(7)], type: "Demo Analyzer" };
    postToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({ status: 200 }),
    );
    fireEvent.click(screen.getByTestId("Save-btn"));

    expect(await screen.findByText(/ACC7/)).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText(/ACC0/)).toBeNull());
    expect(hrefWrittenTo).toBeNull();
  });

  it.each([1, 0])(
    "removes stale server-pagination controls after a refresh leaves %i pages",
    async (totalPages) => {
      worklist = {
        resultList: [row(0)],
        type: "Demo Analyzer",
        paging: { totalPages: 2, currentPage: 1, searchTermToPage: [] },
      };
      renderScreen();
      await screen.findByText(/ACC0/);
      expect(screen.getByRole("button", { name: "next" })).toBeInTheDocument();

      worklist = {
        resultList: totalPages ? [row(7)] : [],
        type: "Demo Analyzer",
        paging: { totalPages, currentPage: 1, searchTermToPage: [] },
      };
      postToOpenElisServerFullResponse.mockImplementation(
        (url, body, callback) => callback({ status: 200 }),
      );
      fireEvent.click(screen.getByTestId("Save-btn"));

      await waitFor(() =>
        expect(screen.queryByText(/ACC0/)).not.toBeInTheDocument(),
      );
      expect(screen.queryByRole("button", { name: "next" })).toBeNull();
    },
  );
});
