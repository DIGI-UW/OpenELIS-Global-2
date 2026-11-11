/**
 * Every field on the external connection form is named by its label, so a
 * screen reader announces "Username" rather than an anonymous edit box; and
 * an edited name or description is what the save sends, not the stored copy
 * the server would otherwise apply over it.
 */
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import ExternalConnectionAddModify from "./ExternalConnectionAddModify";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

describe("ExternalConnectionAddModify labels", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({
        externalConnection: {
          nameLocalization: {
            id: "1688",
            localizedValue: "Test SMSC",
            english: "Test SMSC",
            valuesAsMap: { en: "Test SMSC" },
          },
          descriptionLocalization: {
            id: "1687",
            localizedValue: "Lab SMS gateway",
            english: "Lab SMS gateway",
            valuesAsMap: { en: "Lab SMS gateway" },
          },
          programmedConnection: "SMPP_SERVER",
          activeAuthenticationType: "BASIC",
          uri: "smpp://smsc.example.org:2775",
          active: true,
        },
        basicAuthenticationData: { username: "oesmpp" },
        programmedConnections: ["SMPP_SERVER"],
        authenticationTypes: ["BASIC", "NONE"],
      }),
    );
  });

  const addNotification = vi.fn();
  const renderForm = () =>
    render(
      <MemoryRouter
        initialEntries={["/MasterListsPage/externalConnectionEdit?ID=2"]}
      >
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification,
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <Route path="/MasterListsPage/externalConnectionEdit">
              <ExternalConnectionAddModify />
            </Route>
            <Route path="/MasterListsPage/externalConnections">
              <div>external connections list</div>
            </Route>
          </IntlProvider>
        </NotificationContext.Provider>
      </MemoryRouter>,
    );

  it("saves the edited name and description, not the stored copies", async () => {
    postToOpenElisServerJsonResponse.mockReset();
    renderForm();
    fireEvent.change(await screen.findByLabelText(/Description/), {
      target: { value: "Renamed gateway" },
    });
    fireEvent.change(document.getElementById("connection-name"), {
      target: { value: "Test SMSC 2" },
    });
    fireEvent.click(screen.getByText("Save"));

    const body = JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]);
    expect(body.externalConnection.nameLocalization).toEqual({
      localizedValue: "Test SMSC 2",
    });
    expect(body.externalConnection.descriptionLocalization).toEqual({
      localizedValue: "Renamed gateway",
    });
  });

  it("keeps the form open and says why when the server refuses the save", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      addNotification.mockReset();
      postToOpenElisServerJsonResponse.mockReset();
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, body, callback) =>
          callback({
            message: "Failed to save external connection: boom",
            status: 500,
          }),
      );
      renderForm();
      fireEvent.change(await screen.findByLabelText(/Description/), {
        target: { value: "Renamed gateway" },
      });
      fireEvent.click(screen.getByText("Save"));
      vi.advanceTimersByTime(1000);

      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: NotificationKinds.error,
          message:
            "The connection was not saved: Failed to save external connection: boom",
        }),
      );
      expect(screen.queryByText("external connections list")).toBeNull();
      expect(screen.getByLabelText(/Description/)).toHaveValue(
        "Renamed gateway",
      );
    } finally {
      vi.useRealTimers();
    }
  });

  it("names the description, username and password fields by their labels", async () => {
    renderForm();

    expect(await screen.findByLabelText(/Description/)).toHaveValue(
      "Lab SMS gateway",
    );
    expect(screen.getByLabelText(/Username/)).toHaveValue("oesmpp");
    expect(screen.getByLabelText(/^Password/)).toHaveAttribute(
      "id",
      "basic-auth-password",
    );
  });
});
