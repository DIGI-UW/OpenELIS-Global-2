import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";

// Stable mocks so useHistory()/useLocation() return the same objects across
// renders — the stepper asserts against history.push.
const { historyMock, locationMock, orderContextValue } = vi.hoisted(() => ({
  historyMock: { push: () => {}, replace: () => {} },
  locationMock: { pathname: "/order/clinical/enter", search: "" },
  orderContextValue: {
    samples: [],
    storageSkipped: false,
    labNumber: "",
    stepProgress: {},
  },
}));

vi.mock("react-router-dom", () => ({
  useHistory: () => historyMock,
  useLocation: () => locationMock,
}));

vi.mock("./OrderContext", () => ({
  useOrderContext: () => orderContextValue,
}));

import OrderStepper, {
  CLINICAL_ORDER_STEPS,
  ENVIRONMENTAL_ORDER_STEPS,
  VECTOR_ORDER_STEPS,
} from "./OrderStepper";

const renderAt = (pathname) => {
  locationMock.pathname = pathname;
  return render(
    <IntlProvider locale="en" messages={messages}>
      <OrderStepper />
    </IntlProvider>,
  );
};

// Each Carbon ProgressStep renders a button whose title is the step label; the
// li also carries assistive text ("Complete"/"Incomplete"), so read the title.
const stepLabels = () =>
  screen.getAllByRole("button").map((button) => button.getAttribute("title"));

const currentStepLabel = (container) =>
  container
    .querySelector(".cds--progress-step--current button")
    .getAttribute("title");

describe("OrderStepper workflow step sets", () => {
  beforeEach(() => {
    historyMock.push = vi.fn();
    orderContextValue.labNumber = "";
  });

  test("environmental workflow exposes Collect between Enter and Label", () => {
    expect(ENVIRONMENTAL_ORDER_STEPS.map((s) => s.key)).toEqual([
      "enter",
      "collect",
      "label",
      "qa",
    ]);
    expect(ENVIRONMENTAL_ORDER_STEPS[1].path).toBe(
      "/order/environmental/collect",
    );
  });

  test("environmental stepper renders 4 steps including Collect", () => {
    renderAt("/order/environmental/enter");

    expect(stepLabels()).toEqual([
      "Enter Order",
      "Collect",
      "Label & Store",
      "QA Review",
    ]);
  });

  test("environmental Collect step is the active step on its own URL", () => {
    const { container } = renderAt("/order/environmental/collect");

    expect(currentStepLabel(container)).toBe("Collect");
  });

  test("clicking the environmental Collect step navigates to its route", () => {
    orderContextValue.labNumber = "ENV-42";
    renderAt("/order/environmental/enter");

    fireEvent.click(screen.getByText("Collect"));

    expect(historyMock.push).toHaveBeenCalledWith(
      "/order/environmental/collect?order=ENV-42",
    );
  });

  test("clinical workflow keeps its own Collect route", () => {
    expect(CLINICAL_ORDER_STEPS.map((s) => s.key)).toEqual([
      "enter",
      "collect",
      "label",
      "qa",
    ]);
    expect(CLINICAL_ORDER_STEPS[1].path).toBe("/order/clinical/collect");
  });

  test("vector workflow has no Collect step", () => {
    expect(VECTOR_ORDER_STEPS.map((s) => s.key)).not.toContain("collect");

    renderAt("/order/vector/enter");

    expect(stepLabels()).not.toContain("Collect");
  });
});
