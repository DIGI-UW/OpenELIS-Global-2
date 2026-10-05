import React from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import Dashboard from "./Dashboard";
import { getFromOpenElisServer } from "../utils/Utils";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  convertAlphaNumLabNumForDisplay: (value) => value,
  hasRole: () => false,
  Roles: { GLOBAL_ADMIN: "Global Administrator" },
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

test("a non-admin opens a tile on every section it counted, not on the first one", () => {
  // The tile's number and the tile's list must answer the same question. The
  // count is taken across every section the reader can see, so the list that
  // opens behind it must not be narrowed to one arbitrary section. Defaulting
  // to sections[0] showed a non-zero tile over an empty table whenever the
  // reader's work sat in any section but the first returned.
  const listRequests = [];
  getFromOpenElisServer.mockImplementation((endpoint, callback) => {
    if (endpoint === "/rest/home-dashboard/metrics") {
      requests.push({ callback });
    } else if (endpoint === "/rest/user-test-sections/ALL") {
      callback([
        { id: "36", value: "Hematology" },
        { id: "170", value: "Environmental & Vector Bacteriology" },
      ]);
    } else if (endpoint.startsWith("/rest/home-dashboard/ORDERS_IN_PROGRESS")) {
      listRequests.push(endpoint);
      callback({ paging: {}, displayItems: [] });
    }
  });

  render(
    <UserSessionDetailsContext.Provider
      value={{ userSessionDetails: { loginName: "results" } }}
    >
      <IntlProvider locale="en" messages={messages}>
        <Dashboard />
      </IntlProvider>
    </UserSessionDetailsContext.Provider>,
  );
  act(() => requests[0].callback({ ordersInProgress: 3 }));
  fireEvent.click(screen.getByText("In Progress").closest(".dashboard-tile"));

  expect(listRequests).not.toHaveLength(0);
  // Not pinned to Hematology just because it came back first.
  expect(listRequests[listRequests.length - 1]).not.toContain(
    "testSectionId=36",
  );
});
