import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import HomeDashBoard from "../Dashboard";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { NotificationContext } from "../../layout/Layout";
import { getFromOpenElisServer } from "../../utils/Utils";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
  };
});

const ORDER_COUNT = 150;
const SERVER_PAGE_SIZE = 100;

const orders = Array.from({ length: ORDER_COUNT }, (_, index) => ({
  id: String(index + 1),
  priority: "ROUTINE",
  orderDate: "2026-09-14",
  patientId: "P" + (index + 1),
  // 16 chars: long lab numbers are displayed verbatim, no dashed reformatting
  labNumber: "ACC" + String(index + 1).padStart(13, "0"),
  testName: "Test " + (index + 1),
  testSection: "1",
}));

const notificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  addNotification: vi.fn(),
};

const userSessionDetails = {
  userSessionDetails: { loginName: "admin", roles: ["Global Administrator"] },
};

const renderDashboard = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <UserSessionDetailsContext.Provider value={userSessionDetails}>
          <NotificationContext.Provider value={notificationContext}>
            <HomeDashBoard />
          </NotificationContext.Provider>
        </UserSessionDetailsContext.Provider>
      </MemoryRouter>
    </IntlProvider>,
  );

const openInProgressTile = async (user) => {
  await user.click(await screen.findByText("In Progress"));
  await screen.findByLabelText("Items per page");
};

describe("Home dashboard order list", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/home-dashboard/metrics")) {
        callback({ ordersInProgress: ORDER_COUNT });
      } else if (url.startsWith("/rest/user-test-sections/ALL")) {
        callback([{ id: "1", value: "Haematology" }]);
      } else if (url.startsWith("/rest/home-dashboard/ORDERS_IN_PROGRESS")) {
        // Mirrors the server: one page of orders at a time, the rest behind
        // ?page=N.
        const requested = Number(
          new URLSearchParams(url.split("?")[1]).get("page") ?? 1,
        );
        callback({
          displayItems: orders.slice(
            (requested - 1) * SERVER_PAGE_SIZE,
            requested * SERVER_PAGE_SIZE,
          ),
          paging: {
            currentPage: String(requested),
            totalPages: String(Math.ceil(ORDER_COUNT / SERVER_PAGE_SIZE)),
          },
        });
      } else {
        callback({});
      }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("pages past the first 100 orders instead of disabling next", async () => {
    const user = userEvent.setup();
    renderDashboard();

    await openInProgressTile(user);

    expect(await screen.findByText("1-100 of 150 items")).toBeInTheDocument();
    expect(screen.getByText("ACC0000000000001")).toBeInTheDocument();
    expect(screen.queryByText("ACC0000000000150")).not.toBeInTheDocument();

    const nextPage = screen.getByRole("button", { name: "Next Page" });
    expect(nextPage).toBeEnabled();

    await user.click(nextPage);

    expect(await screen.findByText("101-150 of 150 items")).toBeInTheDocument();
    expect(screen.getByText("ACC0000000000150")).toBeInTheDocument();
    expect(screen.queryByText("ACC0000000000001")).not.toBeInTheDocument();
  });
});
