/**
 * TestNotificationConfigMenu's Edit icon opens the per-test config editor
 * (a different route), and its Exit buttons discard unsaved checkbox toggles
 * without leaving the list. All three used to reach for a page load.
 */
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/contexts";
import TestNotificationConfigMenu from "./TestNotificationConfigMenu";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const menu = (patientEmailActive) => ({
  menuList: [
    {
      testId: "1",
      patientEmail: { active: patientEmailActive },
      patientSMS: { active: false },
      providerEmail: { active: false },
      providerSMS: { active: false },
    },
  ],
});

const renderScreen = () =>
  render(
    <MemoryRouter
      initialEntries={["/MasterListsPage/testNotificationConfigMenu"]}
    >
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
        }}
      >
        <IntlProvider locale="en" messages={messages}>
          <Route path="/MasterListsPage/testNotificationConfigMenu">
            <TestNotificationConfigMenu />
          </Route>
          <Route path="/MasterListsPage/testNotificationConfig">
            <div>test notification editor</div>
          </Route>
        </IntlProvider>
      </NotificationContext.Provider>
    </MemoryRouter>,
  );

describe("TestNotificationConfigMenu navigation", () => {
  let onServer;

  beforeEach(() => {
    onServer = menu(false);
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/TestNotificationConfigMenu")) {
        return callback(onServer);
      }
      if (url.startsWith("/rest/test-list")) {
        return callback([{ id: "1", value: "Test One" }]);
      }
      return callback([]);
    });
  });

  it("opens the per-test editor through the router", async () => {
    renderScreen();

    fireEvent.click(
      await screen.findByRole("button", { name: "Edit Test Notification" }),
    );

    expect(
      await screen.findByText("test notification editor"),
    ).toBeInTheDocument();
  });

  it("Exit discards an unchecked toggle without leaving the list", async () => {
    renderScreen();
    const checkbox = (await screen.findAllByRole("checkbox"))[0];
    expect(checkbox).not.toBeChecked();

    fireEvent.click(checkbox);
    expect(checkbox).toBeChecked();

    // Only the server knows the toggle was never saved.
    onServer = menu(false);
    fireEvent.click(screen.getAllByText("Exit")[0]);

    expect((await screen.findAllByRole("checkbox"))[0]).not.toBeChecked();
    expect(
      screen.queryByText("test notification editor"),
    ).not.toBeInTheDocument();
  });
});
