import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../languages/en.json";

const { location, push, orderContextValue } = vi.hoisted(() => ({
  location: { pathname: "/order/clinical/qa" },
  push: vi.fn(),
  orderContextValue: {
    isSubmitting: false,
    isReadOnly: false,
    isEditMode: false,
    saveOrder: vi.fn(),
    labNumber: "DEV01260000000000100",
    orderId: "100",
    samples: [{ sampleTypeId: "1", tests: [{ id: "a" }, { id: "b" }] }],
    sampleCheckEnabled: true,
    resetOrder: vi.fn(),
    loadOrder: vi.fn(),
  },
}));
vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push }),
  useLocation: () => location,
}));
vi.mock("./OrderContext", () => ({
  useOrderContext: () => orderContextValue,
}));

import SaveNavigationButtons from "./SaveNavigationButtons";

const renderButtons = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SaveNavigationButtons currentStep={2} {...props} />
    </IntlProvider>,
  );

beforeEach(() => {
  push.mockClear();
  orderContextValue.isSubmitting = false;
  orderContextValue.orderId = "100";
  orderContextValue.sampleCheckEnabled = true;
  orderContextValue.resetOrder.mockClear();
  orderContextValue.loadOrder.mockClear();
  location.pathname = "/order/clinical/qa";
});

// OGC-1266 FR-A1: every step ends in the same three actions.
describe("SaveNavigationButtons on the last step", () => {
  it("Save and finish runs the step's submit handler, not its plain save", async () => {
    const onSave = vi.fn();
    const onSaveAndNext = vi.fn();
    renderButtons({ onSave, onSaveAndNext });

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Save and finish" }));

    expect(onSaveAndNext).toHaveBeenCalledTimes(1);
    expect(onSave).not.toHaveBeenCalled();
  });

  it("Save and exit runs the plain save and returns to the dashboard", async () => {
    const onSave = vi.fn();
    const onSaveAndNext = vi.fn();
    renderButtons({ onSave, onSaveAndNext });

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Save and exit" }));

    expect(onSave).toHaveBeenCalledTimes(1);
    expect(onSaveAndNext).not.toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith(
      "/order/clinical?highlight=DEV01260000000000100",
    );
  });

  it("Save and finish falls back to the plain save when the step has no submit handler", async () => {
    const onSave = vi.fn();
    renderButtons({ onSave });

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Save and finish" }));

    expect(onSave).toHaveBeenCalledTimes(1);
  });

  // Found on the live walk: a save the server refused still left the page,
  // so the error the step had just shown was never seen.
  it("stays on the step when the save fails", async () => {
    const onSave = vi.fn().mockResolvedValue(false);
    renderButtons({ onSave });

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Save and exit" }));

    expect(onSave).toHaveBeenCalledTimes(1);
    expect(push).not.toHaveBeenCalled();
  });

  it("names the order in the dashboard link before the first reload has set it", async () => {
    orderContextValue.labNumber = null;
    orderContextValue.orderData = {
      sampleOrderItems: { labNo: "DEV01260000000000200" },
    };
    renderButtons({ onSave: vi.fn().mockResolvedValue(true) });

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Save and exit" }));

    expect(push).toHaveBeenCalledWith(
      "/order/clinical?highlight=DEV01260000000000200",
    );
    orderContextValue.labNumber = "DEV01260000000000100";
    orderContextValue.orderData = undefined;
  });

  it("uses the step's own primary label when it has one", () => {
    renderButtons({ primaryLabelId: "order.sampleCheck.release" });

    expect(
      screen.getByRole("button", { name: "Release for testing" }),
    ).toBeInTheDocument();
  });
});

describe("SaveNavigationButtons on a middle step", () => {
  it("says Save and next, and stays visible but disabled with the count while items are missing", () => {
    location.pathname = "/order/clinical/collect";
    renderButtons({ currentStep: 1, canProceed: false, toContinueCount: 2 });

    const next = screen.getByRole("button", { name: /Save and next/ });
    expect(next).toBeDisabled();
    expect(screen.getByText("2 items needed to continue")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save and exit" })).toBeEnabled();
  });

  it("is the last step when the clinical Sample check is off", () => {
    location.pathname = "/order/clinical/collect";
    orderContextValue.sampleCheckEnabled = false;
    renderButtons({ currentStep: 1 });

    expect(
      screen.getByRole("button", { name: "Save and finish" }),
    ).toBeInTheDocument();
  });

  it("offers no save while one is in flight", () => {
    orderContextValue.isSubmitting = true;
    renderButtons({ currentStep: 1 });

    expect(
      screen.getByRole("button", { name: "Save and exit" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Save and next" }),
    ).toBeDisabled();
    expect(screen.getByRole("button", { name: /Discard$/ })).toBeDisabled();
  });
});

// The environmental and vector lanes are outside OGC-1266 and keep their
// footer: Save stays on the step, Save & Next advances, Submit ends.
describe("SaveNavigationButtons on the environmental and vector lanes", () => {
  it("keeps Save on the step and Save & Next to advance", async () => {
    location.pathname = "/order/vector/enter";
    const onSave = vi.fn();
    renderButtons({ currentStep: 0, onSave });
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: "Save" }));
    expect(onSave).toHaveBeenCalledTimes(1);
    expect(push).not.toHaveBeenCalled();
    expect(screen.queryByRole("button", { name: /Discard$/ })).toBeNull();

    await user.click(screen.getByRole("button", { name: "Save & Next" }));
    expect(onSave).toHaveBeenCalledTimes(2);
    expect(push).toHaveBeenCalledWith(
      "/order/vector/label?order=DEV01260000000000100",
    );
  });

  it("ends the environmental workflow with Submit", async () => {
    location.pathname = "/order/environmental/qa";
    const onSaveAndNext = vi.fn();
    renderButtons({ currentStep: 2, onSaveAndNext });

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "Submit" }));

    expect(onSaveAndNext).toHaveBeenCalledTimes(1);
  });
});

// OGC-1266 FR-A3: Discard confirms first and names what is lost.
describe("Discard", () => {
  it("names the tests and samples, then reloads a saved order", async () => {
    const user = userEvent.setup();
    renderButtons({ currentStep: 1 });

    await user.click(screen.getByRole("button", { name: /Discard$/ }));
    expect(
      screen.getByText(
        "2 tests and 1 samples entered on this page will be lost.",
      ),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Discard changes$/ }));
    expect(orderContextValue.loadOrder).toHaveBeenCalledWith(
      "DEV01260000000000100",
      false,
    );
    expect(orderContextValue.resetOrder).not.toHaveBeenCalled();
  });

  it("discards the whole order when it was never saved", async () => {
    orderContextValue.orderId = null;
    const user = userEvent.setup();
    renderButtons({ currentStep: 0 });

    await user.click(screen.getByRole("button", { name: /Discard$/ }));
    await user.click(screen.getByRole("button", { name: /Discard order$/ }));

    expect(orderContextValue.resetOrder).toHaveBeenCalledTimes(1);
    expect(push).toHaveBeenCalledWith("/order/clinical");
  });

  it("keeps everything when the user stays", async () => {
    const user = userEvent.setup();
    renderButtons({ currentStep: 1 });

    await user.click(screen.getByRole("button", { name: /Discard$/ }));
    await user.click(screen.getByRole("button", { name: "Stay" }));

    expect(orderContextValue.loadOrder).not.toHaveBeenCalled();
    expect(orderContextValue.resetOrder).not.toHaveBeenCalled();
  });
});
