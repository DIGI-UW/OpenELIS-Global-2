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
import PanelCreate from "../PanelCreate";
import SampleTypeCreate from "../SampleTypeCreate";
import TestSectionCreate from "../TestSectionCreate";

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

const type = async (id, value) =>
  userEvent.type(document.getElementById(id), value);

/**
 * The three create screens confirm in two steps and then post. Each used to
 * reload the document both to clear the form after a create and to abandon a
 * confirmation, and each shows what already exists from a read of the same
 * endpoint it posts to.
 */
const SCREENS = [
  {
    name: "PanelCreate",
    Screen: PanelCreate,
    endPoint: "/rest/PanelCreate",
    read: (names) => ({
      existingSampleTypeList: [{ id: "1", value: "Plasma" }],
      existingPanelList: [
        {
          typeOfSampleName: "Serology",
          panels: names.map((n) => ({ panelName: n })),
        },
      ],
      inactivePanelList: [],
    }),
    fill: async () => {
      await type("eng", "Serum");
      await type("fr", "Serum FR");
      await type("loincPost", "12345-6");
    },
  },
  {
    name: "SampleTypeCreate",
    Screen: SampleTypeCreate,
    endPoint: "/rest/SampleTypeCreate",
    read: (names) => ({
      existingSampleTypeList: names.map((value) => ({ value })),
      inactiveSampleTypeList: [],
    }),
    fill: async () => {
      await type("eng", "Serum");
      await type("fr", "Serum FR");
    },
  },
  {
    name: "TestSectionCreate",
    Screen: TestSectionCreate,
    endPoint: "/rest/TestSectionCreate",
    read: (names) => ({
      existingTestUnitList: names.map((value) => ({ value })),
      inactiveTestUnitList: [],
    }),
    fill: async () => {
      await type("eng", "Serum");
      await type("fr", "Serum FR");
      // Carbon hides the radio input, so the click goes to the element itself.
      fireEvent.click(document.getElementById("domain-clinical"));
    },
  },
];

describe.each(SCREENS)("$name", ({ Screen, endPoint, read, fill }) => {
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
              <Screen />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    onServer = read(["Blood"]);
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

  it("shows what already exists", async () => {
    renderScreen();

    expect(await screen.findByText("Blood")).toBeInTheDocument();
  });

  it("clears the form when the entry is abandoned", async () => {
    renderScreen();
    expect(await screen.findByText("Blood")).toBeInTheDocument();

    await type("eng", "Serum");
    expect(document.getElementById("eng")).toHaveValue("Serum");

    await userEvent.click(screen.getByRole("button", { name: "Previous" }));

    expect(document.getElementById("eng")).toHaveValue("");
    expect(reload).not.toHaveBeenCalled();
  });

  it("clears the form and reads what exists again after a create", async () => {
    renderScreen();
    expect(await screen.findByText("Blood")).toBeInTheDocument();

    await fill();
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => {
        onServer = read(["Blood", "Serum"]);
        callback(true);
      },
    );

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    await waitFor(() => expect(screen.getByText("Serum")).toBeInTheDocument());
    expect(document.getElementById("eng")).toHaveValue("");
    // Creating used to reload the document, which threw away the whole app.
    expect(reload).not.toHaveBeenCalled();
  });

  it("can be submitted again after a create fails", async () => {
    renderScreen();
    expect(await screen.findByText("Blood")).toBeInTheDocument();

    await fill();
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => callback(false),
    );

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    // The reload hid that nothing ever cleared Formik's submitting flag.
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Accept" })).toBeEnabled(),
    );
    expect(document.getElementById("eng")).toHaveValue("Serum");
  });
});
