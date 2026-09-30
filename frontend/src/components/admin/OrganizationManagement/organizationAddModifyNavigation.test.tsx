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
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import OrganizationAddModify from "./OrganizationAddModify";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const addNotification = vi.fn();

const renderScreen = (at: string) =>
  render(
    <MemoryRouter initialEntries={[at]}>
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification,
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
    addNotification.mockReset();
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

  it("stays on the form and says the save failed when the server refuses it", async () => {
    vi.useFakeTimers();
    try {
      renderScreen("/MasterListsPage/organizationEdit?ID=5");
      // Any edit enables Save.
      fireEvent.change(
        document.getElementById("org-name") as HTMLInputElement,
        {
          target: { value: "Org A Updated" },
        },
      );
      (
        postToOpenElisServerJsonResponse as ReturnType<typeof vi.fn>
      ).mockImplementation(
        (url: string, body: string, callback: (r: unknown) => void) =>
          callback({ status: 500, error: "Request failed (HTTP 500)" }),
      );
      fireEvent.click(screen.getByText("Save"));
      act(() => {
        vi.advanceTimersByTime(1000);
      });

      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({ kind: "error" }),
      );
      expect(addNotification).not.toHaveBeenCalledWith(
        expect.objectContaining({ kind: "success" }),
      );
      expect(screen.queryByText("organization list")).toBeNull();
      expect(document.getElementById("org-name")).not.toBeNull();
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

describe("OrganizationAddModify fields", () => {
  beforeEach(() => {
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockReset();
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, callback: (r: unknown) => void) => {
        if (url.startsWith("/rest/Organization?ID=")) {
          return callback({
            id: "5",
            organizationName: "Org A",
            cliaNum: "05D1234567",
            orgTypes: [],
            selectedTypes: [],
          });
        }
        return callback([]);
      },
    );
  });

  it("names every text field for assistive technology, the CLIA number included", () => {
    const errors = vi.spyOn(console, "error");
    renderScreen("/MasterListsPage/organizationEdit?ID=5");

    expect(
      screen.getByLabelText(messages["organization.organizationName"]),
    ).toHaveValue("Org A");
    expect(
      screen.getByLabelText(messages["organization.clia.number"]),
    ).toHaveValue("05D1234567");
    for (const key of [
      "organization.short.CI",
      "organization.isActive",
      "organization.internetaddress",
      "organization.streetAddress",
      "organization.city",
    ]) {
      expect(screen.getByLabelText(messages[key])).toBeInTheDocument();
    }
    expect(
      errors.mock.calls.some(([message]) =>
        String(message).includes("`labelText` is marked as required"),
      ),
    ).toBe(false);
    errors.mockRestore();
  });

  it("keeps a street address and city as long as the columns that store them", async () => {
    renderScreen("/MasterListsPage/organizationEdit?ID=5");
    const street = screen.getByLabelText(
      messages["organization.streetAddress"],
    );
    const city = screen.getByLabelText(messages["organization.city"]);

    await userEvent.type(street, "Section 12, Lot 34, Boram Road");
    await userEvent.type(city, "Port Moresby National Capital");

    expect(street).toHaveValue("Section 12, Lot 34, Boram Road");
    expect(city).toHaveValue("Port Moresby National Capital");
  });
});
