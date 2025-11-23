import React, { useState } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Button,
  FileUploader,
  DataTable,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  InlineNotification,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import config from "../../config.json";

export default function GenericSampleOrderImport() {
  const [file, setFile] = useState(null);
  const [validating, setValidating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [validationResult, setValidationResult] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState(null);

  const handleFileUpload = (event) => {
    const files = event.target.files;
    if (files && files.length > 0) {
      setFile(files[0]);
      setValidationResult(null);
      setImportResult(null);
      setError(null);
    }
  };

  const handleValidate = () => {
    if (!file) {
      setError("Please select a file first");
      return;
    }

    setValidating(true);
    setError(null);
    setValidationResult(null);

    const formData = new FormData();
    formData.append("file", file);

    fetch(config.serverBaseUrl + "/rest/GenericSampleOrder/validate", {
      credentials: "include",
      method: "POST",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: formData,
    })
      .then((response) => response.json())
      .then((data) => {
        setValidating(false);
        if (data && data.errors && data.errors.length > 0) {
          setError("Validation failed. Please check the errors below.");
        }
        setValidationResult(data);
      })
      .catch((error) => {
        setValidating(false);
        setError(error?.message || "Failed to validate file");
      });
  };

  const handleImport = () => {
    if (!file) {
      setError("Please select a file first");
      return;
    }

    if (!validationResult || !validationResult.valid) {
      setError("Please validate the file first and fix any errors");
      return;
    }

    setImporting(true);
    setError(null);
    setImportResult(null);

    const formData = new FormData();
    formData.append("file", file);

    fetch(config.serverBaseUrl + "/rest/GenericSampleOrder/import", {
      credentials: "include",
      method: "POST",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: formData,
    })
      .then((response) => response.json())
      .then((data) => {
        setImporting(false);
        if (data && data.success) {
          setImportResult(data);
        } else {
          setError(data?.error || "Import failed");
        }
      })
      .catch((error) => {
        setImporting(false);
        setError(error?.message || "Failed to import file");
      });
  };

  const getErrorTableHeaders = () => [
    { key: "rowNumber", header: "Row" },
    { key: "field", header: "Field" },
    { key: "message", header: "Error Message" },
  ];

  const getPreviewTableHeaders = () => [
    { key: "rowNumber", header: "Row" },
    { key: "labNo", header: "Lab No" },
    { key: "sampleType", header: "Sample Type" },
    { key: "quantity", header: "Quantity" },
    { key: "from", header: "From" },
    { key: "collectionDate", header: "Collection Date" },
    { key: "sampleQuantity", header: "Samples to Create" },
  ];

  return (
    <div className="adminPageContent">
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "menu.genericSample" },
          { label: "menu.genericSample.import" },
        ]}
      />
      <Section>
        <Heading>
          <FormattedMessage id="menu.genericSample.import" />
        </Heading>

        <Grid fullWidth style={{ marginTop: "2rem" }}>
          <Column lg={16} md={8} sm={4}>
            <FileUploader
              labelTitle="Upload File"
              labelDescription="Upload CSV or Excel file (.csv, .xlsx, .xls)"
              buttonLabel="Select file"
              filenameStatus="edit"
              accept={[".csv", ".xlsx", ".xls"]}
              multiple={false}
              onChange={handleFileUpload}
            />
          </Column>
        </Grid>

        <Grid fullWidth style={{ marginTop: "1rem" }}>
          <Column lg={16} md={8} sm={4}>
            <Button
              kind="primary"
              onClick={handleValidate}
              disabled={!file || validating}
            >
              {validating ? (
                <InlineLoading description="Validating..." />
              ) : (
                <FormattedMessage id="label.button.validate" />
              )}
            </Button>
            <Button
              kind="primary"
              onClick={handleImport}
              disabled={
                !file ||
                !validationResult ||
                !validationResult.valid ||
                importing
              }
              style={{ marginLeft: "1rem" }}
            >
              {importing ? (
                <InlineLoading description="Importing..." />
              ) : (
                <FormattedMessage id="label.button.import" />
              )}
            </Button>
          </Column>
        </Grid>

        {error && (
          <Grid fullWidth style={{ marginTop: "1rem" }}>
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind="error"
                title="Error"
                subtitle={error}
                lowContrast
              />
            </Column>
          </Grid>
        )}

        {validationResult && (
          <Grid fullWidth style={{ marginTop: "2rem" }}>
            <Column lg={16} md={8} sm={4}>
              <Heading>
                <FormattedMessage id="label.validation.results" />
              </Heading>
              <div style={{ marginTop: "1rem" }}>
                <p>
                  <strong>Total Rows:</strong> {validationResult.totalRows}
                </p>
                <p>
                  <strong>Valid Rows:</strong> {validationResult.validRows}
                </p>
                <p>
                  <strong>Invalid Rows:</strong> {validationResult.invalidRows}
                </p>
                <p>
                  <strong>Total Samples to Create:</strong>{" "}
                  {validationResult.totalSamplesToCreate}
                </p>
                <p>
                  <strong>Validation Status:</strong>{" "}
                  {validationResult.valid ? (
                    <span style={{ color: "green" }}>Valid</span>
                  ) : (
                    <span style={{ color: "red" }}>Invalid</span>
                  )}
                </p>
              </div>

              {validationResult.errors &&
                validationResult.errors.length > 0 && (
                  <div style={{ marginTop: "2rem" }}>
                    <Heading>
                      <FormattedMessage id="label.validation.errors" />
                    </Heading>
                    <DataTable
                      rows={validationResult.errors.map((error, index) => ({
                        id: index,
                        rowNumber: error.rowNumber,
                        field: error.field,
                        message: error.message,
                      }))}
                      headers={getErrorTableHeaders()}
                    >
                      {({ rows, headers, getHeaderProps, getTableProps }) => (
                        <table {...getTableProps()}>
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
                              <TableRow key={row.id}>
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.value}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </table>
                      )}
                    </DataTable>
                  </div>
                )}

              {validationResult.previewRows &&
                validationResult.previewRows.length > 0 && (
                  <div style={{ marginTop: "2rem" }}>
                    <Heading>
                      <FormattedMessage id="label.preview.data" />
                    </Heading>
                    <DataTable
                      rows={validationResult.previewRows.map((row, index) => ({
                        id: index,
                        rowNumber: row.rowNumber,
                        labNo: row.defaultFields?.labNo || "Auto-generated",
                        sampleType: row.defaultFields?.sampleTypeId || "-",
                        quantity: row.defaultFields?.quantity || "-",
                        from: row.defaultFields?.from || "-",
                        collectionDate:
                          row.defaultFields?.collectionDate || "-",
                        sampleQuantity: row.sampleQuantity,
                      }))}
                      headers={getPreviewTableHeaders()}
                    >
                      {({ rows, headers, getHeaderProps, getTableProps }) => (
                        <table {...getTableProps()}>
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
                              <TableRow key={row.id}>
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.value}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </table>
                      )}
                    </DataTable>
                  </div>
                )}
            </Column>
          </Grid>
        )}

        {importResult && (
          <Grid fullWidth style={{ marginTop: "2rem" }}>
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind={importResult.success ? "success" : "error"}
                title={
                  importResult.success ? "Import Successful" : "Import Failed"
                }
                subtitle={
                  importResult.success
                    ? `${importResult.message || ""} Created: ${importResult.totalCreated}, Failed: ${importResult.totalFailed}`
                    : importResult.error || "Import failed"
                }
                lowContrast
              />
              {importResult.success &&
                importResult.createdAccessionNumbers &&
                importResult.createdAccessionNumbers.length > 0 && (
                  <div style={{ marginTop: "1rem" }}>
                    <Heading>
                      <FormattedMessage id="label.created.samples" />
                    </Heading>
                    <p>{importResult.createdAccessionNumbers.join(", ")}</p>
                  </div>
                )}
              {importResult.errors && importResult.errors.length > 0 && (
                <div style={{ marginTop: "1rem" }}>
                  <Heading>
                    <FormattedMessage id="label.import.errors" />
                  </Heading>
                  <ul>
                    {importResult.errors.map((error, index) => (
                      <li key={index}>{error}</li>
                    ))}
                  </ul>
                </div>
              )}
            </Column>
          </Grid>
        )}
      </Section>
    </div>
  );
}
