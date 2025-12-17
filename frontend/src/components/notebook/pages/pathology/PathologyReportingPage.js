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
} from "@carbon/react";
import { Report, ChartLine, Calendar, Download } from "@carbon/react/icons";
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

  const handleDownloadCsv = () => {
    const reportType = reportData.reportType || "comprehensive";
    const reportPeriod = reportData.reportPeriod || "";
    const url = `/rest/notebook/pathology/report/export-csv?entryId=${entryId}&reportType=${encodeURIComponent(reportType)}&reportPeriod=${encodeURIComponent(reportPeriod)}`;

    // Create a temporary link to trigger the download
    const link = document.createElement("a");
    link.href = url;
    link.download = `pathology_report_${entryId}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
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
          onClick={handleDownloadCsv}
        >
          <FormattedMessage
            id="pathology.reporting.downloadCsv"
            defaultMessage="Download CSV"
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
    </div>
  );
}

export default PathologyReportingPage;
