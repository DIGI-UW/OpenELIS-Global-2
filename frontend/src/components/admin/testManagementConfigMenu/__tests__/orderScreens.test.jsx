import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import { NotificationContext } from "../../../layout/contexts";
import PanelOrder from "../PanelOrder";
import SampleTypeOrder from "../SampleTypeOrder";
import TestSectionOrder from "../TestSectionOrder";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  const getFromOpenElisServer = vi.fn();
  return {
    ...actual,
    getFromOpenElisServer,
    fetchFromOpenElisServer: vi.fn(
      (url) =>
        new Promise((resolve, reject) =>
          getFromOpenElisServer(url, (response) =>
            response === undefined
              ? reject(new Error("read failed"))
              : resolve(response),
          ),
        ),
    ),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

/**
 * The three display-order screens are the same screen with different nouns:
 * read a stored order, drag entries into a new one, confirm, save. Each used to
 * reload the document both to show a saved order and to throw a pending one
 * away.
 */
const SCREENS = [
  {
    name: "PanelOrder",
    Screen: PanelOrder,
    endPoint: "/rest/PanelOrder",
    listField: "panelList",
  },
  {
    name: "SampleTypeOrder",
    Screen: SampleTypeOrder,
    endPoint: "/rest/SampleTypeOrder",
    listField: "sampleTypeList",
  },
  {
    name: "TestSectionOrder",
    Screen: TestSectionOrder,
    endPoint: "/rest/TestSectionOrder",
    listField: "testSectionList",
  },
];

const shownOrder = () =>
  screen
    .getAllByLabelText("sample-type-list-draggable")
    .map((icon) => icon.closest("[draggable]").textContent.trim());

// A `dataTransfer` passed through fireEvent.drop's init does not reach React's
// handler, so the event carries it as an own property instead.
const dragFirstOntoSecond = () => {
  const rows = screen
    .getAllByLabelText("sample-type-list-draggable")
    .map((icon) => icon.closest("[draggable]"));
  const drop = new Event("drop", { bubbles: true, cancelable: true });
  Object.defineProperty(drop, "dataTransfer", {
    value: { getData: () => "0" },
  });
  fireEvent(rows[1], drop);
};

describe.each(SCREENS)("$name", ({ Screen, endPoint, listField }) => {
  let reload;
  let onServer;

  const orderOf = (names) => ({
    [listField]: names.map((name, index) => ({
      id: String(index + 1),
      value: name,
      sortOrder: index,
    })),
  });

  const renderScreen = () =>
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: vi.fn(),
              }}
            >
              <Screen />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    onServer = orderOf(["Chemistry", "Haematology"]);
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      url.startsWith(endPoint) ? callback(onServer) : callback(undefined),
    );
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  it("shows the stored order", async () => {
    renderScreen();

    expect(await screen.findByText("Chemistry")).toBeInTheDocument();
    expect(shownOrder()).toEqual(["Chemistry", "Haematology"]);
  });

  it("throws a pending reorder away when it is discarded", async () => {
    renderScreen();
    expect(await screen.findByText("Chemistry")).toBeInTheDocument();

    dragFirstOntoSecond();
    expect(shownOrder()).toEqual(["Haematology", "Chemistry"]);

    // Discarding is the case where nothing on the server changed, so nothing
    // but clearing the pending order can bring the stored one back.
    await userEvent.click(screen.getByRole("button", { name: "Previous" }));

    await waitFor(() =>
      expect(shownOrder()).toEqual(["Chemistry", "Haematology"]),
    );
    expect(reload).not.toHaveBeenCalled();
  });

  it("sends the reordered entries when the new order is accepted", async () => {
    renderScreen();
    expect(await screen.findByText("Chemistry")).toBeInTheDocument();

    dragFirstOntoSecond();
    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    const sent = JSON.parse(JSON.parse(payload).jsonChangeList);
    const entries = JSON.parse(Object.values(sent)[0]);
    expect(entries).toEqual([
      { id: 2, sortOrder: 0 },
      { id: 1, sortOrder: 1 },
    ]);
  });

  it("reads the order again after a save, without reloading", async () => {
    renderScreen();
    expect(await screen.findByText("Chemistry")).toBeInTheDocument();

    dragFirstOntoSecond();
    // Someone else's change lands while this screen is open.
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => {
        onServer = orderOf(["Serology"]);
        callback(true);
      },
    );

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    await waitFor(() => expect(shownOrder()).toEqual(["Serology"]));
    expect(reload).not.toHaveBeenCalled();
  });

  it("does not post an empty change when nothing was reordered", async () => {
    renderScreen();
    expect(await screen.findByText("Chemistry")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    expect(
      await screen.findByRole("button", { name: "Next" }),
    ).toBeInTheDocument();
  });

  it("leaves confirm mode once the accepted order is saved", async () => {
    renderScreen();
    expect(await screen.findByText("Chemistry")).toBeInTheDocument();

    dragFirstOntoSecond();
    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    onServer = orderOf(["Haematology", "Chemistry"]);
    // The real endpoint answers after the click handler has already returned,
    // so the callback has to be genuinely async here too — a synchronous one
    // would race the same button's own unconditional setConfirmSelection(true).
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => Promise.resolve().then(() => callback(true)),
    );
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    // Confirm mode cleared: the button is "Next" again, not "Accept" still
    // waiting on a second click to repost the same, now-stale, entries.
    expect(
      await screen.findByRole("button", { name: "Next" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Accept" }),
    ).not.toBeInTheDocument();

    // A fresh reorder posts only the entries just dragged, not anything left
    // over from the accepted save.
    postToOpenElisServerJsonResponse.mockClear();
    dragFirstOntoSecond();
    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));
    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    const sent = JSON.parse(JSON.parse(payload).jsonChangeList);
    const entries = JSON.parse(Object.values(sent)[0]);
    expect(entries).toEqual([
      { id: 2, sortOrder: 0 },
      { id: 1, sortOrder: 1 },
    ]);
  });
});
