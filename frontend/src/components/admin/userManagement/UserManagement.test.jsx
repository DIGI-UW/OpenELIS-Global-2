import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import messages from "../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { createQueryClient } from "../../utils/queryClient";
import { NotificationContext } from "../../layout/contexts";
import UserManagement from "./UserManagement";

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
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const user = (id, first, last, login) => ({
  systemUserId: id,
  combinedUserID: id,
  firstName: first,
  lastName: last,
  loginName: login,
  expDate: "01/01/2035",
  locked: "N",
  disabled: "N",
  active: "Y",
  timeout: "500",
});

const payload = (menuList) => ({
  totalRecordCount: menuList.length,
  fromRecordCount: 1,
  toRecordCount: menuList.length,
  menuList,
  testSections: [{ id: "10", value: "Serology" }],
});

const renderScreen = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <QueryClientProvider client={createQueryClient()}>
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <UserManagement />
          </NotificationContext.Provider>
        </QueryClientProvider>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("UserManagement", () => {
  let reload;

  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    // jsdom makes window.location non-writable, so replace the whole object.
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows the users the server returns", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(payload([user("1", "Open", "ELIS", "admin")])),
    );

    renderScreen();

    expect(await screen.findByText("Open")).toBeInTheDocument();
  });

  it("keeps the active filter mounted and checked while requesting active users", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(payload([user("1", "Open", "ELIS", "admin")])),
    );
    renderScreen();
    const checkbox = await screen.findByLabelText("Only Active");
    expect(checkbox).not.toBeChecked();
    await userEvent.click(screen.getByText("Only Active"));
    await waitFor(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        expect.stringContaining("filter=isActive"),
        expect.any(Function),
      ),
    );
    expect(screen.getByLabelText("Only Active")).toBe(checkbox);
    expect(checkbox).toBeChecked();
  });

  it("reads the list again after a delete, without reloading the document", async () => {
    const before = payload([
      user("1", "Open", "ELIS", "admin"),
      user("2", "Deen", "Dean", "clear"),
    ]);
    const after = payload([user("1", "Open", "ELIS", "admin")]);
    // The server's own state changes on delete, so every read after it sees the
    // shorter list. The screen fires more than one read, so keying on a call
    // count would answer the wrong one.
    let onServer = before;
    getFromOpenElisServer.mockImplementation((url, cb) => cb(onServer));
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) => {
      onServer = after;
      cb({ status: 200 });
    });

    const { container } = renderScreen();
    expect(await screen.findByText("Deen")).toBeInTheDocument();

    // The row itself toggles selection (TableRow onClick), which is how a user
    // picks the record to deactivate.
    await userEvent.click(screen.getByText("Deen").closest("tr"));
    await userEvent.click(screen.getByRole("button", { name: /deactivate/i }));

    // The write has to actually happen, or the refetch assertion below would
    // pass for the wrong reason.
    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalled(),
    );

    // The list reflects the write because the screen read it again.
    await waitFor(() =>
      expect(screen.queryByText("Deen")).not.toBeInTheDocument(),
    );
    // A document reload would have thrown away every bit of client state.
    expect(reload).not.toHaveBeenCalled();
  });

  it("tells the user once when the list fails to load", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    const addNotification = vi.fn();

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
              <UserManagement />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(addNotification).toHaveBeenCalledTimes(1));
    expect(addNotification.mock.calls[0][0]).toMatchObject({
      kind: "error",
      message: messages["server.error.msg"],
    });
  });
});
