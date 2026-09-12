import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
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
import TestOrderability from "../TestOrderability";

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

// One sample type with two tests, so turning one off leaves the group on screen.
const orderability = () => ({
  orderableTestList: [
    {
      sampleType: { id: "1", value: "Blood" },
      activeTests: [
        { id: "10", value: "Glucose" },
        { id: "11", value: "Urea" },
      ],
      inactiveTests: [],
    },
  ],
});

const glucose = () => document.getElementById("1-10");
const cancelButton = () => screen.getAllByRole("button", { name: "Cancel" })[0];

describe("TestOrderability", () => {
  let queryClient;
  let reload;
  let assign;
  let onServer;

  const renderScreen = () =>
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={queryClient}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: vi.fn(),
              }}
            >
              <TestOrderability />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  const turnGlucoseOff = async () => {
    renderScreen();
    await waitFor(() => expect(glucose()).toBeInTheDocument());
    expect(glucose()).toBeChecked();
    // Carbon hides the input behind its label, so the click goes to the input.
    fireEvent.click(glucose());
    await waitFor(() => expect(glucose()).not.toBeChecked());
  };

  beforeEach(() => {
    queryClient = createQueryClient();
    onServer = orderability();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      url.startsWith("/rest/TestOrderability")
        ? callback(onServer)
        : callback(undefined),
    );
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    assign = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign },
    });
  });

  it("shows each orderable test as on", async () => {
    renderScreen();

    await waitFor(() => expect(glucose()).toBeChecked());
    expect(document.getElementById("1-11")).toBeChecked();
  });

  it("keeps the visible draft and submitted changes together after a refetch", async () => {
    await turnGlucoseOff();
    onServer = orderability();
    onServer.orderableTestList[0].activeTests[1].value = "Urea updated";
    await act(async () => {
      await queryClient.invalidateQueries({ queryKey: ["serverData"] });
    });

    expect(glucose()).not.toBeChecked();
    await userEvent.click(screen.getAllByRole("button", { name: "Submit" })[0]);
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));
    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    const sent = JSON.parse(JSON.parse(payload).jsonChangeList);
    expect(JSON.parse(sent.deactivateTest)).toEqual([{ id: "10" }]);
  });

  it("turns a test back on when the change is cancelled", async () => {
    await turnGlucoseOff();

    // Cancelling navigated to this same screen to forget the change; nothing on
    // the server has changed, so only dropping it can bring the state back.
    await userEvent.click(cancelButton());

    await waitFor(() => expect(glucose()).toBeChecked());
    expect(assign).not.toHaveBeenCalled();
    expect(reload).not.toHaveBeenCalled();
  });

  it("sends the test that was turned off and forgets it once saved", async () => {
    await turnGlucoseOff();

    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => callback(true),
    );
    await userEvent.click(screen.getAllByRole("button", { name: "Submit" })[0]);
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    const sent = JSON.parse(JSON.parse(payload).jsonChangeList);
    expect(JSON.parse(sent.deactivateTest)).toEqual([{ id: "10" }]);

    // With the change saved there is nothing left to submit.
    await waitFor(() => expect(glucose()).toBeChecked());
    expect(reload).not.toHaveBeenCalled();
  });
});
