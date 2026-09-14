/**
 * The logbook results table re-runs its own search after a save instead of
 * sending the browser back to the URL it is already on. Confirms the fix for
 * the last same-route window.location.href in the file.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import ResultSearchPage from "./SearchResultForm";
import { ConfigurationContext, NotificationContext } from "../layout/contexts";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

vi.mock("../esignature/ESignatureButton", () => ({
  default: ({ children, onSign, disabled }) => (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onSign && onSign()}
    >
      {children}
    </button>
  ),
  SignatureMeaning: { AUTHORED: "AUTHORED" },
}));

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const row = (id) => ({
  id: String(id),
  analysisId: String(100 + id),
  accessionNumber: `ACC${id}`,
  testName: `Test ${id}(Serum)`,
  normalRange: "10 - 20",
  result: "15",
  resultType: "N",
  reportable: "Y",
});

/** What the server would answer the logbook search right now. */
let queue;
let hrefWrittenTo;

const renderScreen = () =>
  render(
    <MemoryRouter initialEntries={["/LogbookResults"]}>
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
            <ResultSearchPage />
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </MemoryRouter>,
  );

describe("Logbook results refresh", () => {
  beforeEach(() => {
    queue = { testResult: [row(0)] };
    hrefWrittenTo = null;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: {
        ...window.location,
        pathname: "/LogbookResults",
        search: "",
        get href() {
          return "http://localhost/LogbookResults";
        },
        set href(value) {
          hrefWrittenTo = value;
        },
      },
    });
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/LogbookResults")) return callback(queue);
      return callback([]);
    });
    postToOpenElisServerJsonResponse.mockReset();
  });

  it("runs the logbook search on mount", async () => {
    renderScreen();

    expect(await screen.findByText(/ACC0/)).toBeInTheDocument();
  });

  it("serves the queue the server holds after saving, without a page load", async () => {
    renderScreen();
    await screen.findByText(/ACC0/);

    // Only the server knows about ACC7, so seeing it proves a real refetch.
    queue = { testResult: [row(7)] };
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ status: "success" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(await screen.findByText(/ACC7/)).toBeInTheDocument();
    expect(screen.queryByText(/ACC0/)).toBeNull();
    expect(hrefWrittenTo).toBeNull();
  });

  it.each([1, 0])(
    "removes stale pagination when the refreshed queue has %i pages",
    async (totalPages) => {
      queue = {
        testResult: [row(0)],
        paging: { totalPages: 2, currentPage: 1 },
      };
      getFromOpenElisServer.mockImplementation((url, callback) => {
        const response = url.startsWith("/rest/LogbookResults") ? queue : [];
        queueMicrotask(() => callback(response));
      });
      renderScreen();
      await screen.findByText(/ACC0/);
      expect(await screen.findByRole("button", { name: "next" })).toBeEnabled();
      queue = {
        testResult: totalPages ? [row(7)] : [],
        paging: { totalPages, currentPage: 1 },
      };
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, body, callback) => callback({ status: "success" }),
      );
      fireEvent.click(screen.getByRole("button", { name: "Save" }));
      await waitFor(() =>
        expect(screen.queryByText(/ACC0/)).not.toBeInTheDocument(),
      );
      expect(
        screen.queryByRole("button", { name: "next" }),
      ).not.toBeInTheDocument();
    },
  );

  it("starts a fresh row-editing session when a saved queue replaces the old one", async () => {
    queue = { testResult: [row(0)] };
    renderScreen();
    await screen.findByText(/ACC0/);

    fireEvent.change(document.getElementById("testResult0.note"), {
      target: { value: "Only for ACC0" },
    });
    expect(document.getElementById("testResult0.note")).toHaveValue(
      "Only for ACC0",
    );

    queue = { testResult: [row(7)] };
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ status: "success" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(await screen.findByText(/ACC7/)).toBeInTheDocument();
    expect(document.getElementById("testResult0.note")).toHaveValue("");
  });
});
