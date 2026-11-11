/**
 * Every control on the test notification screens has an accessible name: the
 * channel checkboxes name the channel and the test, and the subject and message
 * template fields name themselves.
 */
import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import TestNotificationConfigMenu from "./TestNotificationConfigMenu";
import TestNotificationConfigEdit from "./TestNotificationConfigEdit";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const channels = {
  testId: "1",
  patientEmail: { active: true },
  patientSMS: { active: false },
  providerEmail: { active: false },
  providerSMS: { active: false },
};

const renderAt = (entry, path, Component) =>
  render(
    <MemoryRouter initialEntries={[entry]}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
        }}
      >
        <IntlProvider locale="en" messages={messages}>
          <Route path={path}>
            <Component />
          </Route>
        </IntlProvider>
      </NotificationContext.Provider>
    </MemoryRouter>,
  );

describe("test notification screens label their controls", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/TestNotificationConfigMenu")) {
        return callback({ menuList: [channels] });
      }
      if (url.startsWith("/rest/TestNotificationConfig")) {
        return callback(channels);
      }
      if (url.startsWith("/rest/test-list")) {
        return callback([{ id: "1", value: "Test One" }]);
      }
      return callback([]);
    });
  });

  it("names each channel checkbox after its channel and test", async () => {
    renderAt(
      "/MasterListsPage/testNotificationConfigMenu",
      "/MasterListsPage/testNotificationConfigMenu",
      TestNotificationConfigMenu,
    );

    await screen.findByText("Test One", {}, { timeout: 10000 });
    const patientEmail = screen.getByLabelText(
      "Send Patient Email for Test One",
    );
    expect(patientEmail).toHaveAttribute("type", "checkbox");
    expect(patientEmail).toBeChecked();
    for (const channel of ["Patient SMS", "Provider Email", "Provider SMS"]) {
      expect(
        screen.getByLabelText(`Send ${channel} for Test One`),
      ).not.toBeChecked();
    }
  }, 30000);

  it("names the subject and message template fields", async () => {
    renderAt(
      "/MasterListsPage/testNotificationConfig?testId=1",
      "/MasterListsPage/testNotificationConfig",
      TestNotificationConfigEdit,
    );

    const subjects = await screen.findAllByRole(
      "textbox",
      { name: "Subject" },
      { timeout: 10000 },
    );
    const textboxes = screen.getAllByRole("textbox");
    const messageFields = screen.getAllByLabelText("Message");
    expect(subjects.length).toBeGreaterThanOrEqual(2);
    expect(subjects.length + messageFields.length).toBe(textboxes.length);
  }, 30000);
});
