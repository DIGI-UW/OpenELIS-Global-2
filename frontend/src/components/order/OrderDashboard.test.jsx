import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";

const {
  push,
  location,
  loadOrder,
  addNotification,
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} = vi.hoisted(() => ({
  push: vi.fn(),
  location: { pathname: "/order/clinical", search: "" },
  loadOrder: vi.fn().mockResolvedValue(),
  addNotification: vi.fn(),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push }),
  useLocation: () => location,
}));
vi.mock("./OrderContext", () => ({
  useWorkflowPrefix: () => "/order/clinical",
  useOrderContext: () => ({ loadOrder, resetOrder: vi.fn() }),
}));
vi.mock("../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification,
  }),
}));
vi.mock("../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { error: "error", success: "success" },
}));
vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
}));
vi.mock("./BarcodeScannerBar", () => ({ default: () => null }));
vi.mock("../common/PageBreadCrumb", () => ({ default: () => null }));

import OrderDashboard from "./OrderDashboard";

const ORDERS = [
  {
    id: "1",
    labNumber: "LAB-ENTERED",
    patientName: "Ada Lovelace",
    priority: "routine",
    workflowType: "clinical",
    progressStatus: "ENTERED",
    complete: false,
    stepProgress: { enter: true },
    sampleCheckEnabled: true,
  },
  {
    id: "2",
    labNumber: "LAB-DONE",
    patientName: "Grace Hopper",
    priority: "stat",
    workflowType: "clinical",
    progressStatus: "SAMPLES_PREPARED",
    complete: true,
    stepProgress: { enter: true, collect: true, label: true },
    sampleCheckEnabled: false,
  },
  {
    id: "3",
    labNumber: "LAB-CANCELLED",
    patientName: "Mary Jackson",
    priority: "routine",
    workflowType: "clinical",
    progressStatus: "CANCELLED",
    complete: false,
    stepProgress: { enter: true },
  },
  {
    id: "4",
    labNumber: "LAB-LEGACY",
    patientName: "Katherine Johnson",
    priority: "routine",
    stepProgress: { enter: true, collect: true, label: true, qa: true },
  },
  {
    id: "5",
    labNumber: "LAB-REFERRED",
    patientName: "Dorothy Vaughan",
    priority: "routine",
    workflowType: "clinical",
    progressStatus: "SAMPLES_PREPARED",
    complete: true,
    status: "referred_out",
    referralSummary: { referredTests: 2, totalTests: 2, referredTo: "CEDRES" },
    stepProgress: { enter: true, collect: true, label: true },
    sampleCheckEnabled: true,
  },
  {
    id: "6",
    labNumber: "LAB-SPLIT",
    patientName: "Annie Easley",
    priority: "routine",
    workflowType: "clinical",
    progressStatus: "SAMPLES_PREPARED",
    complete: false,
    status: "pending_qa",
    referralSummary: { referredTests: 1, totalTests: 3, referredTo: "CEDRES" },
    stepProgress: { enter: true, collect: true, label: true },
    sampleCheckEnabled: true,
  },
];

const renderDashboard = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <OrderDashboard />
    </IntlProvider>,
  );

const LAB_CELL = { selector: "div.order-lab-number" };
const rowOf = (labNumber) =>
  screen.getByText(labNumber, LAB_CELL).closest("tr");
const listed = (labNumber) => screen.findByText(labNumber, LAB_CELL);

beforeEach(() => {
  push.mockClear();
  loadOrder.mockClear();
  addNotification.mockClear();
  postToOpenElisServerJsonResponse.mockReset();
  getFromOpenElisServer.mockReset();
  location.search = "";
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/order/dashboard")) {
      callback({
        orders: ORDERS,
        paging: { currentPage: 1, totalPages: 1 },
      });
    } else if (url.includes("/rest/dictionary/categories/")) {
      callback([
        { code: "10", label: "Duplicate order" },
        { code: "11", label: "Request withdrawn" },
        { code: "12", label: "Other" },
      ]);
    }
  });
});

// OGC-1266 FR-F5: the dashboard names each order's recorded status.
describe("the order status column", () => {
  it("shows the recorded status, Complete when order entry is finished, and nothing for older orders", async () => {
    renderDashboard();

    await listed("LAB-ENTERED");
    expect(
      within(rowOf("LAB-ENTERED")).getByText("Entered"),
    ).toBeInTheDocument();
    expect(within(rowOf("LAB-DONE")).getByText("Complete")).toBeInTheDocument();
    expect(
      within(rowOf("LAB-CANCELLED")).getByText("Cancelled"),
    ).toBeInTheDocument();
    expect(within(rowOf("LAB-LEGACY")).getAllByText("---")).toHaveLength(3);
    expect(within(rowOf("LAB-ENTERED")).getAllByText("---")).toHaveLength(2);
  });

  it("offers Continue and Cancel order only on an open order", async () => {
    renderDashboard();
    await listed("LAB-ENTERED");

    const open = within(rowOf("LAB-ENTERED"));
    expect(open.getByRole("button", { name: "Continue" })).toBeInTheDocument();
    expect(
      open.getByRole("button", { name: /Cancel order$/ }),
    ).toBeInTheDocument();

    const done = within(rowOf("LAB-DONE"));
    expect(done.getByRole("button", { name: "Open" })).toBeInTheDocument();
    expect(done.queryByRole("button", { name: /Cancel order$/ })).toBeNull();

    const cancelled = within(rowOf("LAB-CANCELLED"));
    expect(cancelled.getByRole("button", { name: "Open" })).toBeInTheDocument();
    expect(
      cancelled.queryByRole("button", { name: /Cancel order$/ }),
    ).toBeNull();
  });

  it("continues an entered order at Prepare Samples", async () => {
    renderDashboard();
    await listed("LAB-ENTERED");

    await userEvent
      .setup()
      .click(
        within(rowOf("LAB-ENTERED")).getByRole("button", { name: "Continue" }),
      );

    await waitFor(() =>
      expect(push).toHaveBeenCalledWith(
        "/order/clinical/collect?order=LAB-ENTERED",
      ),
    );
  });
});

// OGC-1266 FR-A4: cancelling asks for a reason from the laboratory's list,
// records it through the cancel endpoint and refreshes the list.
describe("cancelling an order", () => {
  it("sends the chosen reason and reloads the list", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _body, callback) => callback({ progressStatus: "CANCELLED" }),
    );
    renderDashboard();
    await listed("LAB-ENTERED");
    const user = userEvent.setup();

    await user.click(
      within(rowOf("LAB-ENTERED")).getByRole("button", {
        name: /Cancel order$/,
      }),
    );
    const dialog = screen.getByRole("dialog", {
      name: "Cancel order LAB-ENTERED?",
    });
    const confirm = within(dialog).getByRole("button", {
      name: /Cancel order$/,
    });
    expect(confirm).toBeDisabled();

    await user.selectOptions(
      within(dialog).getByLabelText("Reason for cancelling"),
      "Duplicate order",
    );
    expect(confirm).toBeEnabled();
    const dashboardLoads = () =>
      getFromOpenElisServer.mock.calls.filter(([url]) =>
        url.startsWith("/rest/order/dashboard"),
      ).length;
    const loadsBefore = dashboardLoads();
    await user.click(confirm);

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/order/cancel",
      JSON.stringify({ labNumber: "LAB-ENTERED", reason: "Duplicate order" }),
      expect.any(Function),
    );
    await waitFor(() => expect(dashboardLoads()).toBe(loadsBefore + 1));
    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "success",
        message: "Order LAB-ENTERED cancelled",
      }),
    );
  });

  it("takes a free-text reason under Other", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _body, callback) => callback({ progressStatus: "CANCELLED" }),
    );
    renderDashboard();
    await listed("LAB-ENTERED");
    const user = userEvent.setup();

    await user.click(
      within(rowOf("LAB-ENTERED")).getByRole("button", {
        name: /Cancel order$/,
      }),
    );
    const dialog = screen.getByRole("dialog", {
      name: "Cancel order LAB-ENTERED?",
    });
    await user.selectOptions(
      within(dialog).getByLabelText("Reason for cancelling"),
      "Other",
    );
    const confirm = within(dialog).getByRole("button", {
      name: /Cancel order$/,
    });
    expect(confirm).toBeDisabled();
    await user.type(
      within(dialog).getByLabelText("Describe the reason"),
      "Patient seen elsewhere",
    );
    await user.click(confirm);

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/order/cancel",
      JSON.stringify({
        labNumber: "LAB-ENTERED",
        reason: "Patient seen elsewhere",
      }),
      expect.any(Function),
    );
  });

  it("reports the server's refusal and keeps the dialog", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _body, callback) => callback({ error: "order.cancelled" }),
    );
    renderDashboard();
    await listed("LAB-ENTERED");
    const user = userEvent.setup();

    await user.click(
      within(rowOf("LAB-ENTERED")).getByRole("button", {
        name: /Cancel order$/,
      }),
    );
    const dialog = screen.getByRole("dialog", {
      name: "Cancel order LAB-ENTERED?",
    });
    await user.selectOptions(
      within(dialog).getByLabelText("Reason for cancelling"),
      "Request withdrawn",
    );
    await user.click(
      within(dialog).getByRole("button", { name: /Cancel order$/ }),
    );

    expect(addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "error" }),
    );
    expect(
      screen.getByRole("dialog", { name: "Cancel order LAB-ENTERED?" }),
    ).toBeInTheDocument();
  });
});

// FR-A2 and FR-K15: Save and exit highlights the order it came from; Save
// and finish names the completed order.
describe("arriving from a step", () => {
  it("highlights the order named by Save and exit", async () => {
    location.search = "?highlight=LAB-ENTERED";
    renderDashboard();
    await listed("LAB-ENTERED");

    expect(rowOf("LAB-ENTERED")).toHaveClass("order-highlighted");
    expect(rowOf("LAB-DONE")).not.toHaveClass("order-highlighted");
    expect(screen.queryByTestId("order-finished-notice")).toBeNull();
  });

  it("names the order Save and finish completed", async () => {
    location.search = "?done=LAB-DONE";
    renderDashboard();
    await listed("LAB-DONE");

    expect(screen.getByText("Order LAB-DONE complete")).toBeInTheDocument();
    expect(rowOf("LAB-DONE")).toHaveClass("order-highlighted");
  });
});

// OGC-1423: the dashboard tells a fully referred order from a split one.
describe("referred orders", () => {
  it("shows Referred out for an order whose every test is referred, and the referral line on a split order", async () => {
    renderDashboard();
    await listed("LAB-REFERRED");
    const referred = rowOf("LAB-REFERRED");
    expect(within(referred).getByText("Referred out")).toBeInTheDocument();
    expect(
      within(referred).getByTestId("order-referral-line"),
    ).toHaveTextContent("All 2 tests referred to CEDRES");
    const split = rowOf("LAB-SPLIT");
    expect(within(split).queryByText("Referred out")).toBeNull();
    expect(within(split).getByTestId("order-referral-line")).toHaveTextContent(
      "1 of 3 tests referred to CEDRES",
    );
    expect(
      within(rowOf("LAB-ENTERED")).queryByTestId("order-referral-line"),
    ).toBeNull();
  });

  it("offers Has referred tests next to Referred Out in the status filter", async () => {
    renderDashboard();
    await listed("LAB-ENTERED");
    await userEvent.click(
      document.getElementById("status-filter").querySelector("button"),
    );
    const labels = screen
      .getAllByRole("option")
      .map((option) => option.textContent.trim());
    expect(labels).toEqual(
      expect.arrayContaining(["Has referred tests", "Referred Out"]),
    );
  });

  it("keeps the rows the server returned for a typed search", async () => {
    renderDashboard();
    await listed("LAB-REFERRED");
    const search = screen.getByRole("searchbox", { name: /filter table/i });
    await userEvent.type(search, "LAB-REFERRED");

    await waitFor(() => {
      expect(
        getFromOpenElisServer.mock.calls.some(([url]) =>
          url.includes("search=LAB-REFERRED"),
        ),
      ).toBe(true);
    });
    // Before the fix the table also ran Carbon's own text filter over cells
    // that are rendered elements, so every row the server matched was hidden.
    expect(await listed("LAB-REFERRED")).toBeInTheDocument();
    expect(screen.queryByText("No orders found")).toBeNull();
  });

  it("takes the filter labels from the message bundle", async () => {
    render(
      <IntlProvider
        locale="en"
        messages={{
          ...messages,
          "order.dashboard.status.hasReferred": "Avec tests référés",
          "order.dashboard.priority.routine": "Routinier",
        }}
      >
        <OrderDashboard />
      </IntlProvider>,
    );
    await listed("LAB-ENTERED");
    await userEvent.click(
      document.getElementById("status-filter").querySelector("button"),
    );
    expect(
      screen.getByRole("option", { name: "Avec tests référés" }),
    ).toBeInTheDocument();
    await userEvent.keyboard("{Escape}");
    await userEvent.click(
      document.getElementById("priority-filter").querySelector("button"),
    );
    expect(
      screen.getByRole("option", { name: "Routinier" }),
    ).toBeInTheDocument();
  });
});
