/**
 * TestCatalogList — OGC-949 M3 / OGC-928 (RM10/RM11 polish).
 *
 * Validates the list screen's resilience + filter behavior:
 * - rows render from a successful fetch;
 * - a FAILED fetch shows an error state (not a silent empty list — the bug RM10 fixes);
 * - an empty result shows an empty state;
 * - filter state restores from the URL and is sent to the server (RM11 URL-sync + AMR).
 */

// ========== MOCKS (before imports) ==========
const mockHistory = {
  push: vi.fn(),
  replace: vi.fn(),
  location: { search: "" },
};

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useHistory: () => mockHistory };
});

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

// ========== IMPORTS ==========
import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import TestCatalogList from "./TestCatalogList";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

// ========== HELPERS ==========
const renderList = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <TestCatalogList />
      </IntlProvider>
    </BrowserRouter>,
  );

const pageOf = (rows) => ({ rows, total: rows.length });

beforeEach(() => {
  vi.clearAllMocks();
  mockHistory.location = { search: "" };
});

describe("TestCatalogList", () => {
  it("renders the rows returned by the server", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(
        pageOf([
          {
            testId: "7",
            name: "Glucose",
            code: "GLU",
            domain: "CLINICAL",
            active: true,
          },
        ]),
      ),
    );
    renderList();
    expect(await screen.findByText("Glucose")).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails (not a silent empty list)", async () => {
    // getFromOpenElisServer calls back with undefined on a failed fetch.
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderList();
    expect(
      await screen.findByText(messages["label.testCatalog.list.error"]),
    ).toBeInTheDocument();
  });

  it("shows an empty state when no tests match", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(pageOf([])));
    renderList();
    expect(
      await screen.findByText(messages["label.testCatalog.list.empty"]),
    ).toBeInTheDocument();
  });

  it("restores filter state from the URL and sends it to the server", async () => {
    mockHistory.location = { search: "?amr=true&domain=CLINICAL&sampleType=2" };
    let requestedUrl = "";
    getFromOpenElisServer.mockImplementation((url, cb) => {
      // Only the tests endpoint carries filter state; ignore the sample-types
      // reference fetch.
      if (url.includes("/tests")) requestedUrl = url;
      cb(pageOf([]));
    });
    renderList();
    await waitFor(() => expect(requestedUrl).toContain("amr=true"));
    expect(requestedUrl).toContain("domain=CLINICAL");
    expect(requestedUrl).toContain("sampleType=2");
  });

  it("aborts the previous request when filters change (stale-result guard)", () => {
    vi.useFakeTimers();
    try {
      const signals = [];
      getFromOpenElisServer.mockImplementation((url, cb, sig) => {
        // Track only the tests endpoint; the sample-types reference fetch is
        // a separate, signal-less call.
        if (url.includes("/tests")) signals.push(sig);
        cb(pageOf([]));
      });
      renderList();
      expect(signals.length).toBe(1); // initial fetch on mount
      const search = screen.getByPlaceholderText(
        messages["label.testCatalog.list.search"],
      );
      fireEvent.change(search, { target: { value: "x" } });
      act(() => vi.advanceTimersByTime(300)); // debounce fires -> a new fetch starts
      expect(signals.length).toBe(2);
      // The earlier request is aborted so its late response can't overwrite the newer one.
      expect(signals[0].aborted).toBe(true);
      expect(signals[1].aborted).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });

  it("opens the editor for the clicked row", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(
        pageOf([
          { testId: "7", name: "Glucose", domain: "CLINICAL", active: true },
        ]),
      ),
    );
    renderList();
    const cell = await screen.findByText("Glucose");
    fireEvent.click(cell.closest("td"));
    expect(mockHistory.push).toHaveBeenCalledWith(
      "/MasterListsPage/TestCatalogEditor/7/basic-info",
    );
  });

  /**
   * The checkbox selects a row for the "Edit related" batch action, which needs
   * two or more tests. Opening the editor on the same click made it unusable:
   * the box did toggle, then the navigation took the list away before a second
   * row could be picked.
   *
   * It cannot be fixed by stopping propagation on TableSelectRow. Carbon
   * destructures a fixed set of props there and onClick is not among them, so a
   * handler passed to it never reaches the DOM. The row-opening click therefore
   * lives on the data cells, leaving the checkbox cell outside it.
   */
  it("selects a row without opening the editor when the checkbox is clicked", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(
        pageOf([
          { testId: "7", name: "Glucose", domain: "CLINICAL", active: true },
          { testId: "8", name: "Sodium", domain: "CLINICAL", active: true },
        ]),
      ),
    );
    renderList();
    await screen.findByText("Glucose");

    const boxes = document
      .querySelector("tbody")
      .querySelectorAll('input[type="checkbox"]');
    fireEvent.click(boxes[0]);

    expect(
      mockHistory.push,
      "selecting a row must not navigate away from the list",
    ).not.toHaveBeenCalled();
    expect(boxes[0].checked).toBe(true);
  });

  it("still opens the editor from a data cell once a row is selected", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(
        pageOf([
          { testId: "7", name: "Glucose", domain: "CLINICAL", active: true },
        ]),
      ),
    );
    renderList();
    const cell = await screen.findByText("Glucose");

    fireEvent.click(document.querySelector('tbody input[type="checkbox"]'));
    fireEvent.click(cell.closest("td"));

    expect(mockHistory.push).toHaveBeenCalledWith(
      "/MasterListsPage/TestCatalogEditor/7/basic-info",
    );
  });

  it("debounces the search — one fetch after the pause, not per keystroke", () => {
    vi.useFakeTimers();
    try {
      getFromOpenElisServer.mockImplementation((url, cb) => cb(pageOf([])));
      renderList();
      const before = getFromOpenElisServer.mock.calls.length;
      const search = screen.getByPlaceholderText(
        messages["label.testCatalog.list.search"],
      );
      fireEvent.change(search, { target: { value: "glu" } });
      act(() => vi.advanceTimersByTime(200));
      expect(getFromOpenElisServer.mock.calls.length).toBe(before); // still debouncing
      act(() => vi.advanceTimersByTime(150));
      expect(getFromOpenElisServer.mock.calls.length).toBe(before + 1); // fired after 300ms
      expect(getFromOpenElisServer.mock.calls.at(-1)[0]).toContain(
        "search=glu",
      );
    } finally {
      vi.useRealTimers();
    }
  });

  /**
   * Typing must not be interrupted: a search refresh leaves the toolbar Search
   * mounted (focus/cursor kept) and keeps the previous rows on screen instead of
   * swapping the whole table — search included — for a full-table spinner.
   * Reverting the fix unmounts both, so this fails.
   */
  it("keeps the search box and current rows mounted during a search refresh", () => {
    vi.useFakeTimers();
    try {
      let refreshPending = false;
      getFromOpenElisServer.mockImplementation((url, cb) => {
        if (!url.includes("/tests")) return cb([]); // sample-types reference fetch
        if (refreshPending) return; // hold the refresh in-flight -> loading stays true
        cb(
          pageOf([
            { testId: "7", name: "Glucose", domain: "CLINICAL", active: true },
          ]),
        );
      });
      renderList();
      expect(screen.getByText("Glucose")).toBeInTheDocument();

      refreshPending = true;
      const search = screen.getByPlaceholderText(
        messages["label.testCatalog.list.search"],
      );
      fireEvent.change(search, { target: { value: "glu" } });
      act(() => vi.advanceTimersByTime(300)); // debounce fires -> refresh now loading

      expect(
        screen.getByPlaceholderText(messages["label.testCatalog.list.search"]),
      ).toBeInTheDocument();
      expect(screen.getByText("Glucose")).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it("shows the sample type in its own column", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/tests")) {
        cb(
          pageOf([
            {
              testId: "7",
              name: "Glucose (Serum)",
              sampleType: "Serum",
              code: "GLU",
              domain: "CLINICAL",
              active: true,
            },
          ]),
        );
      } else {
        cb([]);
      }
    });
    renderList();
    expect(await screen.findByText("Serum")).toBeInTheDocument();
  });

  it("New test button opens create-in-place", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(url.includes("/tests") ? pageOf([]) : []),
    );
    renderList();
    const button = await screen.findByTestId("new-test-button");
    fireEvent.click(button);
    expect(mockHistory.push).toHaveBeenCalledWith(
      "/MasterListsPage/TestCatalogEditor/new/basic-info",
    );
  });
});
