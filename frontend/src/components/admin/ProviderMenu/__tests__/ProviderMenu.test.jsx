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
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import {
  NotificationContext,
  ConfigurationContext,
} from "../../../layout/contexts";
import ProviderMenu from "../ProviderMenu";

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
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

const providers = (lastNames) => ({
  providers: lastNames.map((lastName, index) => ({
    id: String(index + 1),
    fhirUuid: `uuid-${index + 1}`,
    active: true,
    person: {
      lastName,
      firstName: "Ada",
      workPhone: "555",
      fax: "",
      email: "",
    },
  })),
  fromRecordCount: "1",
  toRecordCount: String(lastNames.length),
  totalRecordCount: String(lastNames.length),
});

describe("ProviderMenu", () => {
  let reload;
  let onServer;

  const renderScreen = () =>
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <ConfigurationContext.Provider
              value={{
                reloadConfiguration: vi.fn(),
                configurationProperties: {},
              }}
            >
              <NotificationContext.Provider
                value={{
                  notificationVisible: false,
                  setNotificationVisible: vi.fn(),
                  addNotification: vi.fn(),
                }}
              >
                <ProviderMenu />
              </NotificationContext.Provider>
            </ConfigurationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    onServer = providers(["Lovelace"]);
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      url.startsWith("/rest/ProviderMenu") ||
      url.startsWith("/rest/SearchProviderMenu")
        ? callback(onServer)
        : callback(undefined),
    );
    postToOpenElisServerFullResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  it("reads the providers once, and only the browse endpoint", async () => {
    renderScreen();

    await waitFor(() =>
      expect(screen.getByText("Lovelace")).toBeInTheDocument(),
    );
    const reads = getFromOpenElisServer.mock.calls.map(([url]) => url);
    expect(
      reads.filter((u) => u.startsWith("/rest/ProviderMenu")),
    ).toHaveLength(1);
    // Searching is a different endpoint and nothing has been searched for.
    expect(
      reads.filter((u) => u.startsWith("/rest/SearchProviderMenu")),
    ).toEqual([]);
  });

  it("reads the providers again when a write answers, not before", async () => {
    renderScreen();
    await waitFor(() =>
      expect(screen.getByText("Lovelace")).toBeInTheDocument(),
    );

    let answer;
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => {
        answer = () => {
          onServer = providers(["Hopper"]);
          callback({ status: 200 });
        };
      },
    );

    fireEvent.click(screen.getAllByLabelText("selectRows")[0]);
    await userEvent.click(screen.getByRole("button", { name: "Deactivate" }));

    // The write is still in flight: the list stands and the page is not thrown
    // away, which is what reloading straight after sending used to do.
    expect(screen.getByText("Lovelace")).toBeInTheDocument();
    expect(reload).not.toHaveBeenCalled();

    answer();
    await waitFor(() => expect(screen.getByText("Hopper")).toBeInTheDocument());
    expect(reload).not.toHaveBeenCalled();
  });

  it("clears the row selection after a deactivation, so Modify has nothing stale to open", async () => {
    renderScreen();
    await waitFor(() =>
      expect(screen.getByText("Lovelace")).toBeInTheDocument(),
    );

    fireEvent.click(screen.getAllByLabelText("selectRows")[0]);
    const modifyButton = screen.getByRole("button", { name: "Modify" });
    expect(modifyButton).toBeEnabled();

    // The row is gone from the next read, the way a real deactivation leaves it.
    postToOpenElisServerFullResponse.mockImplementation(
      (url, payload, callback) => {
        onServer = providers([]);
        callback({ status: 200 });
      },
    );
    await userEvent.click(screen.getByRole("button", { name: "Deactivate" }));
    await waitFor(() =>
      expect(screen.queryByText("Lovelace")).not.toBeInTheDocument(),
    );

    // Modify used to stay enabled on the row id that no longer exists, and
    // clicking it crashed the screen looking that row up.
    expect(screen.getByRole("button", { name: "Modify" })).toBeDisabled();
  });
});
