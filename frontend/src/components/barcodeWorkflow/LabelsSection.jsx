import React, { useEffect, useMemo, useState } from "react";
import {
  NumberInput,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Tooltip,
} from "@carbon/react";
import { Locked } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { clampToMax, normalizeQuantity } from "./quantity";
import "./LabelsSection.scss";

// ===========================================================================
// Legacy order/specimen-count model (OGC-284).
//
// Preserved verbatim in shape EXCEPT the hardcoded applicableLabelTypes arrays
// — those locked the section to exactly {order, specimen} and are the OGC-284
// gap this rewrite closes. The three existing callers (SampleType,
// GenericSampleOrder, SampleBatchEntry) read only orderRow.quantities.order and
// sampleRows[0].quantities.specimen from onChange, so dropping the field is
// safe for them.
// ===========================================================================
const createOrderRow = (orderQuantity) => {
  const normalizedOrderQuantity = normalizeQuantity(orderQuantity);
  return {
    rowType: "order",
    rowId: "order-row",
    sampleRef: null,
    quantities: { order: normalizedOrderQuantity },
    rowTotal: normalizedOrderQuantity,
  };
};

const createSampleRows = (specimenQuantities = []) => {
  return specimenQuantities.map((quantity, index) => {
    const normalizedQuantity = normalizeQuantity(quantity);
    return {
      rowType: "sample",
      rowId: `sample-row-${index + 1}`,
      sampleRef: `sample-${index + 1}`,
      quantities: { specimen: normalizedQuantity },
      rowTotal: normalizedQuantity,
    };
  });
};

export const calculateRunningTotal = (orderRow, sampleRows) => {
  const orderTotal = normalizeQuantity(orderRow?.rowTotal);
  const sampleTotal = (sampleRows || []).reduce(
    (total, row) => total + normalizeQuantity(row?.rowTotal),
    0,
  );
  return orderTotal + sampleTotal;
};

export const buildLabelRowsModel = (orderQuantity, specimenQuantities) => {
  const orderRow = createOrderRow(orderQuantity);
  const sampleRows = createSampleRows(specimenQuantities);
  return {
    orderRow,
    sampleRows,
    runningTotal: calculateRunningTotal(orderRow, sampleRows),
  };
};

// ===========================================================================
// API-driven aggregation model (OGC-285 M5).
//
// Built from the POST /api/orderEntry/labelRequest response:
//   { order_columns, sample_columns, order_row, sample_rows }
// Each cell carries default/max/locked/source(+source_test_*). The component
// holds a 2D quantity map keyed by preset id so edits are O(1) and the running
// total recomputes from the live map (never from stale cell defaults).
// ===========================================================================

// preset id -> chosen qty, seeded from each cell's resolved default (clamped).
const seedOrderQuantities = (orderRow) => {
  const next = {};
  (orderRow?.cells || []).forEach((cell) => {
    next[cell.preset_id] = clampToMax(cell.default, cell.max);
  });
  return next;
};

// sample_id_local -> { preset id -> chosen qty }, seeded from cell defaults.
const seedSampleQuantities = (sampleRows) => {
  const next = {};
  (sampleRows || []).forEach((row) => {
    const cellMap = {};
    (row.cells || []).forEach((cell) => {
      cellMap[cell.preset_id] = clampToMax(cell.default, cell.max);
    });
    next[row.sample_id_local] = cellMap;
  });
  return next;
};

// Sum every chosen quantity across the order row and all sample rows.
export const calculateAggregateTotal = (orderQuantities, sampleQuantities) => {
  let total = 0;
  Object.values(orderQuantities || {}).forEach((qty) => {
    total += normalizeQuantity(qty);
  });
  Object.values(sampleQuantities || {}).forEach((cellMap) => {
    Object.values(cellMap || {}).forEach((qty) => {
      total += normalizeQuantity(qty);
    });
  });
  return total;
};

// Shape the chosen quantities into the OrderLabelPersistRequest contract
// (order_cells + sample_rows[].cells), so the parent can round-trip edits to
// the save endpoint when the M5b live hook lands.
export const buildPersistPayload = (
  orderColumns,
  sampleColumns,
  sampleRows,
  orderQuantities,
  sampleQuantities,
) => ({
  order_cells: (orderColumns || []).map((col) => ({
    preset_id: col.preset_id,
    qty: normalizeQuantity(orderQuantities?.[col.preset_id]),
  })),
  sample_rows: (sampleRows || []).map((row) => ({
    sample_id_local: row.sample_id_local,
    cells: (sampleColumns || []).map((col) => ({
      preset_id: col.preset_id,
      qty: normalizeQuantity(
        sampleQuantities?.[row.sample_id_local]?.[col.preset_id],
      ),
    })),
  })),
});

// Source tag i18n + colour for an aggregated cell. Falls back to the raw
// source string so an unknown future source still renders a chip.
const SOURCE_TAG = {
  test: { id: "orderEntry.labels.source.test", type: "teal" },
  preset_default: {
    id: "orderEntry.labels.source.presetDefault",
    type: "gray",
  },
};

// ---------------------------------------------------------------------------
// One editable quantity cell: NumberInput clamped to max, a source Tag chip
// below it, and a Lock icon + tooltip when the cell is locked. A locked cell
// renders the (read-only) default as plain text instead of an input.
// ---------------------------------------------------------------------------
const LabelQuantityCell = ({ idPrefix, cell, value, inputLabel, onChange }) => {
  const intl = useIntl();
  if (!cell) {
    return <TableCell />;
  }

  const sourceMeta = SOURCE_TAG[cell.source];
  const sourceLabel = sourceMeta
    ? intl.formatMessage({ id: sourceMeta.id })
    : cell.source;
  const sourceTitle =
    cell.source === "test" && cell.source_test_name
      ? `${sourceLabel}: ${cell.source_test_name}`
      : sourceLabel;

  // Each input's accessible name combines the row + column so a screen-reader
  // user can tell which preset/sample an input drives (the visual label is
  // hidden because the column header already shows it). A locked cell's icon
  // button gets the same distinguishable name plus the locked phrasing.
  const lockedLabel = intl.formatMessage(
    { id: "orderEntry.labels.locked.tooltipFor" },
    { label: inputLabel },
  );

  return (
    <TableCell>
      <div className="labels-section__cell">
        {cell.locked ? (
          <div className="labels-section__locked">
            <span className="labels-section__locked-value">{value}</span>
            <Tooltip align="top" label={lockedLabel}>
              <button
                type="button"
                className="labels-section__lock-trigger"
                aria-label={lockedLabel}
              >
                <Locked size={16} className="labels-section__lock-icon" />
              </button>
            </Tooltip>
          </div>
        ) : (
          <NumberInput
            id={`${idPrefix}-${cell.preset_id}`}
            label={inputLabel}
            hideLabel
            min={0}
            max={cell.max > 0 ? cell.max : undefined}
            value={value}
            onChange={(_event, state) => {
              const raw =
                state && state.value !== undefined
                  ? state.value
                  : _event?.target?.value;
              onChange(clampToMax(raw, cell.max));
            }}
          />
        )}
        {sourceMeta || cell.source ? (
          <Tag
            type={sourceMeta ? sourceMeta.type : "gray"}
            size="sm"
            className="labels-section__source-tag"
            title={sourceTitle}
          >
            {sourceTitle}
          </Tag>
        ) : null}
      </div>
    </TableCell>
  );
};

// ---------------------------------------------------------------------------
// A generic dynamic-column label table (used for both the order table and the
// sample table). `rowHeaderRenderer` produces the left-most row-header cell.
// ---------------------------------------------------------------------------
const LabelTable = ({
  titleId,
  rowHeaderLabelId,
  columns,
  rows,
  idPrefix,
  rowHeaderRenderer,
  inputLabelFor,
  quantityFor,
  onCellChange,
}) => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: titleId });

  return (
    <TableContainer title={title} className="labels-section__table">
      <Table aria-label={title} size="sm" useZebraStyles>
        <TableHead>
          <TableRow>
            <TableHeader scope="col">
              <FormattedMessage id={rowHeaderLabelId} />
            </TableHeader>
            {columns.map((col) => (
              <TableHeader key={col.preset_id} scope="col">
                {col.name}
              </TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => {
            const cellByPreset = {};
            (row.cells || []).forEach((cell) => {
              cellByPreset[cell.preset_id] = cell;
            });
            return (
              <TableRow key={row.key}>
                <TableHeader scope="row">{rowHeaderRenderer(row)}</TableHeader>
                {columns.map((col) => (
                  <LabelQuantityCell
                    key={col.preset_id}
                    idPrefix={`${idPrefix}-${row.key}`}
                    cell={cellByPreset[col.preset_id]}
                    value={quantityFor(row, col.preset_id)}
                    inputLabel={inputLabelFor(row, col)}
                    onChange={(nextValue) =>
                      onCellChange(row, col.preset_id, nextValue)
                    }
                  />
                ))}
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

// ---------------------------------------------------------------------------
// API-driven two-table view.
// ---------------------------------------------------------------------------
const ApiLabelsSection = ({ labelRequest, onChange, sampleLabelFor }) => {
  const intl = useIntl();
  const orderColumns = labelRequest.order_columns || [];
  const sampleColumns = labelRequest.sample_columns || [];
  const orderRow = labelRequest.order_row || { cells: [] };
  const sampleRows = labelRequest.sample_rows || [];

  const [orderQuantities, setOrderQuantities] = useState(() =>
    seedOrderQuantities(orderRow),
  );
  const [sampleQuantities, setSampleQuantities] = useState(() =>
    seedSampleQuantities(sampleRows),
  );

  // Re-seed whenever the aggregation response changes (new tests / samples).
  useEffect(() => {
    setOrderQuantities(seedOrderQuantities(orderRow));
    setSampleQuantities(seedSampleQuantities(sampleRows));
  }, [labelRequest]);

  const total = useMemo(
    () => calculateAggregateTotal(orderQuantities, sampleQuantities),
    [orderQuantities, sampleQuantities],
  );

  const emitChange = (nextOrder, nextSample) => {
    if (!onChange) {
      return;
    }
    onChange({
      orderQuantities: nextOrder,
      sampleQuantities: nextSample,
      runningTotal: calculateAggregateTotal(nextOrder, nextSample),
      persistPayload: buildPersistPayload(
        orderColumns,
        sampleColumns,
        sampleRows,
        nextOrder,
        nextSample,
      ),
    });
  };

  const handleOrderChange = (_row, presetId, nextValue) => {
    const next = { ...orderQuantities, [presetId]: nextValue };
    setOrderQuantities(next);
    emitChange(next, sampleQuantities);
  };

  const handleSampleChange = (row, presetId, nextValue) => {
    const localId = row.sampleIdLocal;
    const rowMap = { ...(sampleQuantities[localId] || {}) };
    rowMap[presetId] = nextValue;
    const next = { ...sampleQuantities, [localId]: rowMap };
    setSampleQuantities(next);
    emitChange(orderQuantities, next);
  };

  // Adapt the order_row (a single cells[] object) into the LabelTable row shape.
  const orderTableRows = [{ key: "order", cells: orderRow.cells || [] }];
  const sampleTableRows = sampleRows.map((row, index) => ({
    key: row.sample_id_local,
    sampleIdLocal: row.sample_id_local,
    sampleNumber: index + 1,
    cells: row.cells || [],
  }));

  return (
    <div className="labels-section labels-section--api">
      <Stack gap={6}>
        <LabelTable
          titleId="orderEntry.labels.orderTable.title"
          rowHeaderLabelId="orderEntry.labels.col.row"
          columns={orderColumns}
          rows={orderTableRows}
          idPrefix="order-label"
          rowHeaderRenderer={() => (
            <FormattedMessage id="orderEntry.labels.orderRow.header" />
          )}
          inputLabelFor={(_row, col) => col.name}
          quantityFor={(_row, presetId) => orderQuantities[presetId] ?? 0}
          onCellChange={handleOrderChange}
        />

        <LabelTable
          titleId="orderEntry.labels.sampleTable.title"
          rowHeaderLabelId="orderEntry.labels.col.sample"
          columns={sampleColumns}
          rows={sampleTableRows}
          idPrefix="sample-label"
          rowHeaderRenderer={(row) => sampleLabelFor(row)}
          inputLabelFor={(row, col) =>
            intl.formatMessage(
              { id: "orderEntry.labels.input.sample" },
              { number: row.sampleNumber, column: col.name },
            )
          }
          quantityFor={(row, presetId) =>
            sampleQuantities[row.sampleIdLocal]?.[presetId] ?? 0
          }
          onCellChange={handleSampleChange}
        />

        <p className="labels-section__total" role="status">
          <FormattedMessage id="orderEntry.labels.total" values={{ total }} />
        </p>
      </Stack>
    </div>
  );
};

// ---------------------------------------------------------------------------
// Legacy count-based view (OGC-284) — preserved for the three existing callers.
// ---------------------------------------------------------------------------
const LegacyLabelsSection = ({
  orderQuantity,
  specimenQuantities,
  onChange,
  orderLabelText,
  specimenLabelFormatter,
  runningTotalLabel,
}) => {
  const [model, setModel] = useState(() =>
    buildLabelRowsModel(orderQuantity, specimenQuantities),
  );

  useEffect(() => {
    setModel(buildLabelRowsModel(orderQuantity, specimenQuantities));
  }, [orderQuantity, JSON.stringify(specimenQuantities)]);

  const updateModel = (nextModel) => {
    setModel(nextModel);
    if (onChange) {
      onChange(nextModel);
    }
  };

  const updateOrderQuantity = (nextValue) => {
    const normalizedOrderQuantity = normalizeQuantity(nextValue);
    const nextOrderRow = {
      ...model.orderRow,
      quantities: {
        ...model.orderRow.quantities,
        order: normalizedOrderQuantity,
      },
      rowTotal: normalizedOrderQuantity,
    };
    const nextModel = {
      ...model,
      orderRow: nextOrderRow,
      runningTotal: calculateRunningTotal(nextOrderRow, model.sampleRows),
    };
    updateModel(nextModel);
  };

  const updateSpecimenQuantity = (index, nextValue) => {
    const normalizedSpecimenQuantity = normalizeQuantity(nextValue);
    const nextSampleRows = model.sampleRows.map((row, rowIndex) => {
      if (rowIndex !== index) {
        return row;
      }
      return {
        ...row,
        quantities: { ...row.quantities, specimen: normalizedSpecimenQuantity },
        rowTotal: normalizedSpecimenQuantity,
      };
    });

    const nextModel = {
      ...model,
      sampleRows: nextSampleRows,
      runningTotal: calculateRunningTotal(model.orderRow, nextSampleRows),
    };
    updateModel(nextModel);
  };

  return (
    <div className="labels-section labels-section--legacy">
      <Stack gap={5}>
        <NumberInput
          id="labels-order"
          label={orderLabelText}
          min={0}
          value={model.orderRow.quantities.order}
          onChange={(event, { value, direction }) => {
            const current = model.orderRow.quantities.order;
            const next =
              direction === "up"
                ? current + 1
                : direction === "down"
                  ? Math.max(0, current - 1)
                  : normalizeQuantity(value);
            updateOrderQuantity(next);
          }}
        />
        {model.sampleRows.map((sampleRow, index) => (
          <NumberInput
            key={sampleRow.rowId}
            id={sampleRow.rowId}
            label={specimenLabelFormatter(index + 1)}
            min={0}
            value={sampleRow.quantities.specimen}
            onChange={(event, { value, direction }) => {
              const current = sampleRow.quantities.specimen;
              const next =
                direction === "up"
                  ? current + 1
                  : direction === "down"
                    ? Math.max(0, current - 1)
                    : normalizeQuantity(value);
              updateSpecimenQuantity(index, next);
            }}
          />
        ))}
        <p>{`${runningTotalLabel}: ${model.runningTotal}`}</p>
      </Stack>
    </div>
  );
};

/**
 * LabelsSection (OGC-285 M5, T143).
 *
 * Dual-mode label-quantity editor for Order Entry.
 *
 * - API-driven mode (preferred): pass `labelRequest`, the response of
 *   POST /api/orderEntry/labelRequest. Renders two dynamic-column Carbon tables
 *   (Order Labels + Sample Labels) whose columns are the applicable presets, a
 *   source Tag chip under each cell, a Lock icon + tooltip on locked cells, and
 *   a live total row. `onChange` fires with
 *   { orderQuantities, sampleQuantities, runningTotal, persistPayload }.
 *
 * - Legacy mode: omit `labelRequest` and pass `orderQuantity` /
 *   `specimenQuantities` (OGC-284). Preserved for the existing SampleType,
 *   GenericSampleOrder, and SampleBatchEntry callers — `onChange` fires with the
 *   { orderRow, sampleRows, runningTotal } model those callers consume.
 *
 * The hardcoded order/specimen label-type lockdown (OGC-284 gap) is gone: the
 * API mode is fully driven by configurable preset columns.
 */
const LabelsSection = ({
  labelRequest = undefined,
  orderQuantity = 0,
  specimenQuantities = [],
  onChange = undefined,
  orderLabelText = "Order labels",
  specimenLabelFormatter = (sampleNumber) =>
    `Specimen labels sample ${sampleNumber}`,
  runningTotalLabel = "Running total",
  sampleLabelFormatter = undefined,
}) => {
  const intl = useIntl();

  if (labelRequest) {
    const sampleLabelFor = (row) =>
      sampleLabelFormatter
        ? sampleLabelFormatter(row)
        : intl.formatMessage(
            { id: "orderEntry.labels.sampleRow.header" },
            { number: row.sampleNumber, id: row.sampleIdLocal },
          );
    return (
      <ApiLabelsSection
        labelRequest={labelRequest}
        onChange={onChange}
        sampleLabelFor={sampleLabelFor}
      />
    );
  }

  return (
    <LegacyLabelsSection
      orderQuantity={orderQuantity}
      specimenQuantities={specimenQuantities}
      onChange={onChange}
      orderLabelText={orderLabelText}
      specimenLabelFormatter={specimenLabelFormatter}
      runningTotalLabel={runningTotalLabel}
    />
  );
};

export default LabelsSection;
