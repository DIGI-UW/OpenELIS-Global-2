import React, { useState } from "react";
import {
  Button,
  InlineLoading,
  NumberInput,
  Stack,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
  Tile,
} from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { clampToMax, normalizeQuantity } from "./quantity";
import "./PostSavePrintDialog.scss";

// Normalise one incoming row into the dialog's preset-driven shape.
//
// OGC-285 M6: rows are now per-preset (presetId + preset name + the saved
// quantity that caps the decrease-only input). Older callers may still pass the
// flat {labelType, quantity} shape (e.g. the legacy ExistingOrder reprint list);
// those degrade gracefully — labelName falls back to labelType, savedQty to
// quantity, and presetId stays null so onPrint receives the label name instead.
// Rewrite the `quantity` query param of a print URL to the chosen value.
//
// The legacy LabelMakerServlet URL carries `&quantity=N`; without this, the
// decrease-only NumberInput would be inert for the (currently only) live print
// path — the user lowers the count but the fixed saved quantity still prints.
// Only URLs that already declare a quantity param are rewritten; the snapshot
// reprint endpoint (/api/barcode/print/{id}/{presetId}) has no quantity param
// and renders at the stored qty, so it is left untouched.
const applyQuantityToUrl = (url, quantity) => {
  if (!url || !/[?&]quantity=/.test(url)) {
    return url;
  }
  const safeQty = quantity > 0 ? quantity : 1;
  return url.replace(/([?&]quantity=)[^&]*/, `$1${safeQty}`);
};

const normalizeRow = (row, idx) => {
  if (typeof row === "string") {
    return {
      key: `${row}-${idx}`,
      presetId: null,
      labelName: row,
      sampleNumber: null,
      savedQty: 1,
      dimensionsMm: "",
      printUrl: "",
    };
  }
  const presetId = row?.presetId ?? null;
  const labelName = row?.labelName ?? row?.labelType ?? "";
  const sampleNumber =
    typeof row?.sampleNumber === "number" ? row.sampleNumber : null;
  // savedQty is the ceiling for the decrease-only input. Accept the new
  // savedQty first, then fall back to the legacy quantity field.
  const savedQty = normalizeQuantity(row?.savedQty ?? row?.quantity ?? 0);
  const keyBase = presetId != null ? `preset-${presetId}` : labelName;
  return {
    key:
      sampleNumber != null ? `${keyBase}-${sampleNumber}` : `${keyBase}-${idx}`,
    presetId,
    labelName,
    sampleNumber,
    savedQty,
    dimensionsMm: row?.dimensionsMm ?? "",
    printUrl: row?.printUrl ?? "",
  };
};

/**
 * PostSavePrintDialog (OGC-285 M6, T171).
 *
 * Preset-driven print list shown after an order saves (and reused on the
 * reprint surface). Each row is one persisted label preset: its name, a
 * decrease-only Carbon NumberInput capped at the saved quantity, and a Print
 * button that renders that preset's snapshot PDF
 * (GET /api/barcode/print/{orderId}/{presetId}). A dialog-level
 * "Skip — Print Later" action lets the technician defer printing.
 *
 * The OGC-284 hardcoded label-type lockdown (order/specimen/block/slide/freezer)
 * is gone: rows render whatever presets the backend persisted, by name.
 */
const PostSavePrintDialog = ({
  accessionNumber,
  printableLabelTypes = [],
  onPrint,
  onSkip,
  onPopupBlocked,
  isLoading = false,
}) => {
  const intl = useIntl();

  const rows = printableLabelTypes.map(normalizeRow);

  // Per-row chosen quantity, keyed by row.key, seeded to the saved quantity.
  // Edits clamp to [0, savedQty] so the technician can only ever decrease.
  const [quantities, setQuantities] = useState(() => {
    const seed = {};
    rows.forEach((row) => {
      seed[row.key] = row.savedQty;
    });
    return seed;
  });

  if (!accessionNumber) return null;

  const quantityFor = (row) =>
    quantities[row.key] === undefined ? row.savedQty : quantities[row.key];

  const handleQuantityChange = (row, rawValue) => {
    setQuantities((prev) => ({
      ...prev,
      [row.key]: clampToMax(rawValue, row.savedQty),
    }));
  };

  const labelNameFor = (row) =>
    row.sampleNumber != null
      ? `${row.labelName} ${row.sampleNumber}`
      : row.labelName;

  const handlePrint = (row) => {
    const chosenQty = clampToMax(quantityFor(row), row.savedQty);
    if (onPrint) {
      onPrint(
        row.presetId != null ? row.presetId : row.labelName,
        chosenQty,
        row,
      );
      return;
    }
    if (!row.printUrl) {
      console.warn(
        "PostSavePrintDialog: no printUrl or onPrint handler for",
        row.labelName,
      );
      return;
    }
    // Honour the chosen (decreased) quantity for legacy quantity-bearing URLs.
    const url = applyQuantityToUrl(row.printUrl, chosenQty);
    const printWindow = window.open(url);
    if (!printWindow) {
      console.warn(
        "PostSavePrintDialog: window.open returned null (popup blocked?) for",
        url,
      );
      if (onPopupBlocked) {
        onPopupBlocked(row);
      }
    }
  };

  return (
    <Tile>
      <Stack gap={5}>
        <div>
          <p className="post-save-dialog__caption">
            <FormattedMessage id="barcode.print.dialog.title" />
          </p>
          <Tag type="cool-gray" className="post-save-dialog__accession-tag">
            {accessionNumber}
          </Tag>
        </div>

        {onSkip && (
          <div className="post-save-dialog__skip">
            <Button kind="ghost" size="sm" onClick={onSkip}>
              <FormattedMessage id="barcode.print.later" />
            </Button>
          </div>
        )}

        {rows.length > 0 && (
          <StructuredListWrapper isCondensed isFlush>
            <StructuredListBody>
              {rows.map((row) => (
                <StructuredListRow key={row.key}>
                  <StructuredListCell>
                    <p className="post-save-dialog__label-name">
                      {labelNameFor(row)}
                    </p>
                    {row.dimensionsMm ? (
                      <p className="post-save-dialog__label-qty">
                        {row.dimensionsMm}
                      </p>
                    ) : null}
                  </StructuredListCell>
                  <StructuredListCell className="post-save-dialog__qty-cell">
                    <NumberInput
                      id={`post-save-qty-${row.key}`}
                      label={intl.formatMessage(
                        { id: "barcode.print.dialog.quantityFor" },
                        { label: labelNameFor(row) },
                      )}
                      hideLabel
                      min={0}
                      max={row.savedQty > 0 ? row.savedQty : undefined}
                      value={quantityFor(row)}
                      onChange={(_event, state) => {
                        const raw =
                          state && state.value !== undefined
                            ? state.value
                            : _event?.target?.value;
                        handleQuantityChange(row, raw);
                      }}
                    />
                  </StructuredListCell>
                  <StructuredListCell className="post-save-dialog__action-cell">
                    <Button
                      size="sm"
                      renderIcon={Printer}
                      onClick={() => handlePrint(row)}
                    >
                      <FormattedMessage id="barcode.print.button" />
                    </Button>
                  </StructuredListCell>
                </StructuredListRow>
              ))}
            </StructuredListBody>
          </StructuredListWrapper>
        )}

        {isLoading && <InlineLoading />}
      </Stack>
    </Tile>
  );
};

export default PostSavePrintDialog;
