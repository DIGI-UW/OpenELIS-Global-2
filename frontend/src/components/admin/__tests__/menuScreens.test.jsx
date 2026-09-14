import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import { createQueryClient } from "../../utils/queryClient";
import { NotificationContext } from "../../layout/contexts";
import ExternalConnectionMenu from "../externalConnections/ExternalConnectionMenu";
import OrganizationManagement from "../OrganizationManagement/OrganizationManagement";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
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
    postToOpenElisServer: vi.fn(),
  };
});

/**
 * Both screens list records with paging and a search box, and deactivating one
 * used to reload the document to show the list without it.
 */
const SCREENS = [
  {
    name: "ExternalConnectionMenu",
    Screen: ExternalConnectionMenu,
    browse: "/rest/ExternalConnectionMenu",
    search: "/rest/SearchExternalConnectionMenu",
    rows: (names) => ({
      menuList: names.map((name, index) => ({
        id: index + 1,
        nameLocalization: { localizedValue: name },
        programmedConnection: "",
        uri: "",
        activeAuthenticationType: "",
        active: true,
      })),
      fromRecordCount: 1,
      toRecordCount: names.length,
      totalRecordCount: names.length,
    }),
  },
  {
    name: "OrganizationManagement",
    Screen: OrganizationManagement,
    browse: "/rest/OrganizationMenu",
    search: "/rest/SearchOrganizationMenu",
    rows: (names) => ({
      menuList: names.map((organizationName, index) => ({
        id: String(index + 1),
        organizationName,
        organization: null,
        isActive: "Y",
        streetAddress: "",
      })),
      fromRecordCount: 1,
      toRecordCount: names.length,
      totalRecordCount: names.length,
    }),
  },
];

describe.each(SCREENS)("$name", ({ Screen, browse, search, rows }) => {
  let reload;
  let onServer;
  let addNotification;

  const renderScreen = () =>
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification,
              }}
            >
              <Screen />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    onServer = rows(["Kepler Lab"]);
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      url.startsWith(browse) || url.startsWith(search)
        ? callback(onServer)
        : callback(undefined),
    );
    postToOpenElisServer.mockReset();
    addNotification = vi.fn();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  it("reads the records once, from the endpoint for browsing", async () => {
    renderScreen();

    await waitFor(() =>
      expect(screen.getByText("Kepler Lab")).toBeInTheDocument(),
    );
    const reads = getFromOpenElisServer.mock.calls.map(([url]) => url);
    expect(reads.filter((u) => u.startsWith(browse))).toHaveLength(1);
    // Nothing has been searched for.
    expect(reads.filter((u) => u.startsWith(search))).toEqual([]);
  });

  it("reads the records again once one is deactivated, without reloading", async () => {
    renderScreen();
    await waitFor(() =>
      expect(screen.getByText("Kepler Lab")).toBeInTheDocument(),
    );

    postToOpenElisServer.mockImplementation((url, payload, callback) => {
      onServer = rows(["Brahe Lab"]);
      callback(200);
    });

    fireEvent.click(screen.getAllByLabelText("selectRows")[0]);
    await userEvent.click(screen.getByRole("button", { name: "Deactivate" }));

    await waitFor(() =>
      expect(screen.getByText("Brahe Lab")).toBeInTheDocument(),
    );
    expect(screen.getByRole("button", { name: "Modify" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Deactivate" })).toBeDisabled();
    // Deactivating used to reload the document, throwing away the whole app.
    expect(reload).not.toHaveBeenCalled();
  });

  it("keeps the selection and current rows when deactivation fails", async () => {
    renderScreen();
    await screen.findByText("Kepler Lab");
    const readsBefore = getFromOpenElisServer.mock.calls.length;
    postToOpenElisServer.mockImplementation((url, payload, callback) =>
      callback(500),
    );

    fireEvent.click(screen.getAllByLabelText("selectRows")[0]);
    await userEvent.click(screen.getByRole("button", { name: "Deactivate" }));

    expect(screen.getByRole("button", { name: "Modify" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Deactivate" })).toBeEnabled();
    expect(screen.getByText("Kepler Lab")).toBeInTheDocument();
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(readsBefore);
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "error" }),
    );
  });
});
