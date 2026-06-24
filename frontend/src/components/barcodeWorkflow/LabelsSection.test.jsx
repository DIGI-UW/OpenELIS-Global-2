import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import LabelsSection, {
  buildLabelRowsModel,
  calculateRunningTotal,
  calculateAggregateTotal,
  buildPersistPayload,
} from "./LabelsSection";

const renderWithIntl = (ui) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

// Mirrors the POST /api/orderEntry/labelRequest response shape (snake_case
// wire keys): two dynamic-column tables, per-cell default/max/locked/source.
const labelRequestFixture = () => ({
  order_columns: [
    { preset_id: 1, name: "Order Label", is_system: true, max: 10 },
  ],
  sample_columns: [
    { preset_id: 17, name: "Specimen Label", is_system: true, max: 5 },
    { preset_id: 24, name: "Slide Label", is_system: true, max: 12 },
  ],
  order_row: {
    cells: [
      {
        preset_id: 1,
        default: 2,
        max: 10,
        locked: false,
        source: "preset_default",
      },
    ],
  },
  sample_rows: [
    {
      sample_id_local: "S1",
      cells: [
        {
          preset_id: 17,
          default: 1,
          max: 5,
          locked: false,
          source: "test",
          source_test_id: 412,
          source_test_name: "CBC",
        },
        // A locked cell driven by a preset default.
        {
          preset_id: 24,
          default: 3,
          max: 12,
          locked: true,
          source: "preset_default",
        },
      ],
    },
    {
      sample_id_local: "S2",
      cells: [
        {
          preset_id: 17,
          default: 1,
          max: 5,
          locked: false,
          source: "test",
          source_test_id: 518,
          source_test_name: "ESR",
        },
      ],
    },
  ],
});

// =========================================================================
// API-driven mode (OGC-285 M5, T143 / T135).
// =========================================================================
describe("LabelsSection — API-driven two-table mode", () => {
  test("renders two tables: Order Labels and Sample Labels", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    const tables = screen.getAllByRole("table");
    expect(tables).toHaveLength(2);

    // Table titles come from the TableContainer header.
    expect(screen.getByText("Order Labels")).toBeInTheDocument();
    expect(screen.getByText("Sample Labels")).toBeInTheDocument();
  });

  test("renders a dynamic column per applicable preset", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    // Order table column header.
    expect(
      screen.getByRole("columnheader", { name: "Order Label" }),
    ).toBeInTheDocument();
    // Sample table column headers — both presets.
    expect(
      screen.getByRole("columnheader", { name: "Specimen Label" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("columnheader", { name: "Slide Label" }),
    ).toBeInTheDocument();
  });

  test("renders a row per sample plus the order row", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    expect(screen.getByText("Order")).toBeInTheDocument();
    expect(screen.getByText("Sample 1")).toBeInTheDocument();
    expect(screen.getByText("Sample 2")).toBeInTheDocument();
  });

  test("source Tag chips reflect each cell's source", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    // The order cell + locked sample cell are preset-default sourced.
    expect(screen.getAllByText("Preset default").length).toBeGreaterThan(0);
    // The test-sourced cells show the test name appended.
    expect(screen.getByText("From test: CBC")).toBeInTheDocument();
    expect(screen.getByText("From test: ESR")).toBeInTheDocument();
  });

  test("locked cell shows a lock icon and no editable input", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    // The locked S1 / Slide Label cell renders a lock button named after the
    // cell, not an editable spinbutton.
    const lockTrigger = screen.getByRole("button", {
      name: "Sample 1 Slide Label quantity is locked by the test configuration and cannot be changed.",
    });
    expect(lockTrigger).toBeInTheDocument();

    // No Slide Label spinbutton exists (it's the only Slide Label cell and it
    // is locked); its default renders as read-only text instead.
    expect(
      screen.queryByRole("spinbutton", {
        name: "Sample 1 Slide Label quantity",
      }),
    ).toBeNull();

    // order(1) + S1/Specimen(1) + S2/Specimen(1) = 3 editable spinbuttons.
    expect(screen.getAllByRole("spinbutton")).toHaveLength(3);
  });

  test("initial total sums every cell default (locked included)", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    // order 2 + S1 specimen 1 + S1 slide 3 (locked) + S2 specimen 1 = 7
    expect(screen.getByText("Total labels: 7")).toBeInTheDocument();
  });

  test("total recomputes when an editable cell quantity changes", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    expect(screen.getByText("Total labels: 7")).toBeInTheDocument();

    // The order cell's input is named after its column.
    fireEvent.change(screen.getByRole("spinbutton", { name: "Order Label" }), {
      target: { value: "5" },
    });

    // 2 -> 5 on the order cell: total 7 -> 10.
    expect(screen.getByText("Total labels: 10")).toBeInTheDocument();
  });

  test("editing a cell clamps the value to the column max", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    // max for the order preset is 10; 99 must clamp to 10.
    fireEvent.change(screen.getByRole("spinbutton", { name: "Order Label" }), {
      target: { value: "99" },
    });

    // order clamps to 10: total = 10 + 1 + 3 + 1 = 15.
    expect(screen.getByText("Total labels: 15")).toBeInTheDocument();
  });

  test("onChange fires with a persist payload shaped to the save contract", () => {
    const onChange = vi.fn();
    renderWithIntl(
      <LabelsSection
        labelRequest={labelRequestFixture()}
        onChange={onChange}
      />,
    );

    fireEvent.change(screen.getByRole("spinbutton", { name: "Order Label" }), {
      target: { value: "4" },
    });

    expect(onChange).toHaveBeenCalled();
    const last = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    expect(last.runningTotal).toBe(9); // 4 + 1 + 3 + 1
    expect(last.persistPayload.order_cells).toEqual([{ preset_id: 1, qty: 4 }]);
    // Sample rows keyed by client-supplied local id, one cell per sample column.
    const s1 = last.persistPayload.sample_rows.find(
      (r) => r.sample_id_local === "S1",
    );
    expect(s1.cells).toEqual([
      { preset_id: 17, qty: 1 },
      { preset_id: 24, qty: 3 },
    ]);
  });

  test("a sample cell edit is isolated to that sample row", () => {
    renderWithIntl(<LabelsSection labelRequest={labelRequestFixture()} />);

    // S2's Specimen Label input is named with its sample + column.
    fireEvent.change(
      screen.getByRole("spinbutton", {
        name: "Sample 2 Specimen Label quantity",
      }),
      { target: { value: "4" } },
    );

    // S2 specimen 1 -> 4: total 7 -> 10. (S1 untouched.)
    expect(screen.getByText("Total labels: 10")).toBeInTheDocument();
  });
});

// =========================================================================
// Pure model helpers (API mode).
// =========================================================================
describe("LabelsSection — aggregate helpers", () => {
  test("calculateAggregateTotal sums order + every sample cell", () => {
    const total = calculateAggregateTotal(
      { 1: 2 },
      { S1: { 17: 1, 24: 3 }, S2: { 17: 1 } },
    );
    expect(total).toBe(7);
  });

  test("buildPersistPayload shapes order_cells and per-sample cells", () => {
    const payload = buildPersistPayload(
      [{ preset_id: 1 }],
      [{ preset_id: 17 }],
      [{ sample_id_local: "S1" }],
      { 1: 2 },
      { S1: { 17: 4 } },
    );
    expect(payload).toEqual({
      order_cells: [{ preset_id: 1, qty: 2 }],
      sample_rows: [
        { sample_id_local: "S1", cells: [{ preset_id: 17, qty: 4 }] },
      ],
    });
  });
});

// =========================================================================
// Legacy count-based mode — preserved for the three existing callers
// (SampleType, GenericSampleOrder, SampleBatchEntry). The hardcoded
// applicableLabelTypes lockdown is gone from the model.
// =========================================================================
describe("LabelsSection — legacy count mode", () => {
  test("buildLabelRowsModel builds order + sample rows with a running total", () => {
    const model = buildLabelRowsModel(2, [1, 3]);

    expect(model.orderRow.quantities.order).toBe(2);
    expect(model.sampleRows.map((r) => r.quantities.specimen)).toEqual([1, 3]);
    expect(model.runningTotal).toBe(6);
    // The OGC-284 hardcode is gone: rows no longer carry applicableLabelTypes.
    expect(model.orderRow).not.toHaveProperty("applicableLabelTypes");
    expect(model.sampleRows[0]).not.toHaveProperty("applicableLabelTypes");
  });

  test("buildLabelRowsModel normalizes null and negative quantities to zero", () => {
    const model = buildLabelRowsModel(-4, [2, null, -1]);
    expect(model.orderRow.quantities.order).toBe(0);
    expect(model.sampleRows[1].quantities.specimen).toBe(0);
    expect(model.sampleRows[2].quantities.specimen).toBe(0);
    expect(model.runningTotal).toBe(2);
  });

  test("calculateRunningTotal sums order and sample row totals", () => {
    expect(
      calculateRunningTotal({ rowTotal: 5 }, [
        { rowTotal: 2 },
        { rowTotal: 1 },
      ]),
    ).toBe(8);
  });

  test("renders the legacy NumberInputs and running total when no labelRequest", () => {
    renderWithIntl(
      <LabelsSection orderQuantity={2} specimenQuantities={[1, 3]} />,
    );

    expect(screen.getByLabelText("Order labels")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Specimen labels sample 1"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("Specimen labels sample 2"),
    ).toBeInTheDocument();
    expect(screen.getByText("Running total: 6")).toBeInTheDocument();
  });

  test("legacy onChange emits the order/sample model the callers consume", () => {
    const onChange = vi.fn();
    renderWithIntl(
      <LabelsSection
        orderQuantity={2}
        specimenQuantities={[1]}
        onChange={onChange}
      />,
    );

    fireEvent.change(screen.getByLabelText("Order labels"), {
      target: { value: "3" },
    });

    expect(onChange).toHaveBeenCalled();
    const last = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    expect(last.orderRow.quantities.order).toBe(3);
    expect(last.sampleRows[0].quantities.specimen).toBe(1);
    expect(last.runningTotal).toBe(4);
  });
});
