import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  TextInput,
  ProgressBar,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import {
  Download,
  DocumentExport,
  Email,
  Checkmark,
  Warning,
  Help,
  Analytics,
  Report,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDDataAnalysisPage - Page 10: Data Analysis & Export
 *
 * Purpose: Generate reports and export comprehensive results for the MNTD workflow.
 * This is the final page - no sample manipulation or viewing occurs here.
 * Samples are already archived in Page 9 (Sample Archiving).
 *
 * Who uses it:
 * - Data manager
 * - PI (Principal Investigator)
 *
 * Features:
 * - View validation summary statistics (counts only, no sample table)
 * - Export comprehensive results to Excel/CSV formats
 * - Record result delivery to recipients
 * - View delivery history
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDDataAnalysisPage({
  entryId,
  pageData,
  notebookId: propNotebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // State
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Validation summary (counts only - no sample table)
  const [validationSummary, setValidationSummary] = useState({
    total: 0,
    valid: 0,
    invalid: 0,
    inconclusive: 0,
    pending: 0,
  });

  // Export state
  const [exporting, setExporting] = useState(false);

  // Delivery state
  const [deliveryHistory, setDeliveryHistory] = useState([]);
  const [recipientName, setRecipientName] = useState("");
  const [recipientEmail, setRecipientEmail] = useState("");
  const [delivering, setDelivering] = useState(false);

  // Notebook ID (from parent context or fetched)
  const [notebookId, setNotebookId] = useState(propNotebookId);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load notebook ID from entry if not provided
  useEffect(() => {
    if (!propNotebookId && entryId) {
      getFromOpenElisServer(`/rest/notebook-entry/${entryId}`, (response) => {
        if (componentMounted.current && response?.notebook?.id) {
          setNotebookId(response.notebook.id);
        }
      });
    }
  }, [entryId, propNotebookId]);

  // Load validation summary (counts only)
  const loadValidationSummary = useCallback(() => {
    if (!hasRealPageId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/validation-summary`,
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            setValidationSummary(response);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id, hasRealPageId]);

  // Load delivery history
  const loadDeliveryHistory = useCallback(() => {
    if (!notebookId) return;

    getFromOpenElisServer(
      `/rest/notebook/bulk/notebook/${notebookId}/delivery-history`,
      (response) => {
        if (componentMounted.current && Array.isArray(response)) {
          setDeliveryHistory(response);
        }
      },
    );
  }, [notebookId]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadValidationSummary();

    return () => {
      componentMounted.current = false;
    };
  }, [loadValidationSummary]);

  // Load delivery history when notebookId is available
  useEffect(() => {
    if (notebookId) {
      loadDeliveryHistory();
    }
  }, [notebookId, loadDeliveryHistory]);

  // Handle export
  const handleExport = async (format, dataType = "processed") => {
    if (!notebookId) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.noNotebook",
          defaultMessage: "Notebook not found",
        }),
      );
      return;
    }

    setExporting(true);
    setError(null);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/notebook/bulk/notebook/${notebookId}/export/${format}?dataType=${dataType}`;
      const response = await fetch(endpoint, {
        method: "GET",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const contentType = response.headers.get("content-type") || "";
      const isExcelFile =
        contentType.includes(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ) || contentType.includes("application/vnd.ms-excel");
      const isCsvFile =
        contentType.includes("text/csv") ||
        contentType.includes("application/csv");

      if (
        response.ok &&
        ((format === "excel" && isExcelFile) ||
          (format === "csv" && isCsvFile) ||
          contentType.includes("application/octet-stream"))
      ) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `mntd_${dataType}_results_${notebookId}.${format === "excel" ? "xlsx" : "csv"}`;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        setSuccess(
          intl.formatMessage({
            id: "notebook.mntd.analysis.exportSuccess",
            defaultMessage: "Results exported successfully",
          }),
        );
      } else {
        let errorMessage;
        try {
          if (contentType.includes("application/json")) {
            const data = await response.json();
            errorMessage = data.error;
          } else {
            const text = await response.text();
            if (text.includes("<html") || text.includes("<!DOCTYPE")) {
              errorMessage = `Authentication required (status ${response.status})`;
            } else {
              errorMessage =
                text.substring(0, 100) ||
                `Export failed with status ${response.status}`;
            }
          }
        } catch {
          errorMessage = `Export failed with status ${response.status}`;
        }
        setError(
          errorMessage ||
            intl.formatMessage({
              id: "notebook.mntd.analysis.error.exportFailed",
              defaultMessage: "Export failed",
            }),
        );
      }
    } catch (err) {
      console.error("Export error:", err);
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.network",
          defaultMessage: "Network error during export",
        }),
      );
    } finally {
      setExporting(false);
    }
  };

  // Handle delivery
  const handleRecordDelivery = async () => {
    if (!notebookId || !recipientName.trim()) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.recipientRequired",
          defaultMessage: "Recipient name is required",
        }),
      );
      return;
    }

    setDelivering(true);
    setError(null);

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/notebook/${notebookId}/deliver`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
          body: JSON.stringify({
            recipientName: recipientName.trim(),
            recipientEmail: recipientEmail.trim() || null,
          }),
        },
      );

      const data = await response.json();

      if (response.ok && data.success) {
        setSuccess(
          intl.formatMessage({
            id: "notebook.mntd.analysis.deliverySuccess",
            defaultMessage: "Delivery recorded successfully",
          }),
        );
        setRecipientName("");
        setRecipientEmail("");
        loadDeliveryHistory();
      } else {
        setError(
          data.error ||
            intl.formatMessage({
              id: "notebook.mntd.analysis.error.deliveryFailed",
              defaultMessage: "Failed to record delivery",
            }),
        );
      }
    } catch (err) {
      console.error("Delivery error:", err);
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.network",
          defaultMessage: "Network error",
        }),
      );
    } finally {
      setDelivering(false);
    }
  };

  // Calculate progress percentage
  const progressPercentage =
    validationSummary.total > 0
      ? Math.round(
          ((validationSummary.valid +
            validationSummary.invalid +
            validationSummary.inconclusive) /
            validationSummary.total) *
            100,
        )
      : 0;

  if (loading) {
    return (
      <div className="mntd-data-analysis-page">
        <Loading withOverlay={false} />
      </div>
    );
  }

  return (
    <div className="mntd-data-analysis-page">
      {/* Header */}
      <div className="page-section-header">
        <h4>
          <Report size={20} style={{ marginRight: "0.5rem" }} />
          <FormattedMessage
            id="notebook.mntd.analysis.title"
            defaultMessage="Data Analysis & Export"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.mntd.analysis.description"
            defaultMessage="Generate comprehensive reports and export results. All samples have been archived in the previous step."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "label.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "label.success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
        />
      )}

      {/* Validation Summary Dashboard */}
      <div className="validation-dashboard">
        <Grid narrow>
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem" }}>
              <Analytics size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="notebook.mntd.analysis.sampleSummary"
                defaultMessage="Sample Summary"
              />
            </h5>
            <div className="progress-section">
              <div className="progress-tiles">
                <Tile className="progress-tile">
                  <span className="progress-label">
                    <FormattedMessage
                      id="notebook.mntd.analysis.total"
                      defaultMessage="Total"
                    />
                  </span>
                  <span className="progress-value">
                    {validationSummary.total}
                  </span>
                </Tile>
                <Tile className="progress-tile valid">
                  <span className="progress-label">
                    <Checkmark size={16} />
                    <FormattedMessage
                      id="notebook.mntd.analysis.valid"
                      defaultMessage="Valid"
                    />
                  </span>
                  <span className="progress-value valid-value">
                    {validationSummary.valid}
                  </span>
                </Tile>
                <Tile className="progress-tile invalid">
                  <span className="progress-label">
                    <Warning size={16} />
                    <FormattedMessage
                      id="notebook.mntd.analysis.invalid"
                      defaultMessage="Invalid"
                    />
                  </span>
                  <span className="progress-value invalid-value">
                    {validationSummary.invalid}
                  </span>
                </Tile>
                <Tile className="progress-tile inconclusive">
                  <span className="progress-label">
                    <Help size={16} />
                    <FormattedMessage
                      id="notebook.mntd.analysis.inconclusive"
                      defaultMessage="Inconclusive"
                    />
                  </span>
                  <span className="progress-value inconclusive-value">
                    {validationSummary.inconclusive}
                  </span>
                </Tile>
                <Tile className="progress-tile pending">
                  <span className="progress-label">
                    <FormattedMessage
                      id="notebook.mntd.analysis.pending"
                      defaultMessage="Pending"
                    />
                  </span>
                  <span className="progress-value pending-value">
                    {validationSummary.pending}
                  </span>
                </Tile>
              </div>
              <ProgressBar
                label={intl.formatMessage(
                  {
                    id: "notebook.mntd.analysis.progress",
                    defaultMessage: "Validation Progress: {percent}%",
                  },
                  { percent: progressPercentage },
                )}
                value={progressPercentage}
                max={100}
              />
            </div>
          </Column>
        </Grid>
      </div>

      {/* Export Section */}
      <div className="export-section" style={{ marginTop: "2rem" }}>
        <Grid narrow>
          <Column lg={16} md={8} sm={4}>
            <Tile>
              <h5 style={{ marginBottom: "1rem" }}>
                <DocumentExport size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.mntd.analysis.exportReports"
                  defaultMessage="Export Reports"
                />
              </h5>
              <p style={{ color: "#525252", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.mntd.analysis.exportDescription"
                  defaultMessage="Export comprehensive sample data including all workflow steps, test results, instruments, reagents, QC data, and storage information."
                />
              </p>
              <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap" }}>
                <Button
                  kind="primary"
                  renderIcon={DocumentExport}
                  onClick={() => handleExport("excel", "processed")}
                  disabled={exporting || !notebookId}
                >
                  {exporting ? (
                    <FormattedMessage
                      id="notebook.mntd.analysis.exporting"
                      defaultMessage="Exporting..."
                    />
                  ) : (
                    <FormattedMessage
                      id="notebook.mntd.analysis.exportExcel"
                      defaultMessage="Export to Excel"
                    />
                  )}
                </Button>

                <Button
                  kind="secondary"
                  renderIcon={Download}
                  onClick={() => handleExport("csv", "raw")}
                  disabled={exporting || !notebookId}
                >
                  <FormattedMessage
                    id="notebook.mntd.analysis.exportCsv"
                    defaultMessage="Export to CSV"
                  />
                </Button>
              </div>
            </Tile>
          </Column>
        </Grid>
      </div>

      {/* Delivery Section */}
      <div className="delivery-section" style={{ marginTop: "2rem" }}>
        <Grid narrow>
          <Column lg={16} md={8} sm={4}>
            <Tile>
              <h5 style={{ marginBottom: "1rem" }}>
                <Email size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.mntd.analysis.delivery.title"
                  defaultMessage="Record Delivery"
                />
              </h5>
              <p style={{ color: "#525252", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.mntd.analysis.delivery.description"
                  defaultMessage="Record when results are delivered to recipients for audit trail purposes."
                />
              </p>
              <Grid narrow>
                <Column lg={5} md={4} sm={4}>
                  <TextInput
                    id="recipient-name"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.analysis.delivery.recipientName",
                      defaultMessage: "Recipient Name",
                    })}
                    placeholder={intl.formatMessage({
                      id: "notebook.mntd.analysis.delivery.recipientNamePlaceholder",
                      defaultMessage: "e.g., Data Management Team, PI Name",
                    })}
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                  />
                </Column>
                <Column lg={5} md={4} sm={4}>
                  <TextInput
                    id="recipient-email"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.analysis.delivery.recipientEmail",
                      defaultMessage: "Recipient Email (optional)",
                    })}
                    placeholder="email@example.com"
                    value={recipientEmail}
                    onChange={(e) => setRecipientEmail(e.target.value)}
                    type="email"
                  />
                </Column>
                <Column lg={6} md={4} sm={4}>
                  <Button
                    kind="primary"
                    size="md"
                    renderIcon={Email}
                    onClick={handleRecordDelivery}
                    disabled={delivering || !recipientName.trim()}
                    style={{ marginTop: "1.5rem" }}
                  >
                    <FormattedMessage
                      id="notebook.mntd.analysis.delivery.record"
                      defaultMessage="Record Delivery"
                    />
                  </Button>
                </Column>
              </Grid>

              {/* Delivery History */}
              {deliveryHistory.length > 0 && (
                <div
                  className="delivery-history"
                  style={{ marginTop: "1.5rem" }}
                >
                  <h6 style={{ marginBottom: "0.5rem" }}>
                    <FormattedMessage
                      id="notebook.mntd.analysis.delivery.history"
                      defaultMessage="Delivery History"
                    />
                  </h6>
                  <DataTable
                    rows={deliveryHistory.map((record, idx) => ({
                      id: String(record.id || idx),
                      recipientName: record.recipientName,
                      recipientEmail: record.recipientEmail || "-",
                      fileName: record.fileName || "-",
                      deliveredAt: record.deliveredAt
                        ? new Date(record.deliveredAt).toLocaleString()
                        : "-",
                      deliveredBy: record.deliveredBy || "-",
                    }))}
                    headers={[
                      { key: "recipientName", header: "Recipient" },
                      { key: "recipientEmail", header: "Email" },
                      { key: "fileName", header: "File" },
                      { key: "deliveredAt", header: "Delivered At" },
                      { key: "deliveredBy", header: "Delivered By" },
                    ]}
                    size="sm"
                  >
                    {({
                      rows,
                      headers,
                      getTableProps,
                      getHeaderProps,
                      getRowProps,
                    }) => (
                      <TableContainer>
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
                                    {cell.value}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </DataTable>
                </div>
              )}
            </Tile>
          </Column>
        </Grid>
      </div>
    </div>
  );
}

export default MNTDDataAnalysisPage;
