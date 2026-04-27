import React, { useEffect, useState } from "react";
import { NumberInput, Stack } from "@carbon/react";

const normalizeQuantity = (quantity) => {
  const num = Number(quantity);
  if (
    quantity === null ||
    quantity === undefined ||
    quantity === "" ||
    isNaN(num) ||
    num < 0
  ) {
    return 0;
  }
  return Math.floor(num);
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
    <div data-testid="labels-section-root">
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

export default LabelsSection;
