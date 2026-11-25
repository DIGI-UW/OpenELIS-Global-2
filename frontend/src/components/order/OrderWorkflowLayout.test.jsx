import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi, describe, it, expect, beforeEach } from "vitest";
import userEvent from "@testing-library/user-event";
import messages from "../../languages/en.json";

const { orderContextValue, push } = vi.hoisted(() => ({
  orderContextValue: {
    saveStatus: "saved",
    isDirty: false,
    labNumber: "LAB-42",
    isReadOnly: false,
    isEditMode: false,
    enableEditMode: vi.fn(),
    orderData: {},
  },
  push: vi.fn(),
}));

vi.mock("react-router-dom", () => ({
  useLocation: () => ({ pathname: "/order/clinical/enter", search: "" }),
  useHistory: () => ({ push }),
}));
vi.mock("./OrderContext", () => ({
  useOrderContext: () => orderContextValue,
  SaveStatus: {
    SAVED: "saved",
    SAVING: "saving",
    ERROR: "error",
    UNSAVED: "unsaved",
  },
}));
vi.mock("../common/PageBreadCrumb", () => ({
  default: ({ breadcrumbs }) => (
    <nav>
      {breadcrumbs.map((crumb) => (
        <span key={crumb.label}>{messages[crumb.label] || crumb.label}</span>
      ))}
    </nav>
  ),
}));
vi.mock("./OrderContextCard", () => ({ default: () => null }));
const { searchCases } = vi.hoisted(() => ({
  searchCases: vi.fn(() => Promise.resolve({ rows: [] })),
}));
vi.mock("../microbiology/CaseWorkspaceService", () => ({ searchCases }));
vi.mock("./BarcodeScannerBar", () => ({ default: () => null }));
vi.mock("./SaveNavigationButtons", () => ({ default: () => null }));
vi.mock("./OrderStepper", async () => {
  const actual = await vi.importActual("./OrderStepper");
  return { ...actual, default: () => null };
});

import OrderWorkflowLayout from "./OrderWorkflowLayout";

const renderLayout = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <OrderWorkflowLayout title="order.step.enter">
        <div>form</div>
      </OrderWorkflowLayout>
    </IntlProvider>,
  );

describe("the state after the entry step saves", () => {
  beforeEach(() => {
    push.mockClear();
    orderContextValue.saveStatus = "saved";
    orderContextValue.isDirty = false;
    orderContextValue.labNumber = "LAB-42";
  });

  it("leads with the domain action, not a generic order parent", () => {
    renderLayout();
    expect(screen.getByText("Clinical Orders")).toBeInTheDocument();
    expect(screen.queryByText("Add Generic Order")).not.toBeInTheDocument();
  });

  it("names the saved order and offers the next step", async () => {
    renderLayout();

    const collect = messages["order.step.prepare"];
    expect(screen.getByText("Order LAB-42 saved")).toBeInTheDocument();
    expect(screen.getByText(`Next: ${collect}`)).toBeInTheDocument();
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: collect }));
    expect(push).toHaveBeenCalledWith("/order/clinical/collect");
  });

  it("leaves focus and typing with the form of a reopened saved order", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <OrderWorkflowLayout title="order.step.enter">
          <label>
            Collection Date
            <input />
          </label>
        </OrderWorkflowLayout>
      </IntlProvider>,
    );
    const collect = messages["order.step.prepare"];
    expect(screen.getByRole("button", { name: collect })).not.toHaveFocus();

    const user = userEvent.setup();
    const date = screen.getByRole("textbox", { name: "Collection Date" });
    await user.click(date);
    await user.keyboard("2026-09-28{Enter}");

    expect(date).toHaveFocus();
    expect(date).toHaveValue("2026-09-28");
    expect(push).not.toHaveBeenCalled();
  });

  it("says nothing while the order is unsaved or has changed since", () => {
    orderContextValue.labNumber = null;
    const first = renderLayout();
    expect(screen.queryByText(/saved$/)).not.toBeInTheDocument();
    first.unmount();

    orderContextValue.labNumber = "LAB-42";
    orderContextValue.isDirty = true;
    renderLayout();
    expect(screen.queryByText("Order LAB-42 saved")).not.toBeInTheDocument();
  });
});

// OGC-1266 FR-A4: a cancelled order is read-only and offers no Edit.
describe("a cancelled order", () => {
  beforeEach(() => {
    orderContextValue.isReadOnly = true;
    orderContextValue.progress = {
      status: "CANCELLED",
      cancelReason: "Duplicate order",
    };
  });

  afterEach(() => {
    orderContextValue.isReadOnly = false;
    orderContextValue.progress = undefined;
  });

  it("says why it was cancelled and offers no Edit", () => {
    renderLayout();

    expect(
      screen.getByText("This order was cancelled: Duplicate order"),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Edit" })).toBeNull();
  });
});

describe("the save status indicator", () => {
  beforeEach(() => {
    orderContextValue.saveStatus = "saved";
    orderContextValue.isDirty = false;
    orderContextValue.labNumber = "LAB-42";
    orderContextValue.orderId = "42";
  });

  // OGC-1201 AE: saveStatus starts at SAVED, so a brand-new entry form
  // announced an order that had never been saved. OGC-1051 is the same
  // indicator claiming the opposite; neither is true before an order exists.
  it("says nothing on an untouched new order", () => {
    orderContextValue.labNumber = null;
    orderContextValue.orderId = null;
    renderLayout();

    expect(screen.queryByText("Saved")).not.toBeInTheDocument();
    expect(screen.queryByText("Unsaved changes")).not.toBeInTheDocument();
  });

  it("reports unsaved changes once the user edits a new order", () => {
    orderContextValue.labNumber = null;
    orderContextValue.orderId = null;
    orderContextValue.isDirty = true;
    renderLayout();

    expect(screen.getByText("Unsaved changes")).toBeInTheDocument();
  });

  it("reports a saved order that exists", () => {
    renderLayout();

    expect(screen.getByText("Saved")).toBeInTheDocument();
  });

  it("reports a failed save on an order that does not exist yet", () => {
    orderContextValue.labNumber = null;
    orderContextValue.orderId = null;
    orderContextValue.isDirty = true;
    orderContextValue.saveStatus = "error";
    renderLayout();

    expect(screen.getByText("Save failed")).toBeInTheDocument();
  });
});

describe("the microbiology cases a saved order opened", () => {
  beforeEach(() => {
    push.mockClear();
    searchCases.mockReset();
    orderContextValue.saveStatus = "saved";
    orderContextValue.isDirty = false;
    orderContextValue.labNumber = "LAB-42";
  });

  it("names each case's lab unit and opens the case", async () => {
    searchCases.mockResolvedValue({
      rows: [{ id: "case-1", labUnit: "M4 Bacteriology" }],
    });
    renderLayout();

    await screen.findByText("Opened a microbiology case in M4 Bacteriology");
    expect(searchCases.mock.calls[0][0]).toContain("accessionNumber=LAB-42");
    await userEvent.click(screen.getByRole("button", { name: "Open case" }));
    expect(push).toHaveBeenCalledWith(
      expect.stringContaining("/Microbiology/cases/case-1"),
    );
    expect(screen.queryByText("Focus sentinel")).toBeNull();
  });

  it("ignores a late answer for the order shown before", async () => {
    let lateAnswer;
    searchCases
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            lateAnswer = resolve;
          }),
      )
      .mockResolvedValueOnce({
        rows: [{ id: "case-2", labUnit: "TB unit" }],
      });
    const view = renderLayout();
    orderContextValue.labNumber = "LAB-43";
    view.rerender(
      <IntlProvider locale="en" messages={messages}>
        <OrderWorkflowLayout title="order.step.enter">
          <div>form</div>
        </OrderWorkflowLayout>
      </IntlProvider>,
    );

    await screen.findByText("Opened a microbiology case in TB unit");
    await act(async () =>
      lateAnswer({ rows: [{ id: "case-1", labUnit: "M4 Bacteriology" }] }),
    );
    expect(
      screen.queryByText("Opened a microbiology case in M4 Bacteriology"),
    ).toBeNull();
    expect(
      screen.getByText("Opened a microbiology case in TB unit"),
    ).toBeInTheDocument();
  });

  it("says nothing for an order with unsaved changes", () => {
    orderContextValue.isDirty = true;
    searchCases.mockResolvedValue({
      rows: [{ id: "case-1", labUnit: "M4 Bacteriology" }],
    });
    renderLayout();

    expect(searchCases).not.toHaveBeenCalled();
    expect(screen.queryByText(/Opened a microbiology case/)).toBeNull();
  });
});
