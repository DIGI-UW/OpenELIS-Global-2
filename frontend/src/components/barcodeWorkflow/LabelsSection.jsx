import React, { useState } from "react";
import { NumberInput, Stack } from "@carbon/react";

const normalizeQuantity = (quantity) => {
  if (quantity === null || quantity === undefined || Number(quantity) < 0) {
    return 0;
  }
  return Number(quantity);
};

const createOrderRow = (orderQuantity) => {
  const normalizedOrderQuantity = normalizeQuantity(orderQuantity);
  return {
    rowType: "order",
    rowId: "order-row",
    sampleRef: null,
    applicableLabelTypes: ["order"],
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
      applicableLabelTypes: ["specimen"],
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

const LabelsSection = ({
  orderQuantity = 0,
  specimenQuantities = [],
  onChange = undefined,
  orderLabelText = "Order labels",
  specimenLabelFormatter = (sampleNumber) =>
    `Specimen labels sample ${sampleNumber}`,
  runningTotalLabel = "Running total",
}) => {
  const [model, setModel] = useState(() =>
    buildLabelRowsModel(orderQuantity, specimenQuantities),
  );
  const [orderError, setOrderError] = useState("");
  const [specimenErrors, setSpecimenErrors] = useState([]);

  // NOTE: No useEffect to sync props → internal state after mount.
  // LabelsSection is intentionally "init-only controlled": props set the
  // initial model once (via useState lazy initialiser above), then the
  // component owns its state. Feeding updated quantities back down from the
  // parent's onChange handler would create a bidirectional sync loop because
  // every user interaction triggers onChange → parent setState → new prop →
  // useEffect → setModel → onChange → ... (infinite re-renders).
  // If a parent needs to fully reset the widget, change its `key` prop.

  const updateModel = (nextModel) => {
    setModel(nextModel);
    if (onChange) {
      onChange(nextModel);
    }
  };

  const updateOrderQuantity = (nextValue) => {
    const parsed = parseInt(nextValue, 10);
    if (!nextValue && nextValue !== 0) {
      setOrderError("Required");
      return;
    }
    if (isNaN(parsed) || parsed < 1) {
      setOrderError("Must be at least 1");
      return;
    }
    if (parsed > 15) {
      setOrderError("Max 15 per print");
      return;
    }
    setOrderError("");
    const normalizedOrderQuantity = parsed;
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
    const parsed = parseInt(nextValue, 10);
    const nextErrors = [...specimenErrors];
    if (!nextValue && nextValue !== 0) {
      nextErrors[index] = "Required";
      setSpecimenErrors(nextErrors);
      return;
    }
    if (isNaN(parsed) || parsed < 1) {
      nextErrors[index] = "Must be at least 1";
      setSpecimenErrors(nextErrors);
      return;
    }
    if (parsed > 15) {
      nextErrors[index] = "Max 15 per print";
      setSpecimenErrors(nextErrors);
      return;
    }
    nextErrors[index] = "";
    setSpecimenErrors(nextErrors);
    const normalizedSpecimenQuantity = parsed;
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
    <div data-testid="labels-section-root">
      <Stack gap={5}>
        <NumberInput
          id="labels-order"
          label={orderLabelText}
          min={1}
          max={15}
          hideSteppers
          value={model.orderRow.quantities.order}
          invalid={!!orderError}
          invalidText={orderError}
          onChange={(event, { value }) => updateOrderQuantity(value)}
        />
        {model.sampleRows.map((sampleRow, index) => (
          <NumberInput
            key={sampleRow.rowId}
            id={sampleRow.rowId}
            label={specimenLabelFormatter(index + 1)}
            min={1}
            max={15}
            hideSteppers
            value={sampleRow.quantities.specimen}
            invalid={!!specimenErrors[index]}
            invalidText={specimenErrors[index] || ""}
            onChange={(event, { value }) =>
              updateSpecimenQuantity(index, value)
            }
          />
        ))}
        <p>{`${runningTotalLabel}: ${model.runningTotal}`}</p>
      </Stack>
    </div>
  );
};

export default LabelsSection;
