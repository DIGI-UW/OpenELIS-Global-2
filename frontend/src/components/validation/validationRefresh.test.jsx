/**
 * The validation queue re-runs its own search after a write instead of asking
 * the browser to download the app again. The three actions that used to
 * send the browser to the route it is already on are refetches: a per-row
 * action, a stale-row rejection, and the bulk release of the Clear lane.
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
    postToOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    getFromOpenElisServer: vi.fn(),
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
  SignatureMeaning: { VALIDATED_AND_RELEASED: "VALIDATED_AND_RELEASED" },
}));

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const row = (id, overrides = {}) => ({
  analysisId: String(100 + id),
  accessionNumber: `ACC${id}`,
  testName: `Test ${id}(Serum)`,
  normalRange: "10 - 20",
  normal: true,
  qcStatus: "PASS",
  result: "15",
  resultType: "N",
  nceOpen: false,
  modified: false,
  ackPending: false,
  nonconforming: false,
  critical: false,
  ...overrides,
});

const CONFIG = { AccessionFormat: "", ALLOW_BULK_RELEASE_CLEAR: "true" };

/**
 * What the server would answer the queue's search right now. Tests move it
 * between the assertions so a stale render is distinguishable from a refetch.
 */
let queue;
let assign;

const renderQueue = (rows = [row(0)], paging) => {
  queue = { resultList: rows, qcFailureList: [], paging };
  render(
    <MemoryRouter>
      <ConfigurationContext.Provider
        value={{ configurationProperties: CONFIG }}
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
};

describe("Validation queue refresh", () => {
  beforeEach(() => {
    assign = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: {
        ...window.location,
        pathname: "/validation",
        search: "?type=order&accessionNumber=ACC0",
        assign,
      },
    });
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/AccessionValidation?")) {
        callback(queue);
      }
    });
    postToOpenElisServerJsonResponse.mockReset();
  });

  it("runs the search named in the address bar when the page opens", () => {
    renderQueue();

    expect(screen.getByText(/ACC0/)).toBeInTheDocument();
    expect(
      getFromOpenElisServer.mock.calls.filter(([url]) =>
        url.startsWith("/rest/AccessionValidation?"),
      ),
    ).toHaveLength(1);
  });

  it("serves the queue the server holds after a per-row action, without a page load", () => {
    renderQueue([row(0, { normal: false })]);
    fireEvent.click(screen.getByTestId("review-row-0"));

    // Only the server knows about ACC7, so seeing it proves a real refetch.
    queue = { resultList: [row(7)], qcFailureList: [] };
    fireEvent.click(screen.getByTestId("review-retest"));
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ outcome: "retest" }),
    );
    fireEvent.click(screen.getByTestId("review-confirm-retest"));

    expect(assign).not.toHaveBeenCalled();
    expect(screen.getByText(/ACC7/)).toBeInTheDocument();
    expect(screen.queryByText(/ACC0/)).toBeNull();
  });

  it.each([1, 0])(
    "removes stale pagination after release leaves %i pages",
    async (totalPages) => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.startsWith("/rest/AccessionValidation?")) {
          const response = queue;
          queueMicrotask(() => callback(response));
        }
      });
      renderQueue([row(0)], { totalPages: 2, currentPage: 1 });
      await screen.findByText(/ACC0/);
      expect(await screen.findByRole("button", { name: "next" })).toBeEnabled();
      queue = {
        resultList: totalPages ? [row(7)] : [],
        qcFailureList: [],
        paging: { totalPages, currentPage: 1 },
      };
      fireEvent.click(screen.getByTestId("release-all-clear"));
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, body, callback) => callback({ released: ["100"], skipped: [] }),
      );
      fireEvent.click(
        screen.getByTestId("release-all-clear-sign").querySelector("button"),
      );
      await waitFor(() =>
        expect(screen.queryByText(/ACC0/)).not.toBeInTheDocument(),
      );
      expect(
        screen.queryByRole("button", { name: "next" }),
      ).not.toBeInTheDocument();
    },
  );

  it("serves the queue the server holds after the bulk release, without a page load", () => {
    renderQueue();

    queue = { resultList: [row(7)], qcFailureList: [] };
    fireEvent.click(screen.getByTestId("release-all-clear"));
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ released: ["100"], skipped: [] }),
    );
    fireEvent.click(
      screen.getByTestId("release-all-clear-sign").querySelector("button"),
    );

    expect(assign).not.toHaveBeenCalled();
    expect(screen.getByText(/ACC7/)).toBeInTheDocument();
    expect(screen.queryByText(/ACC0/)).toBeNull();
  });
});
