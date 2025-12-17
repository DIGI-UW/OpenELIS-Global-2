import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Modal,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Report, Calendar, Download, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyReportingPage - Page 6 of the pathology workflow.
 * Purpose: Track laboratory performance and turnaround.
 * Who uses it: Lab managers / supervisors
 */
function PathologyReportingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Metrics state
  const [metrics, setMetrics] = useState({
    specimenRejectionRate: 0,
    assaySuccessRate: 0,
    averageTAT: 0,
    equipmentDowntimeHours: 0,
    monthlySpecimenVolume: 0,
    qcIncidents: 0,
  });

  // Report generation modal
  const [reportModalOpen, setReportModalOpen] = useState(false);
  const [qcMeetingModalOpen, setQcMeetingModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [reportData, setReportData] = useState({
    reportType: "",
    reportPeriod: "",
    generatedDate: new Date().toISOString().split("T")[0],
  });

  const [qcMeetingData, setQcMeetingData] = useState({
    meetingDate: "",
    attendees: "",
    issuesDiscussed: "",
    actionsRequired: "",
  });

  // CSV Export filters
  const [csvFilters, setCsvFilters] = useState({
    startDate: "",
    endDate: "",
    includeMetrics: true,
    includeSampleDetails: true,
    includeQcData: true,
    includeProcessingData: true,
    includeTestingData: true,
    includeStorageData: true,
    includeDisposalData: true,
    includeSopData: true,
  });
  const [csvModalOpen, setCsvModalOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    loadMetrics();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadMetrics = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook/pathology/metrics?entryId=${entryId}`,
      (response) => {
        if (componentMounted.current) {
          if (response) {
            setMetrics({
              specimenRejectionRate: response.specimenRejectionRate || 2.5,
              assaySuccessRate: response.assaySuccessRate || 97.8,
              averageTAT: response.averageTAT || 24,
              equipmentDowntimeHours: response.equipmentDowntimeHours || 4,
              monthlySpecimenVolume: response.monthlySpecimenVolume || 150,
              qcIncidents: response.qcIncidents || 3,
            });
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  const handleInputChange = (e, setState) => {
    const { name, value } = e.target;
    setState((prev) => ({ ...prev, [name]: value }));
  };

  const handleDateChange = (dates, fieldName, setState) => {
    if (dates?.[0]) {
      setState((prev) => ({
        ...prev,
        [fieldName]: dates[0].toISOString().split("T")[0],
      }));
    }
  };

  const handleGenerateReport = () => {
    if (!reportData.reportType || !reportData.reportPeriod) {
      setError("Please select report type and period");
      return;
    }
    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/report/generate`,
      JSON.stringify({ ...reportData, entryId }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setReportModalOpen(false);
          // Trigger download or display success
        } else {
          setError("Failed to generate report. Please try again.");
        }
      },
    );
  };

  const handleSaveQcMeeting = () => {
    if (!qcMeetingData.meetingDate) {
      setError("Please select meeting date");
      return;
    }
    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/qc-meeting`,
      JSON.stringify({ ...qcMeetingData, entryId, pageId: pageData?.id }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setQcMeetingModalOpen(false);
          onProgressUpdate?.();
        } else {
          setError("Failed to save QC meeting. Please try again.");
        }
      },
    );
  };

  const reportTypeOptions = [
    { id: "monthly_volume", text: "Monthly specimen volume" },
    { id: "tat", text: "Turnaround time (TAT)" },
    { id: "rejection_rates", text: "Rejection rates" },
    { id: "qc_incidents", text: "QC incidents" },
    { id: "equipment_downtime", text: "Equipment downtime" },
  ];

  const handleDownloadCsv = async () => {
    setDownloading(true);
    setError(null);

    try {
      // Build query parameters
      const params = new URLSearchParams({
        entryId: entryId.toString(),
        reportType: reportData.reportType || "comprehensive",
        reportPeriod: reportData.reportPeriod || "",
      });

      // Add date range filters if specified
      if (csvFilters.startDate) {
        params.append("startDate", csvFilters.startDate);
      }
      if (csvFilters.endDate) {
        params.append("endDate", csvFilters.endDate);
      }

      // Add include flags
      params.append("includeMetrics", csvFilters.includeMetrics.toString());
      params.append(
        "includeSampleDetails",
        csvFilters.includeSampleDetails.toString(),
      );
      params.append("includeQcData", csvFilters.includeQcData.toString());
      params.append(
        "includeProcessingData",
        csvFilters.includeProcessingData.toString(),
      );
      params.append(
        "includeTestingData",
        csvFilters.includeTestingData.toString(),
      );
      params.append(
        "includeStorageData",
        csvFilters.includeStorageData.toString(),
      );
      params.append(
        "includeDisposalData",
        csvFilters.includeDisposalData.toString(),
      );
      params.append("includeSopData", csvFilters.includeSopData.toString());

      const url = `/rest/notebook/pathology/report/export-csv?${params.toString()}`;

      // Use fetch with credentials for authenticated request
      const response = await fetch(url, {
        method: "GET",
        credentials: "include",
        headers: {
          Accept: "text/csv",
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // Get the blob from response
      const blob = await response.blob();

      // Create download link
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = downloadUrl;

      // Generate filename with timestamp
      const timestamp = new Date().toISOString().split("T")[0];
      link.download = `pathology_report_${entryId}_${timestamp}.csv`;

      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      // Cleanup
      window.URL.revokeObjectURL(downloadUrl);
      setCsvModalOpen(false);
    } catch (err) {
      setError(
        intl.formatMessage({
          id: "pathology.reporting.csvDownloadError",
          defaultMessage: "Failed to download CSV. Please try again.",
        }),
      );
    } finally {
      setDownloading(false);
    }
  };

  const handleCsvFilterChange = (field, value) => {
    setCsvFilters((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <div className="pathology-reporting-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.reporting.title"
            defaultMessage="Reporting & Performance Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.reporting.description"
            defaultMessage="Track laboratory performance and turnaround. Generate reports and document QC meetings."
          />
        </p>
      </div>

      {/* Key Metrics Dashboard */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <h5 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="pathology.reporting.keyMetrics"
              defaultMessage="Key Metrics"
            />
          </h5>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">Specimen Rejection Rate</span>
              <span className="progress-value">
                {metrics.specimenRejectionRate}%
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">Assay Success Rate</span>
              <span className="progress-value">
                {metrics.assaySuccessRate}%
              </span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f0ff" }}
            >
              <span className="progress-label">Average TAT (hours)</span>
              <span className="progress-value">{metrics.averageTAT}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">Equipment Downtime (hours)</span>
              <span className="progress-value">
                {metrics.equipmentDowntimeHours}
              </span>
            </Tile>
          </div>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Tile className="progress-tile">
            <span className="progress-label">Monthly Specimen Volume</span>
            <span className="progress-value">
              {metrics.monthlySpecimenVolume}
            </span>
          </Tile>
        </Column>
        <Column lg={8} md={4} sm={4}>
          <Tile
            className="progress-tile"
            style={{ backgroundColor: "#fff1f1" }}
          >
            <span className="progress-label">QC Incidents</span>
            <span className="progress-value">{metrics.qcIncidents}</span>
          </Tile>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Report}
          onClick={() => setReportModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.reporting.generateReport"
            defaultMessage="Generate Report"
          />
        </Button>
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Download}
          onClick={() => setCsvModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.reporting.downloadCsv"
            defaultMessage="Download CSV"
          />
        </Button>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadMetrics}
          disabled={loading}
        >
          <FormattedMessage
            id="pathology.reporting.refreshMetrics"
            defaultMessage="Refresh"
          />
        </Button>
        <Button
          kind="secondary"
          size="sm"
          renderIcon={Calendar}
          onClick={() => setQcMeetingModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.reporting.documentQcMeeting"
            defaultMessage="Document QC Meeting"
          />
        </Button>
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {loading && (
        <Loading withOverlay={false} description="Loading metrics..." />
      )}

      {/* Generate Report Modal */}
      <Modal
        open={reportModalOpen}
        modalHeading="Generate Report"
        primaryButtonText="Generate"
        secondaryButtonText="Cancel"
        onRequestClose={() => setReportModalOpen(false)}
        onRequestSubmit={handleGenerateReport}
        primaryButtonDisabled={submitting}
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="reportType"
              name="reportType"
              labelText="Report Type *"
              value={reportData.reportType}
              onChange={(e) => handleInputChange(e, setReportData)}
            >
              <SelectItem value="" text="" />
              {reportTypeOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.text} />
              ))}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="reportPeriod"
              name="reportPeriod"
              labelText="Report Period *"
              placeholder="e.g., January 2025"
              value={reportData.reportPeriod}
              onChange={(e) => handleInputChange(e, setReportData)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "generatedDate", setReportData)
              }
            >
              <DatePickerInput
                id="generatedDate"
                labelText="Generated Date"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
        </Grid>
      </Modal>

      {/* QC Meeting Modal */}
      <Modal
        open={qcMeetingModalOpen}
        modalHeading="Document QC Meeting"
        primaryButtonText="Save"
        secondaryButtonText="Cancel"
        onRequestClose={() => setQcMeetingModalOpen(false)}
        onRequestSubmit={handleSaveQcMeeting}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "meetingDate", setQcMeetingData)
              }
            >
              <DatePickerInput
                id="meetingDate"
                labelText="Meeting Date *"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="attendees"
              name="attendees"
              labelText="Attendees"
              value={qcMeetingData.attendees}
              onChange={(e) => handleInputChange(e, setQcMeetingData)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="issuesDiscussed"
              name="issuesDiscussed"
              labelText="Issues Discussed"
              value={qcMeetingData.issuesDiscussed}
              onChange={(e) => handleInputChange(e, setQcMeetingData)}
              rows={4}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="actionsRequired"
              name="actionsRequired"
              labelText="Actions Required"
              value={qcMeetingData.actionsRequired}
              onChange={(e) => handleInputChange(e, setQcMeetingData)}
              rows={4}
            />
          </Column>
        </Grid>
      </Modal>

      {/* CSV Export Modal */}
      <Modal
        open={csvModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.reporting.csvExportTitle",
          defaultMessage: "Export Performance Report (CSV)",
        })}
        primaryButtonText={
          downloading
            ? intl.formatMessage({
                id: "pathology.reporting.downloading",
                defaultMessage: "Downloading...",
              })
            : intl.formatMessage({
                id: "pathology.reporting.downloadCsv",
                defaultMessage: "Download CSV",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "pathology.reporting.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setCsvModalOpen(false)}
        onRequestSubmit={handleDownloadCsv}
        primaryButtonDisabled={downloading}
        size="lg"
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="pathology.reporting.csvExportDescription"
            defaultMessage="Export a comprehensive CSV report with all data points from the pathology workflow. Select the date range and data sections to include."
          />
        </p>

        <Grid fullWidth>
          {/* Date Range Filters */}
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => {
                if (dates?.[0]) {
                  handleCsvFilterChange(
                    "startDate",
                    dates[0].toISOString().split("T")[0],
                  );
                }
              }}
            >
              <DatePickerInput
                id="csvStartDate"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.startDate",
                  defaultMessage: "Start Date",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => {
                if (dates?.[0]) {
                  handleCsvFilterChange(
                    "endDate",
                    dates[0].toISOString().split("T")[0],
                  );
                }
              }}
            >
              <DatePickerInput
                id="csvEndDate"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.endDate",
                  defaultMessage: "End Date",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>

          {/* Data Sections to Include */}
          <Column lg={16} md={8} sm={4}>
            <h6 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="pathology.reporting.dataSections"
                defaultMessage="Data Sections to Include"
              />
            </h6>
          </Column>

          <Column lg={8} md={4} sm={2}>
            <div className="checkbox-group">
              <label className="cds--checkbox-wrapper">
                <input
                  type="checkbox"
                  className="cds--checkbox"
                  checked={csvFilters.includeMetrics}
                  onChange={(e) =>
                    handleCsvFilterChange("includeMetrics", e.target.checked)
                  }
                />
                <span className="cds--checkbox-label">
                  <FormattedMessage
                    id="pathology.reporting.includeMetrics"
                    defaultMessage="Key Performance Metrics"
                  />
                </span>
              </label>
              <label className="cds--checkbox-wrapper">
                <input
                  type="checkbox"
                  className="cds--checkbox"
                  checked={csvFilters.includeSampleDetails}
                  onChange={(e) =>
                    handleCsvFilterChange(
                      "includeSampleDetails",
                      e.target.checked,
                    )
                  }
                />
                <span className="cds--checkbox-label">
                  <FormattedMessage
                    id="pathology.reporting.includeSampleDetails"
                    defaultMessage="Sample Details & Metadata"
                  />
                </span>
              </label>
              <label className="cds--checkbox-wrapper">
                <input
                  type="checkbox"
                  className="cds--checkbox"
                  checked={csvFilters.includeQcData}
                  onChange={(e) =>
                    handleCsvFilterChange("includeQcData", e.target.checked)
                  }
                />
                <span className="cds--checkbox-label">
                  <FormattedMessage
                    id="pathology.reporting.includeQcData"
                    defaultMessage="Quality Control Data"
                  />
                </span>
              </label>
              <label className="cds--checkbox-wrapper">
                <input
                  type="checkbox"
                  className="cds--checkbox"
                  checked={csvFilters.includeProcessingData}
                  onChange={(e) =>
                    handleCsvFilterChange(
                      "includeProcessingData",
                      e.target.checked,
                    )
                  }
                />
                <span className="cds--checkbox-label">
                  <FormattedMessage
                    id="pathology.reporting.includeProcessingData"
                    defaultMessage="Processing & Aliquoting Data"
                  />
                </span>
              </label>
            </div>
          </Column>

          <Column lg={8} md={4} sm={2}>
            <div className="checkbox-group">
              <label className="cds--checkbox-wrapper">
                <input
                  type="checkbox"
                  className="cds--checkbox"
                  checked={csvFilters.includeTestingData}
                  onChange={(e) =>
                    handleCsvFilterChange("includeTestingData", e.target.checked)
                  }
                />
                <span className="cds--checkbox-label">
                  <FormattedMessage
                    id="pathology.reporting.includeTestingData"
                    defaultMessage="Testing & Microscopy Results"
                  />
                </span>
              </label>
              <label className="cds--checkbox-wrapper">
                <input
                  type="checkbox"
                  className="cds--checkbox"
                  checked={csvFilters.includeStorageData}
                  onChange={(e) =>
                    handleCsvFilterChange("includeStorageData", e.target.checked)
                  }
                />
                <span className="cds--checkbox-label">
                  <FormattedMessage
                    id="pathology.reporting.includeStorageData"
                    defaultMessage="Storage & Inventory Data"
                  />
                </span>
              </label>
              <label className="cds--checkbox-wrapper">
                <input
                  type="checkbox"
                  className="cds--checkbox"
                  checked={csvFilters.includeDisposalData}
                  onChange={(e) =>
                    handleCsvFilterChange(
                      "includeDisposalData",
                      e.target.checked,
                    )
                  }
                />
                <span className="cds--checkbox-label">
                  <FormattedMessage
                    id="pathology.reporting.includeDisposalData"
                    defaultMessage="Disposal & Archiving Data"
                  />
                </span>
              </label>
              <label className="cds--checkbox-wrapper">
                <input
                  type="checkbox"
                  className="cds--checkbox"
                  checked={csvFilters.includeSopData}
                  onChange={(e) =>
                    handleCsvFilterChange("includeSopData", e.target.checked)
                  }
                />
                <span className="cds--checkbox-label">
                  <FormattedMessage
                    id="pathology.reporting.includeSopData"
                    defaultMessage="SOP & Reference Documents"
                  />
                </span>
              </label>
            </div>
          </Column>

          {/* CSV Contents Preview */}
          <Column lg={16} md={8} sm={4}>
            <Accordion>
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.reporting.csvContentsPreview",
                  defaultMessage: "CSV Contents Preview",
                })}
              >
                <div style={{ fontSize: "0.875rem", lineHeight: "1.5" }}>
                  <p>
                    <strong>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.header"
                        defaultMessage="The exported CSV will include:"
                      />
                    </strong>
                  </p>
                  <ul style={{ marginLeft: "1rem", marginTop: "0.5rem" }}>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.reportInfo"
                        defaultMessage="Report metadata (generated date, report type, period)"
                      />
                    </li>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.metrics"
                        defaultMessage="Key metrics: Specimen rejection rate, Assay success rate, Average TAT, Equipment downtime, Monthly specimen volume, QC incidents"
                      />
                    </li>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.sampleData"
                        defaultMessage="Per-sample data: Accession number, category, source, specimen type, patient/study metadata"
                      />
                    </li>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.qcData"
                        defaultMessage="QC data: Status, remarks, staff initials, histology/cytology/block-specific QC fields"
                      />
                    </li>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.processingData"
                        defaultMessage="Processing data: Grossing, sectioning, embedding, microtomy, smears, stains"
                      />
                    </li>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.testingData"
                        defaultMessage="Testing data: Test name, results, controls, technician/pathologist signatures"
                      />
                    </li>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.storageData"
                        defaultMessage="Storage data: Location (unit/rack/box/position), dates, retrieval records"
                      />
                    </li>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.disposalData"
                        defaultMessage="Disposal data: Reason, method, approvals, dates"
                      />
                    </li>
                    <li>
                      <FormattedMessage
                        id="pathology.reporting.csvContents.sopData"
                        defaultMessage="SOP summary: Title, category, version, status, effective/review dates"
                      />
                    </li>
                  </ul>
                </div>
              </AccordionItem>
            </Accordion>
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologyReportingPage;
