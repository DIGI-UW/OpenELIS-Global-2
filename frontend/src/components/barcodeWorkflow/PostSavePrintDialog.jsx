import React from "react";
import { Button, InlineLoading, Stack, Tile } from "@carbon/react";
import { useIntl } from "react-intl";

const normalizePrintableLabel = (printableLabel) => {
  if (typeof printableLabel === "string") {
    return {
      labelType: printableLabel,
      quantity: 1,
      dimensionsMm: "",
      printUrl: "",
    };
  }

  return {
    labelType: printableLabel?.labelType || "",
    quantity: printableLabel?.quantity ?? 0,
    dimensionsMm: printableLabel?.dimensionsMm || "",
    printUrl: printableLabel?.printUrl || "",
  };
};

const PostSavePrintDialog = ({
  accessionNumber,
  printableLabelTypes = [],
  onPrint = undefined,
  onDone = undefined,
  isLoading = false,
}) => {
  const intl = useIntl();

  if (!accessionNumber) {
    return null;
  }

  const printableLabels = printableLabelTypes.map(normalizePrintableLabel);

  const handlePrint = (printableLabel) => {
    if (onPrint) {
      onPrint(printableLabel.labelType);
      return;
    }

    if (printableLabel.printUrl) {
      window.open(printableLabel.printUrl);
    }
  };

  return (
    <Tile>
      <Stack gap={5}>
        <h4>{`${intl.formatMessage({ id: "barcode.print.dialog.title" })}: ${accessionNumber}`}</h4>
        {printableLabels.map((printableLabel) => (
          <div key={printableLabel.labelType}>
            <p>{printableLabel.labelType}</p>
            <p>
              {intl.formatMessage(
                { id: "barcode.print.qty" },
                { quantity: printableLabel.quantity },
              )}
            </p>
            {printableLabel.dimensionsMm && (
              <p>{printableLabel.dimensionsMm}</p>
            )}
            <Button onClick={() => handlePrint(printableLabel)}>
              {intl.formatMessage({ id: "barcode.print.button" })}
            </Button>
          </div>
        ))}
        {isLoading && <InlineLoading />}
        <Button kind="secondary" onClick={onDone}>
          {intl.formatMessage({
            id:
              printableLabels.length > 0
                ? "barcode.print.done"
                : "barcode.print.skip",
          })}
        </Button>
      </Stack>
    </Tile>
  );
};

export default PostSavePrintDialog;
