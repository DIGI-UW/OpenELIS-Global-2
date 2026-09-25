import React from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import Dashboard from "./Dashboard";
import { getFromOpenElisServer } from "../utils/Utils";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  convertAlphaNumLabNumForDisplay: (value) => value,
  hasRole: () => false,
}));
vi.mock("../layout/Layout", async () => {
  const { createContext } = await import("react");
  return {
    NotificationContext: createContext({
      notificationVisible: false,
      setNotificationVisible: vi.fn(),
      addNotification: vi.fn(),
    }),
  };
});
vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { warning: "warning" },
}));

let requests;
beforeEach(() => {
  requests = [];
  getFromOpenElisServer.mockImplementation((endpoint, callback, signal) => {
    if (endpoint === "/rest/home-dashboard/metrics") {
      requests.push({ callback, signal });
    }
  });
});

const openDashboard = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <Dashboard />
    </IntlProvider>,
  );

test("failed metrics show a recoverable error instead of crashing or showing zero counts", () => {
  openDashboard();
  act(() => requests[0].callback(undefined));
  expect(screen.getByRole("alert")).toHaveTextContent(
    "Dashboard counts could not be loaded.",
  );
  expect(screen.queryByText("In Progress")).not.toBeInTheDocument();
  expect(screen.queryByText("Loading Dasboard...")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Retry", exact: true }));
  expect(requests).toHaveLength(2);
  act(() => requests[1].callback({ ordersInProgress: 127 }));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  const tile = screen.getByText("In Progress").closest(".dashboard-tile");
  expect(within(tile).getByText("127")).toBeVisible();
});

test("leaving the Dashboard aborts its pending metrics request", () => {
  const { unmount } = openDashboard();
  expect(requests[0].signal).toBeInstanceOf(AbortSignal);
  expect(requests[0].signal.aborted).toBe(false);
  act(() => unmount());
  expect(requests[0].signal.aborted).toBe(true);
});

test("a late response from a replaced request cannot overwrite retry results", () => {
  openDashboard();
  act(() => requests[0].callback(undefined));
  fireEvent.click(screen.getByRole("button", { name: "Retry", exact: true }));
  act(() => requests[1].callback({ ordersInProgress: 127 }));
  act(() => requests[0].callback(undefined));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.getByText("127")).toBeVisible();
});
