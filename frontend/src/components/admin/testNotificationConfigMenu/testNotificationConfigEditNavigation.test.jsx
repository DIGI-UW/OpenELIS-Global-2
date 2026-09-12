/**
 * TestNotificationConfigEdit's Exit button returns to the menu list, a
 * different route/component.
 */
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/contexts";
import TestNotificationConfigEdit from "./TestNotificationConfigEdit";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const renderScreen = () =>
  render(
    <MemoryRouter
      initialEntries={["/MasterListsPage/testNotificationConfig?testId=1"]}
    >
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
        }}
      >
        <IntlProvider locale="en" messages={messages}>
          <Route path="/MasterListsPage/testNotificationConfig">
            <TestNotificationConfigEdit />
          </Route>
          <Route path="/MasterListsPage/testNotificationConfigMenu">
            <div>test notification menu</div>
          </Route>
        </IntlProvider>
      </NotificationContext.Provider>
    </MemoryRouter>,
  );

describe("TestNotificationConfigEdit navigation", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/TestNotificationConfig")) {
        return callback({
          testId: "1",
          patientEmail: { active: false },
          patientSMS: { active: false },
          providerEmail: { active: false },
          providerSMS: { active: false },
        });
      }
      if (url.startsWith("/rest/test-list")) {
        return callback([{ id: "1", value: "Test One" }]);
      }
      return callback([]);
    });
  });

  it("goes back to the menu list through the router", async () => {
    renderScreen();

    fireEvent.click(await screen.findByText("Exit"));

    expect(
      await screen.findByText("test notification menu"),
    ).toBeInTheDocument();
  });
});
