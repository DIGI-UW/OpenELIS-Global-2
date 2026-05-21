import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  TextInput,
  TextArea,
  Modal,
  Checkbox,
  Loading,
} from "@carbon/react";
import { Report, Renew, Archive, ChartBubble } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * BacteriologyReportingDataExportPage - Page 8 of the Bacteriology workflow.
 * AGGREGATION page type - shows summary statistics and generates notebook-level reports.
 *
 * Purpose: Finalize results and generate reports for this notebook.
 *
 * Who uses it:
 * - Lab manager
 * - Data manager
 * - Quality officer
 *
 * Data Points (Bacteriology-specific):
 * - Report Generation: Automatically generates CSV report of all data points
 * - Bacteriology Data Summary: Aggregated stats (read-only)
 * - Archiving: Final results archival with audit trail
 *
 * System Actions:
 * - Final bacteriology report generated (CSV)
 * - Results archived with chain of custody
 * - Full audit trail maintained
 */
function BacteriologyReportingDataExportPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Loading and notification state
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Report generation state
  const [isGeneratingReport, setIsGeneratingReport] = useState(false);

  // Archiving state
  const [showArchiveModal, setShowArchiveModal] = useState(false);
  const [archiveData, setArchiveData] = useState({
    archiveLocation: "",
    archiveNotes: "",
    createBackup: true,
    retentionPeriod: "5 years",
    supervisorSignoff: "",
  });
  const [isArchiving, setIsArchiving] = useState(false);

  // Bacteriology data summary (aggregated from notebook)
  const [dataSummary, setDataSummary] = useState({
    totalCultures: 0,
    positiveIsolates: 0,
    astCompleted: 0,
    mdrOrganisms: 0,
    qcPassRate: "0%",
  });

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load data summary
  useEffect(() => {
    componentMounted.current = true;
    loadDataSummary();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, notebookId]);

  // Load aggregated data summary from notebook
  const loadDataSummary = useCallback(() => {
    if (!notebookId) {
      setLoading(false);
      return;
    }

    setLoading(true);

    // Load summary from notebook aggregation endpoint
    getFromOpenElisServer(
      `/rest/notebook/${notebookId}/summary`,
      (response) => {
        if (componentMounted.current) {
          if (response) {
            setDataSummary({
              totalCultures:
                response.totalCultures || response.totalSamples || 0,
              positiveIsolates: response.positiveIsolates || 0,
              astCompleted: response.astCompleted || 0,
              mdrOrganisms: response.mdrOrganisms || 0,
              qcPassRate: response.qcPassRate || "N/A",
            });
          }
          setLoading(false);
        }
      },
    );
  }, [notebookId]);

  // Helper function to trigger file download
  const downloadFile = useCallback((blob, fileName) => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }, []);

  // Handle generating report - automatically generates CSV report
  const handleGenerateReport = useCallback(() => {
    if (!notebookId) {
      setError("Notebook ID is required to generate report.");
      return;
    }

    setIsGeneratingReport(true);
    setError(null);

    const requestData = {
      notebookId: notebookId,
      reportFormat: "CSV",
      includeAllPages: true,
    };

    fetch(
      `${config.serverBaseUrl}/rest/notebook/${notebookId}/generate-report`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: JSON.stringify(requestData),
      },
    )
      .then((response) => {
        if (!response.ok) {
          return response.json().then((err) => {
            throw new Error(err.error || "Failed to generate report");
          });
        }
        const contentDisposition = response.headers.get("Content-Disposition");
        let fileName = `Bacteriology_Notebook_Report_${new Date().toISOString().split("T")[0]}.csv`;

        if (contentDisposition) {
          const match = contentDisposition.match(/filename="?([^"]+)"?/);
          if (match) fileName = match[1];
        }

        return response.blob().then((blob) => ({ blob, fileName }));
      })
      .then(({ blob, fileName }) => {
        if (componentMounted.current) {
          downloadFile(blob, fileName);
          setIsGeneratingReport(false);
          setSuccess(
            intl.formatMessage({
              id: "notebook.bacteriology.reporting.reportGenerated",
              defaultMessage: "Report generated and downloaded successfully.",
            }),
          );
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        }
      })
      .catch((err) => {
        if (componentMounted.current) {
          setIsGeneratingReport(false);
          setError(err.message || "Failed to generate report.");
        }
      });
  }, [notebookId, downloadFile, onProgressUpdate, intl]);

  // Handle archiving
  const handleArchiveResults = useCallback(() => {
    if (!archiveData.supervisorSignoff) {
      setError("Supervisor sign-off is required for archiving.");
      return;
    }

    if (!hasRealPageId) {
      setShowArchiveModal(false);
      return;
    }

    setIsArchiving(true);

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/archive`,
      JSON.stringify({
        notebookId: notebookId,
        archiveLocation: archiveData.archiveLocation,
        archiveNotes: archiveData.archiveNotes,
        createBackup: archiveData.createBackup,
        retentionPeriod: archiveData.retentionPeriod,
        supervisorSignoff: archiveData.supervisorSignoff,
      }),
      (response) => {
        if (componentMounted.current) {
          setIsArchiving(false);
          if (response && !response.error) {
            setSuccess(
              intl.formatMessage({
                id: "notebook.bacteriology.reporting.archiveSuccess",
                defaultMessage:
                  "Results archived successfully with full audit trail.",
              }),
            );
            setShowArchiveModal(false);
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to archive results.");
          }
        }
      },
    );
  }, [
    archiveData,
    hasRealPageId,
    notebookId,
    pageData?.id,
    onProgressUpdate,
    intl,
  ]);

  return (
    <div className="bacteriology-reporting-data-export-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.reporting.title"
            defaultMessage="Reporting & Data Export"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.reporting.description"
            defaultMessage="Generate reports for all data points in this notebook and archive results."
          />
        </p>
      </div>

      {/* Bacteriology Data Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <h5 style={{ marginBottom: "1rem" }}>
            <ChartBubble size={20} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="notebook.bacteriology.reporting.dataSummary"
              defaultMessage="Bacteriology Data Summary"
            />
          </h5>
          {loading ? (
            <Loading small withOverlay={false} />
          ) : (
            <div className="progress-tiles">
              <Tile className="progress-tile">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.bacteriology.reporting.totalCultures"
                    defaultMessage="Total Cultures"
                  />
                </span>
                <span className="progress-value">
                  {dataSummary.totalCultures}
                </span>
              </Tile>
              <Tile className="progress-tile">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.bacteriology.reporting.positiveIsolates"
                    defaultMessage="Positive Isolates"
                  />
                </span>
                <span className="progress-value">
                  {dataSummary.positiveIsolates}
                </span>
              </Tile>
              <Tile className="progress-tile">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.bacteriology.reporting.astCompleted"
                    defaultMessage="AST Completed"
                  />
                </span>
                <span className="progress-value">
                  {dataSummary.astCompleted}
                </span>
              </Tile>
              <Tile className="progress-tile">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.bacteriology.reporting.mdrOrganisms"
                    defaultMessage="MDR Organisms"
                  />
                </span>
                <span className="progress-value">
                  {dataSummary.mdrOrganisms}
                </span>
              </Tile>
              <Tile className="progress-tile verified">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.bacteriology.reporting.qcPassRate"
                    defaultMessage="QC Pass Rate"
                  />
                </span>
                <span className="progress-value">{dataSummary.qcPassRate}</span>
              </Tile>
            </div>
          )}
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Action Tiles */}
      <Grid fullWidth style={{ padding: "1rem 0" }}>
        {/* Report Generation Section */}
        <Column lg={8} md={4} sm={4}>
          <Tile style={{ height: "100%" }}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                marginBottom: "1rem",
              }}
            >
              <Report size={24} style={{ marginRight: "0.5rem" }} />
              <h5>
                <FormattedMessage
                  id="notebook.bacteriology.reporting.generateReport"
                  defaultMessage="Generate Report"
                />
              </h5>
            </div>
            <p
              style={{
                color: "#525252",
                marginBottom: "1rem",
                fontSize: "0.875rem",
              }}
            >
              <FormattedMessage
                id="notebook.bacteriology.reporting.generateReportDesc"
                defaultMessage="Download a CSV report containing all data points from this notebook."
              />
            </p>
            <Button
              kind="primary"
              renderIcon={Report}
              onClick={handleGenerateReport}
              disabled={isGeneratingReport}
            >
              {isGeneratingReport ? (
                <FormattedMessage
                  id="notebook.bacteriology.reporting.generating"
                  defaultMessage="Generating..."
                />
              ) : (
                <FormattedMessage
                  id="notebook.bacteriology.reporting.generateReportBtn"
                  defaultMessage="Generate Report"
                />
              )}
            </Button>
          </Tile>
        </Column>

        {/* Archive Section */}
        <Column lg={7} md={4} sm={4}>
          <Tile style={{ height: "100%" }}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                marginBottom: "1rem",
              }}
            >
              <Archive size={24} style={{ marginRight: "0.5rem" }} />
              <h5>
                <FormattedMessage
                  id="notebook.bacteriology.reporting.archiveResults"
                  defaultMessage="Archive Results"
                />
              </h5>
            </div>
            <p
              style={{
                color: "#525252",
                marginBottom: "1rem",
                fontSize: "0.875rem",
              }}
            >
              <FormattedMessage
                id="notebook.bacteriology.reporting.archiveResultsDesc"
                defaultMessage="Archive completed results with chain of custody documentation and full audit trail."
              />
            </p>
            <Button
              kind="tertiary"
              renderIcon={Archive}
              onClick={() => setShowArchiveModal(true)}
            >
              <FormattedMessage
                id="notebook.bacteriology.reporting.archiveResultsBtn"
                defaultMessage="Archive Results"
              />
            </Button>
          </Tile>
        </Column>

        {/* Refresh Button */}
        <Column lg={1} md={8} sm={4}>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            onClick={loadDataSummary}
            hasIconOnly
            iconDescription="Refresh"
            tooltipPosition="left"
          />
        </Column>
      </Grid>

      {/* Archive Modal */}
      <Modal
        open={showArchiveModal}
        onRequestClose={() => setShowArchiveModal(false)}
        onRequestSubmit={handleArchiveResults}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.reporting.archiveModal",
          defaultMessage: "Archive Bacteriology Results",
        })}
        primaryButtonText={isArchiving ? "Archiving..." : "Archive Results"}
        secondaryButtonText="Cancel"
        primaryButtonDisabled={isArchiving}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <TextInput
            id="archiveLocation"
            labelText="Archive Location"
            value={archiveData.archiveLocation}
            onChange={(e) =>
              setArchiveData({
                ...archiveData,
                archiveLocation: e.target.value,
              })
            }
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <TextInput
            id="retentionPeriod"
            labelText="Retention Period"
            placeholder="e.g., 5 years, 10 years, indefinite"
            value={archiveData.retentionPeriod}
            onChange={(e) =>
              setArchiveData({
                ...archiveData,
                retentionPeriod: e.target.value,
              })
            }
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <Checkbox
            id="createBackup"
            labelText="Create backup before archiving"
            checked={archiveData.createBackup}
            onChange={(_, { checked }) =>
              setArchiveData({ ...archiveData, createBackup: checked })
            }
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <TextArea
            id="archiveNotes"
            labelText="Archive Notes"
            value={archiveData.archiveNotes}
            onChange={(e) =>
              setArchiveData({ ...archiveData, archiveNotes: e.target.value })
            }
          />
        </div>

        <div style={{ marginTop: "1rem" }}>
          <TextInput
            id="supervisorSignoff"
            labelText="Supervisor Sign-off"
            helperText="Required for archiving"
            value={archiveData.supervisorSignoff}
            onChange={(e) =>
              setArchiveData({
                ...archiveData,
                supervisorSignoff: e.target.value,
              })
            }
            invalid={!archiveData.supervisorSignoff}
            invalidText="Supervisor sign-off is required"
          />
        </div>
      </Modal>
    </div>
  );
}

export default BacteriologyReportingDataExportPage;
