import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Tag,
  Loading,
  Dropdown,
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
  TableSelectRow,
  TableSelectAll,
} from "@carbon/react";
import {
  Download,
  DocumentExport,
  Email,
  FlagFilled,
  Checkmark,
  Warning,
  Help,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import FlagSampleModal from "../workflow/FlagSampleModal";
import config from "../../../config.json";
import "../workflow/NotebookWorkflow.css";

/**
 * ResultCompilationPage - Page 8: Result Compilation & Dissemination
 *
 * US7: Compile analysis outputs into structured result files or database records,
 * deliver results to Data Management Team or designated recipients,
 * and flag invalid or inconclusive results for review.
 *
 * Features:
 * - View samples with validation status (Valid/Invalid/Inconclusive/Pending)
 * - Flag individual or bulk samples with validation status
 * - Export results to Excel/CSV formats
 * - Record result delivery to recipients
 * - View delivery history
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ResultCompilationPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Validation summary
  const [validationSummary, setValidationSummary] = useState({
    total: 0,
    valid: 0,
    invalid: 0,
    inconclusive: 0,
    pending: 0,
  });

  // Flag modal state
  const [flagModalOpen, setFlagModalOpen] = useState(false);

  // Export state
  const [exporting, setExporting] = useState(false);
  const [exportFormat, setExportFormat] = useState("excel");

  // Delivery state
  const [deliveryHistory, setDeliveryHistory] = useState([]);
  const [recipientName, setRecipientName] = useState("");
  const [recipientEmail, setRecipientEmail] = useState("");
  const [delivering, setDelivering] = useState(false);

  // Notebook ID (from parent context)
  const [notebookId, setNotebookId] = useState(null);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  console.log(
    "ResultCompilationPage: pageData=",
    pageData,
    "hasRealPageId=",
    hasRealPageId,
  );

  // Load notebook ID from entry
  useEffect(() => {
    if (entryId) {
      getFromOpenElisServer(`/rest/notebook-entry/${entryId}`, (response) => {
        if (componentMounted.current && response?.notebook?.id) {
          setNotebookId(response.notebook.id);
          console.log(
            "ResultCompilationPage: Loaded notebookId",
            response.notebook.id,
          );
        }
      });
    }
  }, [entryId]);

  // Load samples with validation status
  const loadSamples = useCallback(() => {
    console.log(
      "loadSamples called, hasRealPageId=",
      hasRealPageId,
      "pageData.id=",
      pageData?.id,
    );
    if (!hasRealPageId) {
      console.log("loadSamples: No real page ID, skipping");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const url = `/rest/notebook/bulk/page/${pageData.id}/samples-with-validation`;
    console.log("loadSamples: Fetching from", url);
    getFromOpenElisServer(url, (response) => {
      console.log("loadSamples: Got response", response);
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setSamples(response);
        } else if (response && !response.error) {
          setSamples([]);
        }
        setLoading(false);
      }
    });
  }, [pageData?.id, hasRealPageId]);

  // Load validation summary
  const loadValidationSummary = useCallback(() => {
    if (!hasRealPageId) return;

    getFromOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/validation-summary`,
      (response) => {
        if (componentMounted.current && response && !response.error) {
          setValidationSummary(response);
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
    loadSamples();
    loadValidationSummary();

    return () => {
      componentMounted.current = false;
    };
  }, [loadSamples, loadValidationSummary]);

  // Load delivery history when notebookId is available
  useEffect(() => {
    if (notebookId) {
      loadDeliveryHistory();
    }
  }, [notebookId, loadDeliveryHistory]);

  // Handle flag success
  const handleFlagSuccess = (result) => {
    setSuccess(
      intl.formatMessage(
        {
          id: "notebook.compilation.flagSuccess",
          defaultMessage: "Successfully flagged {count} sample(s) as {status}",
        },
        { count: result.flaggedCount, status: result.status },
      ),
    );
    setSelectedSampleIds([]);
    loadSamples();
    loadValidationSummary();
  };

  // Handle export
  const handleExport = async (format) => {
    if (!notebookId) {
      setError(
        intl.formatMessage({
          id: "notebook.compilation.error.noNotebook",
          defaultMessage: "Notebook not found",
        }),
      );
      return;
    }

    setExporting(true);
    setError(null);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/notebook/bulk/notebook/${notebookId}/export/${format}`;
      const response = await fetch(endpoint, {
        method: "GET",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      // Check content-type to determine if we got a valid file or an error
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
        // Valid file response - create blob and download
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `results_notebook_${notebookId}.${format === "excel" ? "xlsx" : "csv"}`;
        document.body.appendChild(a);
        a.click();
        // Delay cleanup to ensure download starts
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        setSuccess(
          intl.formatMessage({
            id: "notebook.compilation.exportSuccess",
            defaultMessage: "Results exported successfully",
          }),
        );
      } else {
        // Error response - try to parse JSON error, handle HTML/text responses
        let errorMessage;
        try {
          if (contentType.includes("application/json")) {
            const data = await response.json();
            errorMessage = data.error;
          } else {
            // Non-JSON error (could be HTML redirect page or plain text)
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
        console.error("Export error response:", {
          status: response.status,
          contentType,
          errorMessage,
        });
        setError(
          errorMessage ||
            intl.formatMessage({
              id: "notebook.compilation.error.exportFailed",
              defaultMessage: "Export failed",
            }),
        );
      }
    } catch (err) {
      console.error("Export error:", err);
      setError(
        intl.formatMessage({
          id: "notebook.compilation.error.network",
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
          id: "notebook.compilation.error.recipientRequired",
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
            id: "notebook.compilation.deliverySuccess",
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
              id: "notebook.compilation.error.deliveryFailed",
              defaultMessage: "Failed to record delivery",
            }),
        );
      }
    } catch (err) {
      console.error("Delivery error:", err);
      setError(
        intl.formatMessage({
          id: "notebook.compilation.error.network",
          defaultMessage: "Network error",
        }),
      );
    } finally {
      setDelivering(false);
    }
  };

  // Filter samples by validation status
  const filteredSamples = samples.filter((sample) => {
    if (statusFilter === "ALL") return true;
    return sample.validationStatus === statusFilter;
  });

  // Status filter options
  const statusFilterOptions = [
    {
      id: "ALL",
      label: intl.formatMessage({
        id: "notebook.compilation.filter.all",
        defaultMessage: "All",
      }),
    },
    {
      id: "VALID",
      label: intl.formatMessage({
        id: "notebook.compilation.filter.valid",
        defaultMessage: "Valid",
      }),
    },
    {
      id: "INVALID",
      label: intl.formatMessage({
        id: "notebook.compilation.filter.invalid",
        defaultMessage: "Invalid",
      }),
    },
    {
      id: "INCONCLUSIVE",
      label: intl.formatMessage({
        id: "notebook.compilation.filter.inconclusive",
        defaultMessage: "Inconclusive",
      }),
    },
    {
      id: "PENDING",
      label: intl.formatMessage({
        id: "notebook.compilation.filter.pending",
        defaultMessage: "Pending",
      }),
    },
  ];

  // Get validation status tag
  const getValidationTag = (sample) => {
    const color = sample?.validationColor || "gray";
    const displayName = sample?.validationDisplayName || "Pending";

    return (
      <Tag type={color === "gray" ? "cool-gray" : color}>{displayName}</Tag>
    );
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
      <div className="result-compilation-page">
        <Loading withOverlay={false} />
      </div>
    );
  }

  return (
    <div className="result-compilation-page">
      {/* Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.compilation.title"
            defaultMessage="Result Compilation & Dissemination"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.compilation.description"
            defaultMessage="Review validation status, flag samples, and export results for delivery."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "label.error" })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "label.success" })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
        />
      )}

      {/* Validation Summary Dashboard */}
      <div className="validation-dashboard">
        <Grid narrow>
          <Column lg={12} md={8} sm={4}>
            <div className="progress-section">
              <div className="progress-tiles">
                <Tile className="progress-tile">
                  <span className="progress-label">
                    <FormattedMessage
                      id="notebook.compilation.total"
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
                      id="notebook.compilation.valid"
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
                      id="notebook.compilation.invalid"
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
                      id="notebook.compilation.inconclusive"
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
                      id="notebook.compilation.pending"
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
                    id: "notebook.compilation.progress",
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

      {/* Actions Bar */}
      <div className="page-actions-bar">
        <Dropdown
          id="status-filter"
          titleText=""
          label={intl.formatMessage({
            id: "notebook.compilation.filter.label",
            defaultMessage: "Filter by status",
          })}
          items={statusFilterOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={statusFilterOptions.find((o) => o.id === statusFilter)}
          onChange={({ selectedItem }) =>
            setStatusFilter(selectedItem?.id || "ALL")
          }
          size="sm"
        />

        <Button
          kind="primary"
          size="sm"
          renderIcon={FlagFilled}
          onClick={() => setFlagModalOpen(true)}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.compilation.flagSelected"
            defaultMessage="Flag Selected ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={DocumentExport}
          onClick={() => handleExport("excel")}
          disabled={exporting || !notebookId}
        >
          <FormattedMessage
            id="notebook.compilation.exportExcel"
            defaultMessage="Export Excel"
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Download}
          onClick={() => handleExport("csv")}
          disabled={exporting || !notebookId}
        >
          <FormattedMessage
            id="notebook.compilation.exportCsv"
            defaultMessage="Export CSV"
          />
        </Button>
      </div>

      {/* Sample Table with Validation Status */}
      <div className="sample-grid-container">
        {filteredSamples.length > 0 ? (
          <DataTable
            rows={filteredSamples.map((sample) => ({
              id: sample.id,
              externalId: sample.externalId || "-",
              sampleType: sample.sampleType || "-",
              pageStatus: sample.pageStatus || "-",
              validationStatus: sample.validationStatus,
              validationDisplayName: sample.validationDisplayName,
              validationColor: sample.validationColor,
              validationReason: sample.validationReason || "-",
            }))}
            headers={[
              {
                key: "id",
                header: intl.formatMessage({
                  id: "notebook.compilation.column.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.compilation.column.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.compilation.column.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "pageStatus",
                header: intl.formatMessage({
                  id: "notebook.compilation.column.status",
                  defaultMessage: "Status",
                }),
              },
              {
                key: "validationStatus",
                header: intl.formatMessage({
                  id: "notebook.compilation.column.validation",
                  defaultMessage: "Validation",
                }),
              },
              {
                key: "validationReason",
                header: intl.formatMessage({
                  id: "notebook.compilation.column.reason",
                  defaultMessage: "Reason",
                }),
              },
            ]}
            size="sm"
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableSelectAll {...getSelectionProps()} />
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
                    {rows.map((row) => {
                      const sample = filteredSamples.find(
                        (s) => s.id === row.id,
                      );
                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          <TableSelectRow
                            {...getSelectionProps({ row })}
                            checked={selectedSampleIds.includes(row.id)}
                            onChange={() => {
                              setSelectedSampleIds((prev) =>
                                prev.includes(row.id)
                                  ? prev.filter((id) => id !== row.id)
                                  : [...prev, row.id],
                              );
                            }}
                          />
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "validationStatus"
                                ? getValidationTag(sample)
                                : cell.value}
                            </TableCell>
                          ))}
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        ) : (
          <div className="empty-state">
            <FormattedMessage
              id="notebook.compilation.noSamples"
              defaultMessage="No samples found for this page."
            />
          </div>
        )}
      </div>

      {/* Delivery Section */}
      <div className="delivery-section">
        <h5>
          <FormattedMessage
            id="notebook.compilation.delivery.title"
            defaultMessage="Record Delivery"
          />
        </h5>
        <Grid narrow>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="recipient-name"
              labelText={intl.formatMessage({
                id: "notebook.compilation.delivery.recipientName",
                defaultMessage: "Recipient Name",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.compilation.delivery.recipientNamePlaceholder",
                defaultMessage: "e.g., Data Management Team",
              })}
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="recipient-email"
              labelText={intl.formatMessage({
                id: "notebook.compilation.delivery.recipientEmail",
                defaultMessage: "Recipient Email (optional)",
              })}
              placeholder="email@example.com"
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              type="email"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Email}
              onClick={handleRecordDelivery}
              disabled={delivering || !recipientName.trim()}
              style={{ marginTop: "1.5rem" }}
            >
              <FormattedMessage
                id="notebook.compilation.delivery.record"
                defaultMessage="Record Delivery"
              />
            </Button>
          </Column>
        </Grid>

        {/* Delivery History */}
        {deliveryHistory.length > 0 && (
          <div className="delivery-history">
            <h6>
              <FormattedMessage
                id="notebook.compilation.delivery.history"
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
                            <TableCell key={cell.id}>{cell.value}</TableCell>
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
      </div>

      {/* Flag Sample Modal */}
      <FlagSampleModal
        open={flagModalOpen}
        onClose={() => setFlagModalOpen(false)}
        pageId={pageData?.id}
        selectedSamples={selectedSampleIds}
        onFlagSuccess={handleFlagSuccess}
      />
    </div>
  );
}

export default ResultCompilationPage;
