import React from "react";
import { render, screen, within } from "@testing-library/react";
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
import PanelTestAssign from "../PanelTestAssign";

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

const panels = {
  panelList: [
    { id: "1", value: "Renal" },
    { id: "2", value: "Liver" },
  ],
};

const panel = (name, assigned, available) => ({
  selectedPanel: {
    panelIdValuePair: { id: "1", value: name },
    sampleTypeIdValuePair: { id: "5", value: "Blood" },
    tests: assigned.map(([id, value]) => ({ id, value })),
    availableTests: available.map(([id, value]) => ({ id, value })),
  },
});

// The two lists are the only lists on the screen, in order.
const assignedList = () => document.querySelectorAll("ul")[0];
const availableList = () => document.querySelectorAll("ul")[1];
const namesIn = (ul) =>
  Array.from(ul.querySelectorAll("li")).map((li) => li.textContent);

describe("PanelTestAssign", () => {
  let reload;
  let onServer;

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
              <PanelTestAssign />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  const pickRenal = async () => {
    renderScreen();
    await userEvent.selectOptions(await screen.findByRole("combobox"), "1");
    return waitFor(() => expect(namesIn(assignedList())).toEqual(["Glucose"]));
  };

  const moveUreaToAssigned = async () => {
    await userEvent.click(within(availableList()).getByText("Urea"));
    await userEvent.click(screen.getByRole("button", { name: "<" }));
  };

  beforeEach(() => {
    onServer = {
      panels,
      1: panel("Renal", [["10", "Glucose"]], [["11", "Urea"]]),
      2: panel("Liver", [["20", "ALT"]], [["21", "AST"]]),
    };
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      const match = /panelId=(\d+)/.exec(url);
      if (match) return callback(onServer[match[1]]);
      if (url.startsWith("/rest/PanelTestAssign"))
        return callback(onServer.panels);
      return callback(undefined);
    });
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  it("shows a picked panel's tests and the ones still available", async () => {
    await pickRenal();

    expect(namesIn(assignedList())).toEqual(["Glucose"]);
    expect(namesIn(availableList())).toEqual(["Urea"]);
  });

  it("offers nothing from the panel picked before while the next one is read", async () => {
    await pickRenal();

    // Hold the second panel's read open so the wait is observable.
    let answerLiver;
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (/panelId=2/.test(url)) {
        answerLiver = () => callback(onServer["2"]);
        return;
      }
      const match = /panelId=(\d+)/.exec(url);
      if (match) return callback(onServer[match[1]]);
      if (url.startsWith("/rest/PanelTestAssign"))
        return callback(onServer.panels);
      return callback(undefined);
    });

    await userEvent.selectOptions(screen.getByRole("combobox"), "2");

    // Renal's tests are not on offer for Liver, not even for a moment.
    await waitFor(() =>
      expect(document.querySelectorAll("ul")).toHaveLength(0),
    );

    answerLiver();
    await waitFor(() => expect(namesIn(assignedList())).toEqual(["ALT"]));
    expect(namesIn(availableList())).toEqual(["AST"]);
  });

  it("sends the tests as they were moved", async () => {
    await pickRenal();
    await moveUreaToAssigned();

    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(JSON.parse(payload)).toMatchObject({
      panelId: "1",
      currentTests: ["10", "11"],
    });
  });

  it("shows the stored panel again once the move is saved, without reloading", async () => {
    await pickRenal();
    await moveUreaToAssigned();
    expect(namesIn(assignedList())).toEqual(["Glucose", "Urea"]);

    // The move is rejected upstream, so what is stored has not changed.
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => callback(true),
    );
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(namesIn(assignedList())).toEqual(["Glucose"]));
    expect(namesIn(availableList())).toEqual(["Urea"]);
    // Saving used to reload the document, which threw away the whole app.
    expect(reload).not.toHaveBeenCalled();
  });

  it("sends nothing when no panel was picked", async () => {
    renderScreen();
    await screen.findByRole("combobox");

    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    expect(reload).not.toHaveBeenCalled();
  });
});
