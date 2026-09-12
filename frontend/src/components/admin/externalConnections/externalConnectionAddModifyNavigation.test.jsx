/**
 * ExternalConnectionAddModify leaves for the list screen three ways: a
 * missing-ID guard, a successful save, and the Exit button. All three sent
 * the browser back to /MasterListsPage/externalConnections, a route the
 * router already serves.
 */
import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/contexts";
import ExternalConnectionAddModify from "./ExternalConnectionAddModify";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const renderScreen = (at) =>
  render(
    <MemoryRouter initialEntries={[at]}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
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

describe("ExternalConnectionAddModify navigation", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/ExternalConnection?ID=")) {
        return callback({
          externalConnection: {
            nameLocalization: { localizedValue: "Bridge A" },
          },
          programmedConnections: [],
          authenticationTypes: [],
        });
      }
      return callback([]);
    });
    postToOpenElisServerJsonResponse.mockReset();
  });

  it("bounces to the list when opened with no ID to edit", async () => {
    vi.useFakeTimers();
    try {
      // A query string with no ID key is the guard's actual trigger — ID
      // defaults to the truthy string "0" when there is no query string at
      // all, which the component's own guard treats as present.
      renderScreen("/MasterListsPage/externalConnectionEdit?ref=menu");
      act(() => vi.advanceTimersByTime(1000));
      expect(
        await screen.findByText("external connections list"),
      ).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it("goes to the list after a successful save", async () => {
    vi.useFakeTimers();
    try {
      renderScreen("/MasterListsPage/externalConnectionEdit?ID=5");
      const nameField = document.getElementById("connection-name");
      expect(nameField).toHaveValue("Bridge A");
      fireEvent.change(nameField, { target: { value: "Bridge A Updated" } });
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, body, callback) => callback(200),
      );
      fireEvent.click(screen.getByText("Save"));
      act(() => vi.advanceTimersByTime(200));
      expect(
        await screen.findByText("external connections list"),
      ).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it("goes to the list on Exit without saving", async () => {
    renderScreen("/MasterListsPage/externalConnectionEdit?ID=5");
    expect(document.getElementById("connection-name")).toHaveValue("Bridge A");

    fireEvent.click(screen.getByText("Exit"));

    expect(
      await screen.findByText("external connections list"),
    ).toBeInTheDocument();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });
});
