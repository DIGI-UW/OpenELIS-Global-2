import React, { useState, useCallback } from "react";
import {
  FileUploader,
  Button,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  InlineNotification,
  Loading,
  Grid,
  Column,
  Tag,
} from "@carbon/react";
import { DocumentImport, Checkmark, Warning } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";

/**
 * ManifestUpload - CSV manifest upload and preview for bulk sample import
 * Part of Sub-stage 1b of the Biorepository Intake workflow
 *
 * Expected CSV format:
 * externalId,sampleType,projectId,collectionDate,biosafetyLevel,consentId,ethicsApprovalRef,mtaReference
 *
 * @param {Object} props
 * @param {number} props.shipmentId - The shipment ID to associate samples with
 * @param {Function} props.onImportComplete - Callback when import is complete
 * @param {Function} props.onCancel - Callback to cancel
 */
function ManifestUpload({ shipmentId, onImportComplete, onCancel }) {
  const intl = useIntl();

  const [file, setFile] = useState(null);
  const [parsedData, setParsedData] = useState([]);
  const [validationErrors, setValidationErrors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [importStatus, setImportStatus] = useState(null); // null, 'preview', 'importing', 'complete'

  const expectedHeaders = [
    "externalId",
    "sampleType",
    "projectId",
    "collectionDate",
    "biosafetyLevel",
    "consentId",
    "ethicsApprovalRef",
    "mtaReference",
  ];

  const parseCSV = useCallback((csvText) => {
    const lines = csvText.split(/\r?\n/).filter((line) => line.trim());
    if (lines.length < 2) {
      throw new Error(
        "CSV must contain at least a header row and one data row",
      );
    }

    const headers = lines[0].split(",").map((h) => h.trim());

    // Validate required header
    if (!headers.includes("externalId")) {
      throw new Error("CSV must contain 'externalId' column");
    }

    const data = [];
    const errors = [];

    for (let i = 1; i < lines.length; i++) {
      const values = lines[i].split(",").map((v) => v.trim());
      const row = {};

      headers.forEach((header, index) => {
        row[header] = values[index] || "";
      });

      // Validate required field
      if (!row.externalId) {
        errors.push({
          row: i + 1,
          field: "externalId",
          message: "External ID is required",
        });
      }

      // Validate biosafety level if provided
      if (
        row.biosafetyLevel &&
        !["BSL_1", "BSL_2", "BSL_3", "BSL_4"].includes(row.biosafetyLevel)
      ) {
        errors.push({
          row: i + 1,
          field: "biosafetyLevel",
          message: `Invalid biosafety level: ${row.biosafetyLevel}`,
        });
      }

      row._rowNumber = i + 1;
      row._valid = errors.filter((e) => e.row === i + 1).length === 0;
      data.push(row);
    }

    return { data, errors };
  }, []);

  const handleFileChange = useCallback(
    (event) => {
      const uploadedFile = event.target.files?.[0];
      if (!uploadedFile) return;

      if (!uploadedFile.name.endsWith(".csv")) {
        setError(
          intl.formatMessage({
            id: "biorepository.manifest.error.invalidFormat",
            defaultMessage: "Please upload a CSV file",
          }),
        );
        return;
      }

      setFile(uploadedFile);
      setError(null);

      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const { data, errors } = parseCSV(e.target.result);
          setParsedData(data);
          setValidationErrors(errors);
          setImportStatus("preview");
        } catch (err) {
          setError(err.message);
          setParsedData([]);
          setValidationErrors([]);
        }
      };
      reader.onerror = () => {
        setError(
          intl.formatMessage({
            id: "biorepository.manifest.error.readFile",
            defaultMessage: "Failed to read file",
          }),
        );
      };
      reader.readAsText(uploadedFile);
    },
    [intl, parseCSV],
  );

  const handleImport = useCallback(() => {
    if (validationErrors.length > 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.validationErrors",
          defaultMessage: "Please fix validation errors before importing",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);
    setImportStatus("importing");

    // Transform parsed data to sample objects
    const samples = parsedData.map((row) => ({
      externalId: row.externalId,
      sampleTypeId: row.sampleType || null,
      projectId: row.projectId || null,
      collectionDate: row.collectionDate || null,
      biosafetyLevel: row.biosafetyLevel || "BSL_1",
      consentId: row.consentId || null,
      ethicsApprovalRef: row.ethicsApprovalRef || null,
      mtaReference: row.mtaReference || null,
    }));

    postToOpenElisServerJsonResponse(
      "/rest/biorepository/sample/register-bulk",
      JSON.stringify({
        samples,
        shipmentId,
      }),
      (response) => {
        setLoading(false);
        if (response?.error) {
          setError(response.error);
          setImportStatus("preview");
        } else {
          setImportStatus("complete");
          if (onImportComplete) {
            onImportComplete(response?.samples || []);
          }
        }
      },
    );
  }, [parsedData, validationErrors, shipmentId, onImportComplete, intl]);

  const handleClear = useCallback(() => {
    setFile(null);
    setParsedData([]);
    setValidationErrors([]);
    setImportStatus(null);
    setError(null);
  }, []);

  const tableHeaders = [
    { key: "row", header: "#" },
    { key: "externalId", header: "External ID" },
    { key: "sampleType", header: "Sample Type" },
    { key: "biosafetyLevel", header: "BSL" },
    { key: "status", header: "Status" },
  ];

  const tableRows = parsedData.map((row) => ({
    id: String(row._rowNumber),
    row: row._rowNumber,
    externalId: row.externalId,
    sampleType: row.sampleType || "-",
    biosafetyLevel: row.biosafetyLevel || "BSL_1",
    status: row._valid ? "valid" : "error",
  }));

  if (importStatus === "complete") {
    return (
      <InlineNotification
        kind="success"
        title={intl.formatMessage({
          id: "biorepository.manifest.success.title",
          defaultMessage: "Import Complete",
        })}
        subtitle={intl.formatMessage(
          {
            id: "biorepository.manifest.success.message",
            defaultMessage: "{count} samples imported successfully.",
          },
          { count: parsedData.length },
        )}
        lowContrast
        hideCloseButton
      />
    );
  }

  return (
    <div className="manifest-upload">
      {loading && <Loading withOverlay description="Importing samples..." />}

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.manifest.error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onClose={() => setError(null)}
        />
      )}

      <Grid>
        {importStatus !== "preview" && (
          <Column lg={16} md={8} sm={4}>
            <FileUploader
              labelTitle={intl.formatMessage({
                id: "biorepository.manifest.upload.title",
                defaultMessage: "Upload Sample Manifest",
              })}
              labelDescription={intl.formatMessage({
                id: "biorepository.manifest.upload.description",
                defaultMessage:
                  "Upload a CSV file with columns: externalId, sampleType, projectId, collectionDate, biosafetyLevel, consentId, ethicsApprovalRef, mtaReference",
              })}
              buttonLabel={intl.formatMessage({
                id: "biorepository.manifest.upload.button",
                defaultMessage: "Select CSV file",
              })}
              accept={[".csv"]}
              multiple={false}
              onChange={handleFileChange}
              filenameStatus="edit"
            />
          </Column>
        )}

        {importStatus === "preview" && parsedData.length > 0 && (
          <>
            <Column lg={16} md={8} sm={4}>
              <div style={{ marginBottom: "1rem" }}>
                <h5>
                  <FormattedMessage
                    id="biorepository.manifest.preview.title"
                    defaultMessage="Preview: {count} samples"
                    values={{ count: parsedData.length }}
                  />
                </h5>
                {validationErrors.length > 0 && (
                  <Tag type="red" style={{ marginLeft: "0.5rem" }}>
                    <Warning size={16} style={{ marginRight: "0.25rem" }} />
                    {validationErrors.length} error(s)
                  </Tag>
                )}
                {validationErrors.length === 0 && (
                  <Tag type="green" style={{ marginLeft: "0.5rem" }}>
                    <Checkmark size={16} style={{ marginRight: "0.25rem" }} />
                    <FormattedMessage
                      id="biorepository.manifest.preview.valid"
                      defaultMessage="All valid"
                    />
                  </Tag>
                )}
              </div>
            </Column>

            <Column lg={16} md={8} sm={4}>
              <DataTable rows={tableRows} headers={tableHeaders} size="sm">
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <Table {...getTableProps()} size="sm">
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "status" ? (
                                cell.value === "valid" ? (
                                  <Tag type="green" size="sm">
                                    <FormattedMessage
                                      id="biorepository.manifest.status.valid"
                                      defaultMessage="Valid"
                                    />
                                  </Tag>
                                ) : (
                                  <Tag type="red" size="sm">
                                    <FormattedMessage
                                      id="biorepository.manifest.status.error"
                                      defaultMessage="Error"
                                    />
                                  </Tag>
                                )
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
            </Column>

            <Column lg={16} md={8} sm={4}>
              <div
                className="form-actions"
                style={{ marginTop: "1rem", display: "flex", gap: "1rem" }}
              >
                <Button
                  onClick={handleImport}
                  disabled={loading || validationErrors.length > 0}
                  renderIcon={DocumentImport}
                >
                  <FormattedMessage
                    id="biorepository.manifest.button.import"
                    defaultMessage="Import {count} Samples"
                    values={{ count: parsedData.length }}
                  />
                </Button>
                <Button
                  kind="secondary"
                  onClick={handleClear}
                  disabled={loading}
                >
                  <FormattedMessage
                    id="biorepository.manifest.button.clear"
                    defaultMessage="Clear & Upload New File"
                  />
                </Button>
                {onCancel && (
                  <Button kind="ghost" onClick={onCancel} disabled={loading}>
                    <FormattedMessage
                      id="biorepository.button.cancel"
                      defaultMessage="Cancel"
                    />
                  </Button>
                )}
              </div>
            </Column>
          </>
        )}
      </Grid>
    </div>
  );
}

ManifestUpload.propTypes = {
  shipmentId: PropTypes.number,
  onImportComplete: PropTypes.func,
  onCancel: PropTypes.func,
};

export default ManifestUpload;
