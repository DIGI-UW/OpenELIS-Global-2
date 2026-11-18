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
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

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
const addNotification = vi.fn();

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
            addNotification,
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
    addNotification.mockReset();
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

  it("asks the server for the page Carbon moves to, and reopens it after accepting", async () => {
    const pages = {
      1: {
        resultList: [row(0)],
        type: "Demo Analyzer",
        paging: { totalPages: 2, currentPage: 1, searchTermToPage: [] },
      },
      2: {
        resultList: [row(1)],
        type: "Demo Analyzer",
        paging: { totalPages: 2, currentPage: 2, searchTermToPage: [] },
      },
    };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (!url.startsWith("/rest/AnalyzerResults")) return;
      const requested =
        Number(new URL(url, "http://x").searchParams.get("page")) || 1;
      callback(pages[requested]);
    });
    const pageUrls = () =>
      getFromOpenElisServer.mock.calls
        .map(([url]) => url)
        .filter((url) => url.includes("page="));
    renderScreen();
    await screen.findByText(/ACC0/);
    expect(pageUrls()).toEqual([]);

    fireEvent.click(screen.getByRole("button", { name: "Next Page" }));

    expect(await screen.findByText(/ACC1/)).toBeInTheDocument();
    expect(screen.queryByText(/ACC0/)).toBeNull();
    expect(pageUrls()).toEqual(["/rest/AnalyzerResults?id=22&page=2"]);

    postToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({ status: 200 }),
    );
    fireEvent.click(screen.getByTestId("Save-btn"));

    await waitFor(() => expect(pageUrls()).toHaveLength(2));
    expect(pageUrls()[1]).toBe("/rest/AnalyzerResults?id=22&page=2");
    expect(await screen.findByText(/ACC1/)).toBeInTheDocument();
    const posted = JSON.parse(
      postToOpenElisServerFullResponse.mock.calls[0][1],
    );
    expect(posted.paging.currentPage).toBe(2);
    expect(posted.resultList.map((r) => r.accessionNumber)).toEqual(["ACC1"]);
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

  /**
   * OGC-1662 — the reread after a save carries the server's defaults: no
   * notes, and matched groupings ticked again. The next save sent those while
   * the screen still showed the reviewer's notes and an unticked box.
   */
  it("sends what the screen shows on the save after a reread", async () => {
    const box = (id) => document.getElementById(`resultList${id}.isAccepted`);
    const note = (id) => document.getElementById(`resultList${id}.note`);
    worklist = {
      resultList: [
        row(2),
        { ...row(8), isAccepted: true },
        { ...row(14), isAccepted: true },
      ],
      type: "Demo Analyzer",
    };
    renderScreen();
    await screen.findByText(/ACC14/);

    fireEvent.change(note(2), {
      target: { value: "Instrument keyed another patient ID" },
    });
    fireEvent.click(box(14));
    fireEvent.change(note(14), { target: { value: "Hold for the ward" } });
    worklist = {
      resultList: [row(2), { ...row(14), isAccepted: true }],
      type: "Demo Analyzer",
    };
    postToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({ status: 200 }),
    );
    fireEvent.click(screen.getByTestId("Save-btn"));
    await waitFor(() => expect(screen.queryByText(/ACC8/)).toBeNull());

    expect(note(2)).toHaveValue("Instrument keyed another patient ID");
    expect(box(14)).not.toBeChecked();
    expect(note(14)).toHaveValue("Hold for the ward");
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );

    fireEvent.click(box(2));
    fireEvent.click(screen.getByTestId("Save-btn"));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2),
    );
    const sent = JSON.parse(postToOpenElisServerFullResponse.mock.calls[1][1]);
    const sentRow = (id) => sent.resultList.find((r) => r.id === String(id));
    expect(sentRow(2)).toMatchObject({
      isAccepted: true,
      note: "Instrument keyed another patient ID",
    });
    expect(sentRow(14)).toMatchObject({
      isAccepted: false,
      note: "Hold for the ward",
    });
  });

  it("keeps a test left out on every one of its rows after a reread", async () => {
    const part = {
      ...row(15),
      sampleGroupingNumber: 14,
      testId: "14",
      accessionNumber: "ACC14",
      componentId: "301",
      testName: "Log viral load",
    };
    worklist = {
      resultList: [
        { ...row(8), isAccepted: true },
        { ...row(14), isAccepted: true },
        { ...part, isAccepted: true },
      ],
      type: "Demo Analyzer",
    };
    renderScreen();
    await screen.findByText(/ACC14/);
    fireEvent.click(document.getElementById("resultList14.isAccepted"));
    worklist = {
      resultList: [
        { ...row(14), isAccepted: true },
        { ...part, isAccepted: true },
      ],
      type: "Demo Analyzer",
    };
    postToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({ status: 200 }),
    );
    fireEvent.click(screen.getByTestId("Save-btn"));
    await waitFor(() => expect(screen.queryByText(/ACC8/)).toBeNull());

    fireEvent.click(screen.getByTestId("Save-btn"));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledTimes(2),
    );
    const sent = JSON.parse(postToOpenElisServerFullResponse.mock.calls[1][1]);
    expect(sent.resultList.map((r) => [r.id, Boolean(r.isAccepted)])).toEqual([
      ["14", false],
      ["15", false],
    ]);
  });

  it("shows a reread row as the server sent it when the reviewer had not touched it", async () => {
    worklist = {
      resultList: [row(5), { ...row(8), isAccepted: true }],
      type: "Demo Analyzer",
    };
    renderScreen();
    await screen.findByText(/ACC8/);
    expect(document.getElementById("resultList5.isAccepted")).not.toBeChecked();
    worklist = {
      resultList: [{ ...row(5), isAccepted: true }],
      type: "Demo Analyzer",
    };
    postToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({ status: 200 }),
    );

    fireEvent.click(screen.getByTestId("Save-btn"));
    await waitFor(() => expect(screen.queryByText(/ACC8/)).toBeNull());

    expect(document.getElementById("resultList5.isAccepted")).toBeChecked();
  });

  it("names the selected results a save left in review instead of reporting success", async () => {
    worklist = { resultList: [row(2)], type: "Demo Analyzer" };
    renderScreen();
    await screen.findByText(/ACC2/);
    fireEvent.click(document.getElementById("resultList2.isAccepted"));
    postToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({
        status: 200,
        json: async () => ({
          leftInReview: [
            {
              id: "2",
              accessionNumber: "ACC2",
              testName: "Test 2",
              reason: "awaiting_placement",
            },
          ],
        }),
      }),
    );

    fireEvent.click(screen.getByTestId("Save-btn"));

    await waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "warning",
          message: expect.stringMatching(
            /ACC2 Test 2: choose its analysis or order, or explain the patient mismatch in the note/,
          ),
        }),
      ),
    );
    expect(addNotification).not.toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
  });
});
