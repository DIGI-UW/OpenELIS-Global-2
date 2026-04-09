import React from "react";
import { MultiSelect, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

export const syncReagentUsageQuantities = (
  selectedItems,
  currentQuantities = {},
  defaultValue = "1",
) => {
  const nextQuantities = {};
  selectedItems.forEach((item) => {
    const key = String(item.id);
    nextQuantities[key] =
      currentQuantities[key] !== undefined ? currentQuantities[key] : defaultValue;
  });
  return nextQuantities;
};

export const buildSelectedReagentUsages = (
  selectedItems,
  reagentQuantities = {},
) =>
  selectedItems
    .map((item) => {
      const quantityValue = reagentQuantities[String(item.id)];
      const quantityPerSample = Number.parseFloat(quantityValue);
      if (!Number.isFinite(quantityPerSample) || quantityPerSample <= 0) {
        return null;
      }

      return {
        itemId: item.itemId ?? item.id,
        quantityPerSample,
        name: item.name ?? item.label ?? item.text ?? "",
        lotNumber: item.lotNumber ?? "",
        units: item.units ?? "",
      };
    })
    .filter(Boolean);

export const getInvalidReagentUsageItems = (
  selectedItems,
  reagentQuantities = {},
) =>
  selectedItems.filter((item) => {
    const quantityValue = reagentQuantities[String(item.id)];
    const quantityPerSample = Number.parseFloat(quantityValue);
    return !Number.isFinite(quantityPerSample) || quantityPerSample <= 0;
  });

function ReagentUsageSelector({
  reagents,
  selectedIds,
  reagentQuantities,
  sampleCount = 0,
  disabled = false,
  titleText,
  label,
  onSelectionChange,
  onQuantityChange,
}) {
  const intl = useIntl();
  const selectedItems = reagents.filter((item) => selectedIds.includes(item.id));

  return (
    <div>
      <MultiSelect
        id="selectedReagents"
        titleText={titleText}
        label={label}
        items={reagents}
        itemToString={(item) => (item ? item.label || item.name || "" : "")}
        selectedItems={selectedItems}
        onChange={({ selectedItems: nextSelectedItems }) =>
          onSelectionChange(nextSelectedItems)
        }
        disabled={disabled}
      />

      {selectedItems.length > 0 && (
        <div style={{ marginTop: "1rem" }}>
          <p
            style={{
              marginBottom: "0.75rem",
              fontWeight: 600,
            }}
          >
            <FormattedMessage
              id="notebook.reagentUsage.sectionTitle"
              defaultMessage="Reagent usage"
            />
          </p>

          {selectedItems.map((item) => {
            const quantityValue = reagentQuantities[String(item.id)] ?? "";
            const quantityPerSample = Number.parseFloat(quantityValue);
            const totalQuantity =
              Number.isFinite(quantityPerSample) && sampleCount > 0
                ? quantityPerSample * sampleCount
                : null;
            const helperParts = [];
            if (item.lotNumber) {
              helperParts.push(
                intl.formatMessage(
                  {
                    id: "notebook.reagentUsage.lot",
                    defaultMessage: "FEFO lot shown: {lot}",
                  },
                  { lot: item.lotNumber },
                ),
              );
            }
            if (item.currentQuantity !== undefined && item.currentQuantity !== null) {
              helperParts.push(
                intl.formatMessage(
                  {
                    id: "notebook.reagentUsage.available",
                    defaultMessage: "Available: {quantity} {units}",
                  },
                  {
                    quantity: item.currentQuantity,
                    units: item.units || "",
                  },
                ),
              );
            }
            if (totalQuantity !== null) {
              helperParts.push(
                intl.formatMessage(
                  {
                    id: "notebook.reagentUsage.totalDeduction",
                    defaultMessage:
                      "Total deduction for {count} sample(s): {total} {units}",
                  },
                  {
                    count: sampleCount,
                    total: totalQuantity,
                    units: item.units || "",
                  },
                ),
              );
            }

            return (
              <div
                key={item.id}
                style={{
                  marginBottom: "0.75rem",
                  padding: "0.75rem",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <div style={{ marginBottom: "0.5rem", fontWeight: 600 }}>
                  {item.label || item.name}
                </div>
                <TextInput
                  id={`reagent-quantity-${item.id}`}
                  type="number"
                  min="0"
                  step="0.01"
                  labelText={intl.formatMessage({
                    id: "notebook.reagentUsage.quantityPerSample",
                    defaultMessage: "Quantity used per sample",
                  })}
                  value={quantityValue}
                  onChange={(event) =>
                    onQuantityChange(String(item.id), event.target.value)
                  }
                  invalid={
                    quantityValue !== "" &&
                    (!Number.isFinite(quantityPerSample) || quantityPerSample <= 0)
                  }
                  invalidText={intl.formatMessage({
                    id: "notebook.reagentUsage.quantityInvalid",
                    defaultMessage: "Enter a number greater than 0",
                  })}
                  helperText={helperParts.join(" | ")}
                />
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default ReagentUsageSelector;
