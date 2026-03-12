import React from "react";

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

const LabelsSection = () => {
  return <div data-testid="labels-section-root" />;
};

export default LabelsSection;
