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
import { Printer } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import config from "../../config.json";

/**
 * GenericSampleOrderImport - Configurable sample order import component
 *
 * @param {Object} props - Component configuration
 * @param {string} props.title - Page title (i18n key)
 * @param {string} props.titleDefault - Default page title
 * @param {Array} props.breadcrumbs - Custom breadcrumb array [{label, link}]
 * @param {string} props.validateEndpoint - API endpoint for validation (default: "/rest/GenericSampleOrder/validate")
 * @param {string} props.importEndpoint - API endpoint for import (default: "/rest/GenericSampleOrder/import")
 * @param {boolean} props.showBreadcrumbs - Show breadcrumbs (default: true)
 * @param {boolean} props.showPrintBarcodes - Show print barcodes button after import (default: true)
 * @param {Array} props.acceptedFileTypes - Accepted file types (default: [".csv", ".xlsx", ".xls"])
 * @param {Function} props.onValidationComplete - Callback after validation completes (result) => void
 * @param {Function} props.onImportSuccess - Callback after successful import (result) => void
 * @param {Function} props.onImportError - Callback after import error (error) => void
 * @param {Function} props.getPreviewTableHeaders - Custom function for preview table headers
 * @param {Function} props.transformPreviewRow - Transform preview row for display (row) => row
 * @param {Function} props.renderCustomContent - Render function for custom content
 */
export default function GenericSampleOrderImport({
  title = "menu.genericSample.import",
  titleDefault = "Import Generic Samples",
  breadcrumbs: customBreadcrumbs,
  validateEndpoint = "/rest/GenericSampleOrder/validate",
  importEndpoint = "/rest/GenericSampleOrder/import",
  showBreadcrumbs = true,
  showPrintBarcodes = true,
  acceptedFileTypes = [".csv", ".xlsx", ".xls"],
  onValidationComplete,
  onImportSuccess,
  onImportError,
  getPreviewTableHeaders: customGetPreviewTableHeaders,
  transformPreviewRow,
  renderCustomContent,
}) {
  const intl = useIntl();
  const [file, setFile] = useState(null);
  const [validating, setValidating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [validationResult, setValidationResult] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState(null);

  /**
   * Handle printing barcode for a single sample.
   * Opens the LabelMakerServlet in a new window to generate and print the barcode PDF.
   */
  const handlePrintBarCode = (accessionNumber) => {
    const barcodesPdf =
      config.serverBaseUrl +
      `/LabelMakerServlet?labNo=${encodeURIComponent(accessionNumber)}&type=order&quantity=1`;
    window.open(barcodesPdf);
  };

  /**
   * Handle printing barcodes for all created samples.
   * Opens a new window for each accession number.
   */
  const handlePrintAllBarcodes = (accessionNumbers) => {
    if (!accessionNumbers || accessionNumbers.length === 0) return;

    // Open first one immediately, then stagger the rest to avoid popup blockers
    handlePrintBarCode(accessionNumbers[0]);

    // For remaining samples, open with slight delay
    accessionNumbers.slice(1).forEach((accNo, index) => {
      setTimeout(
        () => {
          handlePrintBarCode(accNo);
        },
        (index + 1) * 500,
      ); // 500ms delay between each
    });
  };

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
      setError(intl.formatMessage({ id: "import.error.selectFile" }));
      return;
    }

    setValidating(true);
    setError(null);
    setValidationResult(null);

    const formData = new FormData();
    formData.append("file", file);

    fetch(config.serverBaseUrl + validateEndpoint, {
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
          setError(intl.formatMessage({ id: "import.error.validationFailed" }));
        }
        setValidationResult(data);
        if (onValidationComplete) {
          onValidationComplete(data);
        }
      })
      .catch((error) => {
        setValidating(false);
        setError(
          error?.message ||
            intl.formatMessage({ id: "import.error.validateFile" }),
        );
      });
  };

  const handleImport = () => {
    if (!file) {
      setError(intl.formatMessage({ id: "import.error.selectFile" }));
      return;
    }

    if (!validationResult || !validationResult.valid) {
      setError(intl.formatMessage({ id: "import.error.validateFirst" }));
      return;
    }

    setImporting(true);
    setError(null);
    setImportResult(null);

    const formData = new FormData();
    formData.append("file", file);

    fetch(config.serverBaseUrl + importEndpoint, {
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
          if (onImportSuccess) {
            onImportSuccess(data);
          }
        } else {
          const errorMsg =
            data?.error || intl.formatMessage({ id: "import.error.failed" });
          setError(errorMsg);
          if (onImportError) {
            onImportError(errorMsg);
          }
        }
      })
      .catch((error) => {
        setImporting(false);
        const errorMsg =
          error?.message ||
          intl.formatMessage({ id: "import.error.importFile" });
        setError(errorMsg);
        if (onImportError) {
          onImportError(errorMsg);
        }
      });
  };

  const getErrorTableHeaders = () => [
    {
      key: "rowNumber",
      header: intl.formatMessage({ id: "import.header.row" }),
    },
    { key: "field", header: intl.formatMessage({ id: "import.header.field" }) },
    {
      key: "message",
      header: intl.formatMessage({ id: "import.header.errorMessage" }),
    },
  ];

  const defaultGetPreviewTableHeaders = () => [
    {
      key: "rowNumber",
      header: intl.formatMessage({ id: "import.header.row" }),
    },
    { key: "labNo", header: intl.formatMessage({ id: "import.header.labNo" }) },
    {
      key: "sampleType",
      header: intl.formatMessage({ id: "import.header.sampleType" }),
    },
    {
      key: "quantity",
      header: intl.formatMessage({ id: "import.header.quantity" }),
    },
    { key: "from", header: intl.formatMessage({ id: "import.header.from" }) },
    {
      key: "collectionDate",
      header: intl.formatMessage({ id: "import.header.collectionDate" }),
    },
    {
      key: "sampleQuantity",
      header: intl.formatMessage({ id: "import.header.samplesToCreate" }),
    },
  ];

  const getPreviewTableHeaders =
    customGetPreviewTableHeaders || defaultGetPreviewTableHeaders;

  // Default breadcrumbs
  const defaultBreadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "menu.genericSample" },
    { label: "menu.genericSample.import" },
  ];

  const breadcrumbs = customBreadcrumbs || defaultBreadcrumbs;

  return (
    <>
      {showBreadcrumbs && <PageBreadCrumb breadcrumbs={breadcrumbs} />}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id={title} defaultMessage={titleDefault} />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <div className="orderLegendBody">
        <Section>
          <Grid fullWidth style={{ marginTop: "2rem" }}>
            <Column lg={16} md={8} sm={4}>
              <FileUploader
                labelTitle={intl.formatMessage({ id: "import.uploadFile" })}
                labelDescription={intl.formatMessage(
                  { id: "import.uploadFileDesc" },
                  { types: acceptedFileTypes.join(", ") },
                )}
                buttonLabel={intl.formatMessage({ id: "import.selectFile" })}
                filenameStatus="edit"
                accept={acceptedFileTypes}
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
                  <InlineLoading
                    description={intl.formatMessage({
                      id: "import.validating",
                    })}
                  />
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
                  <InlineLoading
                    description={intl.formatMessage({ id: "import.importing" })}
                  />
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
                  title={intl.formatMessage({ id: "error.title" })}
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
                    <strong>
                      <FormattedMessage id="import.totalRows" />:
                    </strong>{" "}
                    {validationResult.totalRows}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="import.validRows" />:
                    </strong>{" "}
                    {validationResult.validRows}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="import.invalidRows" />:
                    </strong>{" "}
                    {validationResult.invalidRows}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="import.totalSamplesToCreate" />:
                    </strong>{" "}
                    {validationResult.totalSamplesToCreate}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="import.validationStatus" />:
                    </strong>{" "}
                    {validationResult.valid ? (
                      <span style={{ color: "green" }}>
                        <FormattedMessage id="import.status.valid" />
                      </span>
                    ) : (
                      <span style={{ color: "red" }}>
                        <FormattedMessage id="import.status.invalid" />
                      </span>
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
                        rows={validationResult.previewRows.map((row, index) => {
                          const defaultRow = {
                            id: index,
                            rowNumber: row.rowNumber,
                            labNo:
                              row.defaultFields?.labNo ||
                              intl.formatMessage({
                                id: "import.autoGenerated",
                              }),
                            sampleType: row.defaultFields?.sampleTypeId || "-",
                            quantity: row.defaultFields?.quantity || "-",
                            from: row.defaultFields?.from || "-",
                            collectionDate:
                              row.defaultFields?.collectionDate || "-",
                            sampleQuantity: row.sampleQuantity,
                          };
                          return transformPreviewRow
                            ? transformPreviewRow(defaultRow, row)
                            : defaultRow;
                        })}
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
                    importResult.success
                      ? intl.formatMessage({ id: "import.success" })
                      : intl.formatMessage({ id: "import.failed" })
                  }
                  subtitle={
                    importResult.success
                      ? `${importResult.message || ""} ${intl.formatMessage({ id: "import.created" })}: ${importResult.totalCreated}, ${intl.formatMessage({ id: "import.failedCount" })}: ${importResult.totalFailed}`
                      : importResult.error ||
                        intl.formatMessage({ id: "import.failed" })
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

                      {/* Print Barcode Button */}
                      {showPrintBarcodes && (
                        <div style={{ marginTop: "1rem" }}>
                          <Button
                            kind="primary"
                            renderIcon={Printer}
                            onClick={() =>
                              handlePrintAllBarcodes(
                                importResult.createdAccessionNumbers,
                              )
                            }
                          >
                            <FormattedMessage
                              id="print.barcode.all"
                              defaultMessage="Print All Barcodes ({count})"
                              values={{
                                count:
                                  importResult.createdAccessionNumbers.length,
                              }}
                            />
                          </Button>
                        </div>
                      )}
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

          {/* Custom content render */}
          {renderCustomContent &&
            renderCustomContent({
              file,
              validationResult,
              importResult,
              error,
              validating,
              importing,
            })}
        </Section>
      </div>
    </>
  );
}
