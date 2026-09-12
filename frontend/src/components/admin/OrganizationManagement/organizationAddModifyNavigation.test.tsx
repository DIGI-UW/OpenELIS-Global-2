/**
 * OrganizationAddModify leaves for the list screen three ways: a missing-ID
 * guard, a successful save, and the Exit button. All three sent the browser
 * back to /MasterListsPage/organizationManagement, a route the router already
 * serves.
 */
import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../layout/contexts";
import OrganizationAddModify from "./OrganizationAddModify";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const renderScreen = (at: string) =>
  render(
    <MemoryRouter initialEntries={[at]}>
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <Route path="/MasterListsPage/organizationEdit">
              <OrganizationAddModify />
            </Route>
            <Route path="/MasterListsPage/organizationManagement">
              <div>organization list</div>
            </Route>
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </MemoryRouter>,
  );

describe("OrganizationAddModify navigation", () => {
  beforeEach(() => {
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockReset();
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, callback: (r: unknown) => void) => {
        if (url.startsWith("/rest/Organization?ID=")) {
          return callback({
            id: "5",
            organizationName: "Org A",
            orgTypes: [],
            selectedTypes: [],
          });
        }
        return callback([]);
      },
    );
    (postToOpenElisServerJsonResponse as ReturnType<typeof vi.fn>).mockReset();
  });

  it("bounces to the list when opened with no ID to edit", async () => {
    vi.useFakeTimers();
    try {
      // A query string with no ID key is the guard's actual trigger — ID
      // defaults to the truthy string "0" when there is no query string at
      // all, which the component's own guard treats as present.
      renderScreen("/MasterListsPage/organizationEdit?ref=menu");
      act(() => {
        vi.advanceTimersByTime(1000);
      });
      expect(await screen.findByText("organization list")).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it("goes to the list after a successful save", async () => {
    vi.useFakeTimers();
    try {
      renderScreen("/MasterListsPage/organizationEdit?ID=5");
      const nameField = document.getElementById("org-name") as HTMLInputElement;
      expect(nameField).toHaveValue("Org A");
      fireEvent.change(nameField, { target: { value: "Org A Updated" } });
      (
        postToOpenElisServerJsonResponse as ReturnType<typeof vi.fn>
      ).mockImplementation(
        (url: string, body: string, callback: (status: number) => void) =>
          callback(200),
      );
      fireEvent.click(screen.getByText("Save"));
      act(() => {
        vi.advanceTimersByTime(200);
      });
      expect(await screen.findByText("organization list")).toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it("goes to the list on Exit without saving", async () => {
    renderScreen("/MasterListsPage/organizationEdit?ID=5");
    expect(document.getElementById("org-name")).toHaveValue("Org A");

    fireEvent.click(screen.getByText("Exit"));

    expect(await screen.findByText("organization list")).toBeInTheDocument();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });
});
