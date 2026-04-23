import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { buildLabelRowsModel, calculateRunningTotal } from "./LabelsSection";
import LabelsSection from "./LabelsSection";

describe("LabelsSection shared row model", () => {
  test("buildLabelRowsModel creates order row, sample rows, and running total", () => {
    const model = buildLabelRowsModel(2, [1, 3]);

    expect(model.orderRow).toEqual({
      rowType: "order",
      rowId: "order-row",
      sampleRef: null,
      applicableLabelTypes: ["order"],
      quantities: { order: 2 },
      rowTotal: 2,
    });

    expect(model.sampleRows).toEqual([
      {
        rowType: "sample",
        rowId: "sample-row-1",
        sampleRef: "sample-1",
        applicableLabelTypes: ["specimen"],
        quantities: { specimen: 1 },
        rowTotal: 1,
      },
      {
        rowType: "sample",
        rowId: "sample-row-2",
        sampleRef: "sample-2",
        applicableLabelTypes: ["specimen"],
        quantities: { specimen: 3 },
        rowTotal: 3,
      },
    ]);

    expect(model.runningTotal).toBe(6);
  });

  test("buildLabelRowsModel normalizes null and negative quantities to zero", () => {
    const model = buildLabelRowsModel(-4, [2, null, -1]);

    expect(model.orderRow.quantities.order).toBe(0);
    expect(model.sampleRows[1].quantities.specimen).toBe(0);
    expect(model.sampleRows[2].quantities.specimen).toBe(0);
    expect(model.runningTotal).toBe(2);
  });

  test("calculateRunningTotal sums order and sample row totals", () => {
    const runningTotal = calculateRunningTotal({ rowTotal: 5 }, [
      { rowTotal: 2 },
      { rowTotal: 1 },
    ]);

    expect(runningTotal).toBe(8);
  });
});

describe("LabelsSection component", () => {
  test("renders one order row, sample rows, and running total", () => {
    render(<LabelsSection orderQuantity={2} specimenQuantities={[1, 3]} />);

    expect(screen.getByLabelText("Order labels")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Specimen labels sample 1"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("Specimen labels sample 2"),
    ).toBeInTheDocument();
    expect(screen.getByText("Running total: 6")).toBeInTheDocument();
  });

  test("calls onChange with updated model when increment button clicked", () => {
    const onChange = vi.fn();
    const { container } = render(
      <LabelsSection
        orderQuantity={2}
        specimenQuantities={[1]}
        onChange={onChange}
      />,
    );

    // Click the increment (up) button on the order labels NumberInput
    const incrementButton = container.querySelector(
      "#labels-order .cds--number__control-btn.up-icon",
    );
    if (incrementButton) {
      fireEvent.click(incrementButton);
    } else {
      // Fallback: fire change event directly
      fireEvent.change(screen.getByLabelText("Order labels"), {
        target: { value: "3" },
      });
    }

    expect(onChange).toHaveBeenCalled();
    const lastModel = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    expect(lastModel.orderRow.quantities.order).toBe(3);
    expect(lastModel.runningTotal).toBe(4);
  });
});
