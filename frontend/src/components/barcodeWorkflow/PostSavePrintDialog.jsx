import React from "react";
import {
  Button,
  InlineLoading,
  Stack,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
  Tile,
} from "@carbon/react";
import { Checkmark, Printer } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import "./PostSavePrintDialog.scss";

// Falls back to the raw type for unknowns so new server-side types still
// render (rather than silently disappearing).
const LABEL_TYPE_MESSAGE_IDS = {
  order: "barcode.label.order",
  specimen: "barcode.label.specimen",
  block: "barcode.label.block",
  slide: "barcode.label.slide",
  freezer: "barcode.label.freezer",
};

const normalize = (label) => {
  if (typeof label === "string") {
    return {
      labelType: label,
      quantity: 1,
      dimensionsMm: "",
      printUrl: "",
      sampleNumber: null,
    };
  }
  return {
    labelType: label?.labelType ?? "",
    quantity: label?.quantity ?? 0,
    dimensionsMm: label?.dimensionsMm ?? "",
    printUrl: label?.printUrl ?? "",
    sampleNumber:
      typeof label?.sampleNumber === "number" ? label.sampleNumber : null,
  };
};

const PostSavePrintDialog = ({
  accessionNumber,
  printableLabelTypes = [],
  onPrint,
  onDone,
  isLoading = false,
}) => {
  const intl = useIntl();

  if (!accessionNumber) return null;

  const labels = printableLabelTypes.map(normalize);

  const formatLabelName = (label) => {
    const messageId = LABEL_TYPE_MESSAGE_IDS[label.labelType];
    const baseName = messageId
      ? intl.formatMessage({ id: messageId, defaultMessage: label.labelType })
      : label.labelType;
    return label.sampleNumber != null
      ? `${baseName} ${label.sampleNumber}`
      : baseName;
  };

  const handlePrint = (label) => {
    if (onPrint) {
      onPrint(label.labelType, label.quantity);
      return;
    }
    if (!label.printUrl) {
      console.warn(
        "PostSavePrintDialog: no printUrl or onPrint handler for",
        label.labelType,
      );
      return;
    }
    const printWindow = window.open(label.printUrl);
    if (!printWindow) {
      console.warn(
        "PostSavePrintDialog: window.open returned null (popup blocked?) for",
        label.printUrl,
      );
    }
  };

  const handleDone = () => {
    if (onDone) {
      onDone();
    }
  };

  const rowKey = (label, idx) =>
    label.sampleNumber != null
      ? `${label.labelType}-${label.sampleNumber}`
      : `${label.labelType}-${idx}`;

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

        {labels.length > 0 && (
          <StructuredListWrapper condensed flush>
            <StructuredListBody>
              {labels.map((label, idx) => (
                <StructuredListRow key={rowKey(label, idx)}>
                  <StructuredListCell>
                    <p className="post-save-dialog__label-name">
                      {formatLabelName(label)}
                    </p>
                    <p className="post-save-dialog__label-qty">
                      {intl.formatMessage({
                        id: "label.quantity",
                        defaultMessage: "Quantity",
                      })}
                      {": "}
                      {label.quantity}
                      {label.dimensionsMm ? ` · ${label.dimensionsMm}` : ""}
                    </p>
                  </StructuredListCell>
                  <StructuredListCell className="post-save-dialog__action-cell">
                    <Button
                      size="sm"
                      renderIcon={Printer}
                      onClick={() => handlePrint(label)}
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

        {/* Done is opt-in: pathology/cytology/batch screens render this dialog
            without an onDone because navigating away would lose context. */}
        {onDone && (
          <div className="post-save-dialog__footer">
            <Button
              kind="secondary"
              renderIcon={Checkmark}
              onClick={handleDone}
            >
              <FormattedMessage
                id={
                  labels.length > 0
                    ? "barcode.print.done"
                    : "barcode.print.skip"
                }
              />
            </Button>
          </div>
        )}
      </Stack>
    </Tile>
  );
};

export default PostSavePrintDialog;
