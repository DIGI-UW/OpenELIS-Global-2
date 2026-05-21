import React, { useState, useEffect, useRef } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Dropdown,
  Modal,
  Checkbox,
} from "@carbon/react";
import { DocumentExport } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * ImmunologyResultCompilationPage - Stage 8 of the Immunology workflow.
 * Provides export functionality for compiled results.
 *
 * DELIVERABLES:
 * - Raw data files (ELISA OD values, FCS files)
 * - Analyzed data (calculated concentrations, cell percentages)
 * - QC summary (controls passed/failed)
 * - Graphs/visualizations (standard curves, dot plots, histograms)
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ImmunologyResultCompilationPage({
  entryId,
  notebookId: propNotebookId,
  pageData,
  progress: _progress, // eslint-disable-line no-unused-vars
  onProgressUpdate: _onProgressUpdate, // eslint-disable-line no-unused-vars
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // State
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Export state
  const [exporting, setExporting] = useState(false);
  const [exportModalOpen, setExportModalOpen] = useState(false);
  const [exportOptions, setExportOptions] = useState({
    includeRawData: true,
    includeAnalyzedData: true,
    includeQCSummary: true,
    includeVisualizations: false,
    format: "excel",
  });

  // Notebook ID (from parent context or loaded from entry)
  const [notebookId, setNotebookId] = useState(propNotebookId);

  // Load notebook ID from entry if not provided
  useEffect(() => {
    componentMounted.current = true;

    if (propNotebookId) {
      setNotebookId(propNotebookId);
    } else if (entryId) {
      getFromOpenElisServer(`/rest/notebook-entry/${entryId}`, (response) => {
        if (componentMounted.current && response?.notebook?.id) {
          setNotebookId(response.notebook.id);
        }
      });
    }

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, propNotebookId]);

  // Handle export with options
  const handleExport = async () => {
    const nbId = notebookId || propNotebookId;
    if (!nbId) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.compilation.error.noNotebook",
          defaultMessage: "Notebook not found",
        }),
      );
      return;
    }

    setExporting(true);
    setError(null);

    try {
      const queryParams = new URLSearchParams({
        includeRawData: exportOptions.includeRawData,
        includeAnalyzedData: exportOptions.includeAnalyzedData,
        includeQCSummary: exportOptions.includeQCSummary,
        includeVisualizations: exportOptions.includeVisualizations,
      });

      const endpoint = `${config.serverBaseUrl}/rest/notebook/bulk/notebook/${nbId}/export/${exportOptions.format}?${queryParams}`;
      const response = await fetch(endpoint, {
        method: "GET",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const contentType = response.headers.get("content-type") || "";
      const isValidFile =
        contentType.includes("application/vnd.openxmlformats") ||
        contentType.includes("application/vnd.ms-excel") ||
        contentType.includes("text/csv") ||
        contentType.includes("application/octet-stream");

      if (response.ok && isValidFile) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `immunology_results_${nbId}.${exportOptions.format === "excel" ? "xlsx" : "csv"}`;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        setSuccess(
          intl.formatMessage({
            id: "notebook.immunology.compilation.exportSuccess",
            defaultMessage: "Results exported successfully",
          }),
        );
        setExportModalOpen(false);
      } else {
        let errorMessage;
        try {
          if (contentType.includes("application/json")) {
            const data = await response.json();
            errorMessage = data.error;
          } else {
            errorMessage = `Export failed with status ${response.status}`;
          }
        } catch {
          errorMessage = `Export failed with status ${response.status}`;
        }
        setError(errorMessage);
      }
    } catch (err) {
      console.error("Export error:", err);
      setError(
        intl.formatMessage({
          id: "notebook.immunology.compilation.error.network",
          defaultMessage: "Network error during export",
        }),
      );
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="immunology-result-compilation-page">
      {/* Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.immunology.compilation.title"
            defaultMessage="Result Compilation & Export"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.immunology.compilation.description"
            defaultMessage="Export compiled analysis outputs and generate reports for dissemination."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Export Section */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <Tile className="export-tile">
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "1rem",
                padding: "2rem",
                textAlign: "center",
              }}
            >
              <DocumentExport size={48} style={{ color: "#0f62fe" }} />
              <div>
                <h5 style={{ margin: 0 }}>
                  <FormattedMessage
                    id="notebook.immunology.compilation.exportTitle"
                    defaultMessage="Export Results"
                  />
                </h5>
                <p
                  style={{
                    margin: "0.5rem 0 1.5rem 0",
                    color: "#525252",
                    maxWidth: "500px",
                  }}
                >
                  <FormattedMessage
                    id="notebook.immunology.compilation.exportDescription"
                    defaultMessage="Export all compiled results including raw data, analyzed data, and QC summary. Choose your preferred format and select which deliverables to include."
                  />
                </p>
                <Button
                  kind="primary"
                  size="lg"
                  renderIcon={DocumentExport}
                  onClick={() => setExportModalOpen(true)}
                  disabled={!notebookId}
                >
                  <FormattedMessage
                    id="notebook.immunology.compilation.exportButton"
                    defaultMessage="Export Results"
                  />
                </Button>
              </div>
            </div>
          </Tile>
        </Column>
      </Grid>

      {/* Deliverables Info */}
      <Grid fullWidth style={{ marginTop: "1.5rem" }}>
        <Column lg={8} md={4} sm={4}>
          <Tile>
            <h6 style={{ marginBottom: "0.75rem" }}>
              <FormattedMessage
                id="notebook.immunology.compilation.deliverables"
                defaultMessage="Available Deliverables"
              />
            </h6>
            <ul style={{ margin: 0, paddingLeft: "1.25rem", color: "#525252" }}>
              <li>
                <FormattedMessage
                  id="notebook.immunology.compilation.deliverable.rawData"
                  defaultMessage="Raw Data (ELISA OD values, FCS files)"
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.immunology.compilation.deliverable.analyzedData"
                  defaultMessage="Analyzed Data (concentrations, cell percentages)"
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.immunology.compilation.deliverable.qcSummary"
                  defaultMessage="QC Summary (controls passed/failed)"
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.immunology.compilation.deliverable.visualizations"
                  defaultMessage="Visualizations (standard curves, dot plots, histograms)"
                />
              </li>
            </ul>
          </Tile>
        </Column>
        <Column lg={8} md={4} sm={4}>
          <Tile>
            <h6 style={{ marginBottom: "0.75rem" }}>
              <FormattedMessage
                id="notebook.immunology.compilation.formats"
                defaultMessage="Export Formats"
              />
            </h6>
            <ul style={{ margin: 0, paddingLeft: "1.25rem", color: "#525252" }}>
              <li>
                <strong>Excel (.xlsx)</strong> -{" "}
                <FormattedMessage
                  id="notebook.immunology.compilation.format.excel"
                  defaultMessage="Recommended for comprehensive reports with multiple sheets"
                />
              </li>
              <li>
                <strong>CSV (.csv)</strong> -{" "}
                <FormattedMessage
                  id="notebook.immunology.compilation.format.csv"
                  defaultMessage="Simple format for data import into other systems"
                />
              </li>
            </ul>
          </Tile>
        </Column>
      </Grid>

      {/* Export Options Modal */}
      <Modal
        open={exportModalOpen}
        onRequestClose={() => setExportModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.compilation.exportModal.title",
          defaultMessage: "Export Results",
        })}
        primaryButtonText={
          exporting
            ? intl.formatMessage({
                id: "label.exporting",
                defaultMessage: "Exporting...",
              })
            : intl.formatMessage({
                id: "notebook.immunology.compilation.exportModal.export",
                defaultMessage: "Export",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleExport}
        primaryButtonDisabled={exporting}
        size="md"
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="notebook.immunology.compilation.exportModal.description"
            defaultMessage="Select the data to include in the export file."
          />
        </p>

        <Dropdown
          id="export-format"
          titleText={intl.formatMessage({
            id: "notebook.immunology.compilation.exportFormat",
            defaultMessage: "Export Format",
          })}
          items={[
            { id: "excel", label: "Excel (.xlsx)" },
            { id: "csv", label: "CSV (.csv)" },
          ]}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={{
            id: exportOptions.format,
            label:
              exportOptions.format === "excel" ? "Excel (.xlsx)" : "CSV (.csv)",
          }}
          onChange={({ selectedItem }) =>
            setExportOptions((prev) => ({
              ...prev,
              format: selectedItem?.id || "excel",
            }))
          }
          style={{ marginBottom: "1rem" }}
        />

        <h6 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="notebook.immunology.compilation.exportModal.deliverables"
            defaultMessage="Deliverables"
          />
        </h6>

        <Checkbox
          id="include-raw-data"
          labelText={intl.formatMessage({
            id: "notebook.immunology.compilation.includeRawData",
            defaultMessage: "Raw Data (ELISA OD values, FCS files)",
          })}
          checked={exportOptions.includeRawData}
          onChange={(e, { checked }) =>
            setExportOptions((prev) => ({ ...prev, includeRawData: checked }))
          }
        />

        <Checkbox
          id="include-analyzed-data"
          labelText={intl.formatMessage({
            id: "notebook.immunology.compilation.includeAnalyzedData",
            defaultMessage: "Analyzed Data (concentrations, cell percentages)",
          })}
          checked={exportOptions.includeAnalyzedData}
          onChange={(e, { checked }) =>
            setExportOptions((prev) => ({
              ...prev,
              includeAnalyzedData: checked,
            }))
          }
        />

        <Checkbox
          id="include-qc-summary"
          labelText={intl.formatMessage({
            id: "notebook.immunology.compilation.includeQCSummary",
            defaultMessage: "QC Summary (controls passed/failed)",
          })}
          checked={exportOptions.includeQCSummary}
          onChange={(e, { checked }) =>
            setExportOptions((prev) => ({ ...prev, includeQCSummary: checked }))
          }
        />

        <Checkbox
          id="include-visualizations"
          labelText={intl.formatMessage({
            id: "notebook.immunology.compilation.includeVisualizations",
            defaultMessage:
              "Visualizations (standard curves, dot plots, histograms)",
          })}
          checked={exportOptions.includeVisualizations}
          onChange={(e, { checked }) =>
            setExportOptions((prev) => ({
              ...prev,
              includeVisualizations: checked,
            }))
          }
        />
      </Modal>
    </div>
  );
}

export default ImmunologyResultCompilationPage;
