import React from "react";
import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import { vi } from "vitest";

const { orderContextValue, currentLocation } = vi.hoisted(() => ({
  orderContextValue: { resetOrder: vi.fn(), isEditMode: false },
  currentLocation: { pathname: "/order/clinical/enter", search: "" },
}));

vi.mock("react-router-dom", () => ({
  useLocation: () => currentLocation,
}));

vi.mock("./OrderContext", () => ({
  useOrderContext: () => orderContextValue,
}));

import { useNewOrderReset } from "./useNewOrderReset";

const Probe = ({ workflowPrefix }) => {
  const isNewOrder = useNewOrderReset(workflowPrefix);
  return <span data-testid="is-new-order">{String(isNewOrder)}</span>;
};

const mountAt = (pathname, search, workflowPrefix = "/order/clinical") => {
  currentLocation.pathname = pathname;
  currentLocation.search = search;
  return render(<Probe workflowPrefix={workflowPrefix} />);
};

describe("useNewOrderReset", () => {
  beforeEach(() => {
    orderContextValue.resetOrder.mockClear();
    orderContextValue.isEditMode = false;
  });

  it("clears the previous order when the step is entered without an order", () => {
    const { getByTestId } = mountAt("/order/clinical/enter", "");

    expect(orderContextValue.resetOrder).toHaveBeenCalledTimes(1);
    expect(getByTestId("is-new-order")).toHaveTextContent("true");
  });

  // OGC-1201 AR: edit mode outlives the step that set it, so pressing Edit and
  // then re-entering the step from the side navigation used to carry the
  // previous order's lab number and patient onto a brand-new form.
  it("still clears the previous order while edit mode is on", () => {
    orderContextValue.isEditMode = true;

    mountAt("/order/clinical/enter", "");

    expect(orderContextValue.resetOrder).toHaveBeenCalledTimes(1);
  });

  it("keeps the order the URL addresses", () => {
    const { getByTestId } = mountAt("/order/clinical/enter", "?order=LAB-1");

    expect(orderContextValue.resetOrder).not.toHaveBeenCalled();
    expect(getByTestId("is-new-order")).toHaveTextContent("false");
  });

  // The barcode scanner pushes ?labNumber=, which the clinical and vector
  // steps used to ignore — a scanned order was reset on arrival.
  it("keeps an order addressed by the barcode scanner's labNumber", () => {
    mountAt("/order/clinical/enter", "?labNumber=LAB-2");

    expect(orderContextValue.resetOrder).not.toHaveBeenCalled();
  });

  it("clears an order parameter carried in from another workflow", () => {
    mountAt(
      "/order/environmental/enter",
      "?order=LAB-3",
      "/order/environmental",
    );
    expect(orderContextValue.resetOrder).not.toHaveBeenCalled();

    orderContextValue.resetOrder.mockClear();
    mountAt("/order/clinical/enter", "?order=LAB-3", "/order/environmental");

    expect(orderContextValue.resetOrder).toHaveBeenCalledTimes(1);
  });
});
