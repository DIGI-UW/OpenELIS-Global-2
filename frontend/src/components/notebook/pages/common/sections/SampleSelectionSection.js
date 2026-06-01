import React, { useCallback } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Button,
  TextInput,
  NumberInput,
  Tag,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { createEmptyRequestRow } from "../biorepoRequestReferenceHelpers";

/**
 * Section B: Requested sample reference rows (AHRI BR-F-02)
 * Requesters describe samples; Biorepository matches inventory at fulfillment.
 */
function SampleSelectionSection({ selectedSamples, onSamplesChange, readOnly }) {
  const intl = useIntl();

  const handleAddRow = useCallback(() => {
    onSamplesChange([...(selectedSamples || []), createEmptyRequestRow()]);
  }, [selectedSamples, onSamplesChange]);

  const handleRemoveRow = useCallback(
    (rowId) => {
      onSamplesChange((selectedSamples || []).filter((row) => row.id !== rowId));
    },
    [selectedSamples, onSamplesChange],
  );

  const handleFieldChange = useCallback(
    (rowId, field, value) => {
      onSamplesChange(
        (selectedSamples || []).map((row) =>
          row.id === rowId ? { ...row, [field]: value } : row,
        ),
      );
    },
    [selectedSamples, onSamplesChange],
  );

  const headers = [
    {
      key: "requestedAccessionNumber",
      header: intl.formatMessage({
        id: "biorepo.import.searchModal.accessionLabNumber",
        defaultMessage: "Accession / Lab Number",
      }),
    },
    {
      key: "requestedBarcode",
      header: intl.formatMessage({
        id: "biorepo.import.field.batchNo",
        defaultMessage: "Batch No. / Barcode",
      }),
    },
    {
      key: "requestedSampleType",
      header: intl.formatMessage({
        id: "biorepo.import.field.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "requestedOriginLab",
      header: intl.formatMessage({
        id: "biorepo.import.searchModal.originLab",
        defaultMessage: "Origin Lab",
      }),
    },
    {
      key: "requestedProjectId",
      header: intl.formatMessage({
        id: "biorepo.import.searchModal.project",
        defaultMessage: "Project",
      }),
    },
    {
      key: "quantityRequested",
      header: intl.formatMessage({
        id: "biorepo.import.field.quantityRequested",
        defaultMessage: "Qty Requested",
      }),
    },
    {
      key: "unitOfMeasure",
      header: intl.formatMessage({
        id: "biorepo.import.field.unit",
        defaultMessage: "Unit",
      }),
    },
    {
      key: "remark",
      header: intl.formatMessage({
        id: "biorepo.import.field.remark",
        defaultMessage: "Remark",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  const rows = (selectedSamples || []).map((row) => ({
    id: row.id,
    requestedAccessionNumber: row.requestedAccessionNumber || "",
    requestedBarcode: row.requestedBarcode || "",
    requestedSampleType: row.requestedSampleType || "",
    requestedOriginLab: row.requestedOriginLab || "",
    requestedProjectId: row.requestedProjectId || "",
    quantityRequested: row.quantityRequested ?? "",
    unitOfMeasure: row.unitOfMeasure || "",
    remark: row.remark || "",
  }));

  return (
    <div className="biorepo-section" style={{ marginBottom: "2rem" }}>
      <h4 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="biorepo.import.section.samples"
          defaultMessage="Section B: Sample Details"
        />
      </h4>

      <p
        style={{
          fontSize: "0.8125rem",
          color: "#525252",
          marginBottom: "0.75rem",
        }}
      >
        <FormattedMessage
          id="biorepo.import.reference.helper"
          defaultMessage="Describe the sample(s) you need. Exact accession, barcode, or storage location is not required — Biorepository will match inventory during fulfillment."
        />
      </p>

      {!readOnly && (
        <div style={{ marginBottom: "0.75rem" }}>
          <Button kind="secondary" size="sm" renderIcon={Add} onClick={handleAddRow}>
            <FormattedMessage
              id="biorepo.import.reference.addRow"
              defaultMessage="Add Requested Sample"
            />
          </Button>
        </div>
      )}

      {rows.length > 0 ? (
        <DataTable rows={rows} headers={headers} size="sm">
          {({ rows: tableRows, headers: tableHeaders, getTableProps, getHeaderProps, getRowProps }) => (
            <TableContainer>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {tableHeaders.map((header) => (
                      <TableHeader key={header.key} {...getHeaderProps({ header })}>
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tableRows.map((row) => {
                    const source = (selectedSamples || []).find((item) => item.id === row.id);
                    return (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        <TableCell>
                          {readOnly ? (
                            source?.requestedAccessionNumber || "-"
                          ) : (
                            <TextInput
                              id={`accession-${row.id}`}
                              labelText=""
                              hideLabel
                              size="sm"
                              value={source?.requestedAccessionNumber || ""}
                              onChange={(e) =>
                                handleFieldChange(row.id, "requestedAccessionNumber", e.target.value)
                              }
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {readOnly ? (
                            source?.requestedBarcode || "-"
                          ) : (
                            <TextInput
                              id={`barcode-${row.id}`}
                              labelText=""
                              hideLabel
                              size="sm"
                              value={source?.requestedBarcode || ""}
                              onChange={(e) =>
                                handleFieldChange(row.id, "requestedBarcode", e.target.value)
                              }
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {readOnly ? (
                            source?.requestedSampleType || "-"
                          ) : (
                            <TextInput
                              id={`sampleType-${row.id}`}
                              labelText=""
                              hideLabel
                              size="sm"
                              value={source?.requestedSampleType || ""}
                              onChange={(e) =>
                                handleFieldChange(row.id, "requestedSampleType", e.target.value)
                              }
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {readOnly ? (
                            source?.requestedOriginLab || "-"
                          ) : (
                            <TextInput
                              id={`originLab-${row.id}`}
                              labelText=""
                              hideLabel
                              size="sm"
                              value={source?.requestedOriginLab || ""}
                              onChange={(e) =>
                                handleFieldChange(row.id, "requestedOriginLab", e.target.value)
                              }
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {readOnly ? (
                            source?.requestedProjectId || "-"
                          ) : (
                            <TextInput
                              id={`project-${row.id}`}
                              labelText=""
                              hideLabel
                              size="sm"
                              value={source?.requestedProjectId || ""}
                              onChange={(e) =>
                                handleFieldChange(row.id, "requestedProjectId", e.target.value)
                              }
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {readOnly ? (
                            source?.quantityRequested ?? "-"
                          ) : (
                            <NumberInput
                              id={`quantity-${row.id}`}
                              label=""
                              hideLabel
                              size="sm"
                              min={0}
                              value={source?.quantityRequested ?? ""}
                              onChange={(e, { value }) =>
                                handleFieldChange(row.id, "quantityRequested", value)
                              }
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {readOnly ? (
                            source?.unitOfMeasure || "-"
                          ) : (
                            <TextInput
                              id={`unit-${row.id}`}
                              labelText=""
                              hideLabel
                              size="sm"
                              value={source?.unitOfMeasure || ""}
                              onChange={(e) =>
                                handleFieldChange(row.id, "unitOfMeasure", e.target.value)
                              }
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {readOnly ? (
                            source?.remark || ""
                          ) : (
                            <TextInput
                              id={`remark-${row.id}`}
                              labelText=""
                              hideLabel
                              size="sm"
                              value={source?.remark || ""}
                              onChange={(e) =>
                                handleFieldChange(row.id, "remark", e.target.value)
                              }
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {!readOnly && (
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={TrashCan}
                              iconDescription="Remove"
                              hasIconOnly
                              onClick={() => handleRemoveRow(row.id)}
                            />
                          )}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      ) : (
        <Tag type="cool-gray">
          <FormattedMessage
            id="biorepo.import.noSamples"
            defaultMessage="No samples selected"
          />
        </Tag>
      )}
    </div>
  );
}

export default SampleSelectionSection;
