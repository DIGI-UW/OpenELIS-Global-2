import { buildLabelRowsModel, calculateRunningTotal } from "./LabelsSection";

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
