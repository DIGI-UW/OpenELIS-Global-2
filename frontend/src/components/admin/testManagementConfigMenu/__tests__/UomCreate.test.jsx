import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import { NotificationContext } from "../../../layout/contexts";
import UomCreate from "../UomCreate";

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

const units = (values) => ({
  existingUomList: values.map((value) => ({ value })),
  inactiveUomList: [],
  existingEnglishNames: values.join("$"),
});

const field = () => document.getElementById("uomNew");

describe("UomCreate", () => {
  let reload;
  let onServer;
  let addNotification;

  const renderScreen = () =>
    render(
      <MemoryRouter initialEntries={["/MasterListsPage/UomCreate"]}>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification,
              }}
            >
              <Route path="/MasterListsPage/UomCreate">
                <UomCreate />
              </Route>
              <Route path="/MasterListsPage/UomManagement">
                <div>unit list</div>
              </Route>
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    onServer = units(["mg"]);
    addNotification = vi.fn();
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      url.startsWith("/rest/UomCreate")
        ? callback(onServer)
        : callback(undefined),
    );
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn(), replace: vi.fn() },
    });
  });

  it("shows the units that already exist", async () => {
    renderScreen();

    expect(await screen.findByText("mg")).toBeInTheDocument();
  });

  it("clears the field and reads the units again after a create", async () => {
    renderScreen();
    expect(await screen.findByText("mg")).toBeInTheDocument();

    await userEvent.type(field(), "mL");
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => {
        onServer = units(["mg", "mL"]);
        callback(true);
      },
    );

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    await waitFor(() => expect(screen.getByText("mL")).toBeInTheDocument());
    expect(field()).toHaveValue("");
    // Creating used to reload the document, which threw away the whole app.
    expect(reload).not.toHaveBeenCalled();
  });

  it("checks a new name against the units it read most recently", async () => {
    renderScreen();
    expect(await screen.findByText("mg")).toBeInTheDocument();

    await userEvent.type(field(), "mL");
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => {
        onServer = units(["mg", "mL"]);
        callback(true);
      },
    );
    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));
    await waitFor(() => expect(screen.getByText("mL")).toBeInTheDocument());

    // The name just created is a duplicate now.
    await userEvent.type(field(), "mL");

    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();
  });

  it("keeps the typed name when the create fails", async () => {
    renderScreen();
    expect(await screen.findByText("mg")).toBeInTheDocument();

    await userEvent.type(field(), "mL");
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => callback(false),
    );

    await userEvent.click(screen.getByRole("button", { name: "Next" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    // Reloading on a failed create threw the entry away and said nothing.
    expect(field()).toHaveValue("mL");
    expect(reload).not.toHaveBeenCalled();
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "error" }),
    );
  });

  it("goes back to the unit list without leaving the app", async () => {
    renderScreen();
    expect(await screen.findByText("mg")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Previous" }));

    expect(await screen.findByText("unit list")).toBeInTheDocument();
    expect(window.location.replace).not.toHaveBeenCalled();
  });
});
