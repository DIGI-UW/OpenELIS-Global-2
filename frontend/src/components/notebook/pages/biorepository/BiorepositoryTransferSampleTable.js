import React from "react";
import PropTypes from "prop-types";
import {
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TextInput,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { parseQuantityValue } from "./biorepositoryQuantityHelpers";

const COMMON_CONDITIONS = [
  "Good",
  "Frozen",
  "Thawed",
  "Partially Thawed",
  "Damaged",
];
const COMMON_PRESERVATIVES = [
  "None",
  "Formalin",
  "PBS",
  "RNAlater",
  "DMSO",
  "EDTA",
  "Glycerol",
];

export function buildDefaultTransferItemMetadata(sample) {
  const sampleItemId = sample.sampleItemId || sample.id;
  return {
    collectionDate: sample.collectionDate || sample.data?.collectionDate || "",
    quantity:
      sample.quantity ??
      sample.volume ??
      sample.data?.sampleVolume ??
      sample.data?.volume ??
      "",
    unitOfMeasure:
      sample.unitOfMeasure ||
      sample.unitOfMeasureName ||
      sample.data?.unitOfMeasure ||
      "",
    sampleCondition:
      sample.sampleCondition || sample.data?.sampleCondition || "",
    preservationMedium:
      sample.preservative ||
      sample.preservationMedium ||
      sample.data?.preservative ||
      sample.data?.preservationMedium ||
      "",
  };
}

export function buildTransferSamplesForValidation(samples, itemMetadata) {
  return (samples || []).map((sample) => {
    const sampleItemId = sample.sampleItemId || sample.id;
    const metadata = itemMetadata?.[sampleItemId] || {};
    return {
      ...sample,
      sampleItemId,
      collectionDate: metadata.collectionDate ?? sample.collectionDate,
      quantity: metadata.quantity ?? sample.quantity,
      unitOfMeasure: metadata.unitOfMeasure ?? sample.unitOfMeasure,
      sampleCondition: metadata.sampleCondition,
      preservationMedium: metadata.preservationMedium,
    };
  });
}

function BiorepositoryTransferSampleTable({
  samples,
  itemMetadata,
  onItemMetadataChange,
}) {
  if (!samples || samples.length === 0) {
    return null;
  }

  const handleFieldChange = (sampleItemId, field, value) => {
    onItemMetadataChange({
      ...itemMetadata,
      [sampleItemId]: {
        ...(itemMetadata?.[sampleItemId] || {}),
        [field]: value,
      },
    });
  };

  return (
    <div style={{ marginBottom: "1rem" }}>
      <h5 style={{ marginBottom: "0.75rem", fontWeight: 600 }}>
        <FormattedMessage
          id="biorepository.transfer.sampleMetadata"
          defaultMessage="Per-sample transfer metadata"
        />
      </h5>
      <TableContainer>
        <Table size="sm">
          <TableHead>
            <TableRow>
              <TableHeader>Sample ID</TableHeader>
              <TableHeader>Type</TableHeader>
              <TableHeader>Collection date</TableHeader>
              <TableHeader>Volume</TableHeader>
              <TableHeader>Condition *</TableHeader>
              <TableHeader>Preservative/Medium *</TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {samples.map((sample) => {
              const sampleItemId = sample.sampleItemId || sample.id;
              const metadata = itemMetadata?.[sampleItemId] || {};
              const sampleIdLabel =
                sample.externalId ||
                sample.sampleExternalId ||
                sample.accessionNumber ||
                sample.labNo ||
                sampleItemId;
              const quantity =
                metadata.quantity ??
                sample.quantity ??
                sample.volume ??
                sample.data?.volume ??
                "";
              const unit =
                metadata.unitOfMeasure ??
                sample.unitOfMeasure ??
                sample.unitOfMeasureName ??
                sample.data?.unitOfMeasure ??
                "";

              return (
                <TableRow key={sampleItemId}>
                  <TableCell>{sampleIdLabel}</TableCell>
                  <TableCell>
                    {sample.sampleType ||
                      sample.typeOfSample ||
                      sample.type ||
                      "-"}
                  </TableCell>
                  <TableCell>
                    <TextInput
                      id={`transfer-collection-date-${sampleItemId}`}
                      labelText=""
                      hideLabel
                      size="sm"
                      type="date"
                      value={metadata.collectionDate || ""}
                      onChange={(e) =>
                        handleFieldChange(
                          sampleItemId,
                          "collectionDate",
                          e.target.value,
                        )
                      }
                    />
                  </TableCell>
                  <TableCell>
                    <div style={{ display: "flex", gap: "0.5rem" }}>
                      <TextInput
                        id={`transfer-quantity-${sampleItemId}`}
                        labelText=""
                        hideLabel
                        size="sm"
                        value={quantity}
                        invalid={
                          quantity !== "" &&
                          parseQuantityValue(quantity) === null
                        }
                        invalidText="Enter a valid quantity"
                        onChange={(e) =>
                          handleFieldChange(
                            sampleItemId,
                            "quantity",
                            e.target.value,
                          )
                        }
                        placeholder="e.g. 2"
                      />
                      <TextInput
                        id={`transfer-unit-${sampleItemId}`}
                        labelText=""
                        hideLabel
                        size="sm"
                        value={unit}
                        onChange={(e) =>
                          handleFieldChange(
                            sampleItemId,
                            "unitOfMeasure",
                            e.target.value,
                          )
                        }
                        placeholder="Unit"
                      />
                    </div>
                  </TableCell>
                  <TableCell>
                    <TextInput
                      id={`transfer-condition-${sampleItemId}`}
                      labelText=""
                      hideLabel
                      size="sm"
                      list={`transfer-condition-options-${sampleItemId}`}
                      value={metadata.sampleCondition || ""}
                      onChange={(e) =>
                        handleFieldChange(
                          sampleItemId,
                          "sampleCondition",
                          e.target.value,
                        )
                      }
                      placeholder="e.g. Good"
                    />
                    <datalist id={`transfer-condition-options-${sampleItemId}`}>
                      {COMMON_CONDITIONS.map((value) => (
                        <option key={value} value={value} />
                      ))}
                    </datalist>
                  </TableCell>
                  <TableCell>
                    <TextInput
                      id={`transfer-preservative-${sampleItemId}`}
                      labelText=""
                      hideLabel
                      size="sm"
                      list={`transfer-preservative-options-${sampleItemId}`}
                      value={metadata.preservationMedium || ""}
                      onChange={(e) =>
                        handleFieldChange(
                          sampleItemId,
                          "preservationMedium",
                          e.target.value,
                        )
                      }
                      placeholder="e.g. RNAlater"
                    />
                    <datalist
                      id={`transfer-preservative-options-${sampleItemId}`}
                    >
                      {COMMON_PRESERVATIVES.map((value) => (
                        <option key={value} value={value} />
                      ))}
                    </datalist>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </div>
  );
}

BiorepositoryTransferSampleTable.propTypes = {
  samples: PropTypes.arrayOf(PropTypes.object),
  itemMetadata: PropTypes.object,
  onItemMetadataChange: PropTypes.func.isRequired,
};

export default BiorepositoryTransferSampleTable;
