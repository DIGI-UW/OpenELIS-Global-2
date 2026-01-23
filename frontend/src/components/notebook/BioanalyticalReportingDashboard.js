import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Card,
  Button,
  Loading,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./notebook/pages/bioanalytical/BioanalyticalPages.css";

/**
 * BioanalyticalReportingDashboard - Central metrics and reporting dashboard
 * for Bioanalytical & Bioequivalence Laboratory workflow.
 *
 * Features:
 * - Overall throughput and sample completion metrics
 * - Quality assurance indicators (QC pass rates, calibration r²)
 * - Bioequivalence study progress tracking
 * - Analytical instrument utilization metrics
 * - Turnaround time by test type
 * - Real-time alerts and anomalies
 * - Export and reporting options
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {function} props.onNavigateToPage - Callback to navigate to specific page
 */
function BioanalyticalReportingDashboard({ entryId, onNavigateToPage }) {
  const intl = useIntl();

  const [isLoading, setIsLoading] = useState(true);
  const [dashboardMetrics, setDashboardMetrics] = useState(null);
  const [alerts, setAlerts] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");

  // Load dashboard data
  useEffect(() => {
    setIsLoading(true);

    setTimeout(() => {
      // Mock dashboard metrics
      const mockMetrics = {
        throughput: {
          totalSamples: 24,
          samplesProcessed: 24,
          samplesInProgress: 0,
          completionPercentage: 100,
          averageTat: "5 days",
        },
        quality: {
          qcPassRate: 100,
          calibrationRSquared: 0.9987,
          acceptedResults: 24,
          rejectedResults: 0,
          westgardViolations: 0,
        },
        studyProgress: {
          totalSubjects: 12,
          samplesCollected: 24,
          analysisComplete: 24,
          qaApproved: 24,
          readyForReporting: 24,
          completionPercentage: 100,
        },
        instrumentUtilization: {
          lcmsRuns: 48,
          lcmsUtilization: 78,
          hplcRuns: 12,
          hplcUtilization: 45,
          dissolutionRuns: 6,
          dissolutionUtilization: 23,
        },
        tatByTest: [
          {
            testName: "Analyte X - Plasma",
            samples: 12,
            avgDays: 4.5,
            minDays: 3,
            maxDays: 6,
          },
          {
            testName: "Analyte X - Urine",
            samples: 12,
            avgDays: 5.2,
            minDays: 4,
            maxDays: 7,
          },
          {
            testName: "Metabolite Y - Plasma",
            samples: 6,
            avgDays: 4.8,
            minDays: 3,
            maxDays: 6,
          },
        ],
      };

      setDashboardMetrics(mockMetrics);

      // Mock alerts
      const mockAlerts = [
        {
          id: "1",
          severity: "INFO",
          message: "All 24 samples have completed analysis",
          timestamp: new Date().toLocaleString(),
        },
        {
          id: "2",
          severity: "INFO",
          message: "QA review 100% complete - ready for external reporting",
          timestamp: new Date().toLocaleString(),
        },
        {
          id: "3",
          severity: "INFO",
          message: "Bioequivalence study within expected timeframe",
          timestamp: new Date().toLocaleString(),
        },
      ];

      setAlerts(mockAlerts);
      setIsLoading(false);
    }, 1500);
  }, [entryId]);

  if (isLoading) {
    return (
      <div style={{ padding: "2rem" }}>
        <Loading description="Loading dashboard metrics..." />
      </div>
    );
  }

  if (!dashboardMetrics) {
    return (
      <div style={{ padding: "2rem" }}>
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notebook.bioanalytical.dashboard.error",
            defaultMessage: "Error",
          })}
          subtitle={intl.formatMessage({
            id: "notebook.bioanalytical.dashboard.loadError",
            defaultMessage: "Failed to load dashboard metrics",
          })}
          lowContrast
        />
      </div>
    );
  }

  const getAlertColor = (severity) => {
    switch (severity) {
      case "CRITICAL":
        return "#da1e28";
      case "WARNING":
        return "#f1c21b";
      case "INFO":
        return "#0043ce";
      default:
        return "#525252";
    }
  };

  return (
    <div style={{ padding: "2rem" }}>
      <div style={{ marginBottom: "2rem" }}>
        <h2 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="notebook.bioanalytical.dashboard.title"
            defaultMessage="Bioanalytical Laboratory Dashboard"
          />
        </h2>
        <p style={{ color: "#525252", fontSize: "0.875rem" }}>
          <FormattedMessage
            id="notebook.bioanalytical.dashboard.subtitle"
            defaultMessage="Real-time metrics, quality indicators, and workflow progress tracking"
          />
        </p>
      </div>

      {/* Alerts Section */}
      {alerts.length > 0 && (
        <div style={{ marginBottom: "2rem" }}>
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <div style={{ marginBottom: "1rem" }}>
                <h4>
                  <FormattedMessage
                    id="notebook.bioanalytical.dashboard.alerts"
                    defaultMessage="System Alerts"
                  />
                </h4>
              </div>
              {alerts.map((alert) => (
                <div key={alert.id} style={{ marginBottom: "0.5rem" }}>
                  <InlineNotification
                    kind={
                      alert.severity === "CRITICAL"
                        ? "error"
                        : alert.severity === "WARNING"
                          ? "warning"
                          : "info"
                    }
                    title={alert.message}
                    subtitle={alert.timestamp}
                    lowContrast
                    hideCloseButton={true}
                  />
                </div>
              ))}
            </Column>
          </Grid>
        </div>
      )}

      {/* Key Metrics Cards */}
      <Grid style={{ marginBottom: "2rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Card
            style={{
              padding: "1.5rem",
              backgroundColor: "#e7f1f5",
              borderLeft: "4px solid #0043ce",
            }}
          >
            <p style={{ fontSize: "0.875rem", color: "#525252", margin: 0 }}>
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.completion"
                defaultMessage="Workflow Completion"
              />
            </p>
            <h3
              style={{
                fontSize: "2rem",
                fontWeight: "bold",
                color: "#161616",
                margin: "0.5rem 0 0 0",
              }}
            >
              {dashboardMetrics.throughput.completionPercentage}%
            </h3>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                margin: "0.5rem 0 0 0",
              }}
            >
              {dashboardMetrics.throughput.samplesProcessed} /{" "}
              {dashboardMetrics.throughput.totalSamples}{" "}
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.samples"
                defaultMessage="samples"
              />
            </p>
          </Card>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <Card
            style={{
              padding: "1.5rem",
              backgroundColor: "#d0e2d4",
              borderLeft: "4px solid #24a148",
            }}
          >
            <p style={{ fontSize: "0.875rem", color: "#525252", margin: 0 }}>
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.qcPassRate"
                defaultMessage="QC Pass Rate"
              />
            </p>
            <h3
              style={{
                fontSize: "2rem",
                fontWeight: "bold",
                color: "#161616",
                margin: "0.5rem 0 0 0",
              }}
            >
              {dashboardMetrics.quality.qcPassRate}%
            </h3>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                margin: "0.5rem 0 0 0",
              }}
            >
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.westgardCompliant"
                defaultMessage="Westgard Rules Compliant"
              />
            </p>
          </Card>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <Card
            style={{
              padding: "1.5rem",
              backgroundColor: "#e8d9f4",
              borderLeft: "4px solid #8f2ba3",
            }}
          >
            <p style={{ fontSize: "0.875rem", color: "#525252", margin: 0 }}>
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.avgTurnaroundTime"
                defaultMessage="Avg Turnaround Time"
              />
            </p>
            <h3
              style={{
                fontSize: "2rem",
                fontWeight: "bold",
                color: "#161616",
                margin: "0.5rem 0 0 0",
              }}
            >
              {dashboardMetrics.throughput.averageTat}
            </h3>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                margin: "0.5rem 0 0 0",
              }}
            >
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.fromCollectionToReport"
                defaultMessage="From collection to report"
              />
            </p>
          </Card>
        </Column>
      </Grid>

      {/* Quality Indicators */}
      <Grid style={{ marginBottom: "2rem" }}>
        <Column lg={8} md={8} sm={4}>
          <div
            style={{
              padding: "1.5rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h4 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.qualityMetrics"
                defaultMessage="Quality Metrics"
              />
            </h4>

            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <tbody>
                <tr>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                    }}
                  >
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.calibrationRSquared"
                        defaultMessage="Calibration R²:"
                      />
                    </strong>
                  </td>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                      textAlign: "right",
                    }}
                  >
                    <span style={{ fontWeight: "bold", color: "#24a148" }}>
                      {dashboardMetrics.quality.calibrationRSquared.toFixed(4)}
                    </span>
                    <span
                      style={{
                        fontSize: "0.75rem",
                        color: "#525252",
                        marginLeft: "0.5rem",
                      }}
                    >
                      ✓ PASS
                    </span>
                  </td>
                </tr>
                <tr>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                    }}
                  >
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.acceptedResults"
                        defaultMessage="Accepted Results:"
                      />
                    </strong>
                  </td>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                      textAlign: "right",
                    }}
                  >
                    <span style={{ fontWeight: "bold", color: "#24a148" }}>
                      {dashboardMetrics.quality.acceptedResults}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                    }}
                  >
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.rejectedResults"
                        defaultMessage="Rejected Results:"
                      />
                    </strong>
                  </td>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                      textAlign: "right",
                    }}
                  >
                    <span
                      style={{
                        fontWeight: "bold",
                        color:
                          dashboardMetrics.quality.rejectedResults > 0
                            ? "#da1e28"
                            : "#24a148",
                      }}
                    >
                      {dashboardMetrics.quality.rejectedResults}
                    </span>
                  </td>
                </tr>
                <tr>
                  <td style={{ padding: "0.75rem" }}>
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.westgardViolations"
                        defaultMessage="Westgard Violations:"
                      />
                    </strong>
                  </td>
                  <td style={{ padding: "0.75rem", textAlign: "right" }}>
                    <span
                      style={{
                        fontWeight: "bold",
                        color:
                          dashboardMetrics.quality.westgardViolations > 0
                            ? "#da1e28"
                            : "#24a148",
                      }}
                    >
                      {dashboardMetrics.quality.westgardViolations}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Column>

        <Column lg={8} md={8} sm={4}>
          <div
            style={{
              padding: "1.5rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h4 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.studyProgress"
                defaultMessage="Bioequivalence Study Progress"
              />
            </h4>

            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <tbody>
                <tr>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                    }}
                  >
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.totalSubjects"
                        defaultMessage="Total Subjects:"
                      />
                    </strong>
                  </td>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                      textAlign: "right",
                    }}
                  >
                    {dashboardMetrics.studyProgress.totalSubjects}
                  </td>
                </tr>
                <tr>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                    }}
                  >
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.samplesCollected"
                        defaultMessage="Samples Collected:"
                      />
                    </strong>
                  </td>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                      textAlign: "right",
                    }}
                  >
                    {dashboardMetrics.studyProgress.samplesCollected}
                  </td>
                </tr>
                <tr>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                    }}
                  >
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.analysisComplete"
                        defaultMessage="Analysis Complete:"
                      />
                    </strong>
                  </td>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                      textAlign: "right",
                    }}
                  >
                    {dashboardMetrics.studyProgress.analysisComplete}
                  </td>
                </tr>
                <tr>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                    }}
                  >
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.qaApproved"
                        defaultMessage="QA Approved:"
                      />
                    </strong>
                  </td>
                  <td
                    style={{
                      padding: "0.75rem",
                      borderBottom: "1px solid #e0e0e0",
                      textAlign: "right",
                    }}
                  >
                    {dashboardMetrics.studyProgress.qaApproved}
                  </td>
                </tr>
                <tr>
                  <td style={{ padding: "0.75rem" }}>
                    <strong style={{ fontSize: "0.875rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.dashboard.readyForReporting"
                        defaultMessage="Ready for Reporting:"
                      />
                    </strong>
                  </td>
                  <td style={{ padding: "0.75rem", textAlign: "right" }}>
                    {dashboardMetrics.studyProgress.readyForReporting}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Column>
      </Grid>

      {/* Turnaround Time by Test */}
      <Grid style={{ marginBottom: "2rem" }}>
        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              padding: "1.5rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h4 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.tatByTest"
                defaultMessage="Turnaround Time by Test"
              />
            </h4>

            <Table>
              <TableHead>
                <TableRow>
                  <TableHeader>
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.testName"
                      defaultMessage="Test Name"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.samples"
                      defaultMessage="Samples"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.avgDays"
                      defaultMessage="Average (days)"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.minDays"
                      defaultMessage="Min (days)"
                    />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.maxDays"
                      defaultMessage="Max (days)"
                    />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {dashboardMetrics.tatByTest.map((tat, idx) => (
                  <TableRow key={idx}>
                    <TableCell>{tat.testName}</TableCell>
                    <TableCell>{tat.samples}</TableCell>
                    <TableCell>{tat.avgDays}</TableCell>
                    <TableCell>{tat.minDays}</TableCell>
                    <TableCell>{tat.maxDays}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </Column>
      </Grid>

      {/* Instrument Utilization */}
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              padding: "1.5rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <h4 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.bioanalytical.dashboard.instrumentUtilization"
                defaultMessage="Instrument Utilization"
              />
            </h4>

            <Grid>
              <Column lg={5} md={4} sm={4} style={{ marginRight: "1rem" }}>
                <div style={{ marginBottom: "1.5rem" }}>
                  <p
                    style={{
                      fontSize: "0.875rem",
                      fontWeight: "bold",
                      margin: "0 0 0.5rem 0",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.lcms"
                      defaultMessage="LC-MS/MS System"
                    />
                  </p>
                  <p style={{ fontSize: "0.875rem", margin: 0 }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.runs"
                      defaultMessage="Runs: {count}"
                      values={{
                        count: dashboardMetrics.instrumentUtilization.lcmsRuns,
                      }}
                    />
                  </p>
                  <div
                    style={{
                      marginTop: "0.5rem",
                      backgroundColor: "#e0e0e0",
                      height: "8px",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        backgroundColor: "#0043ce",
                        height: "100%",
                        width: `${dashboardMetrics.instrumentUtilization.lcmsUtilization}%`,
                      }}
                    />
                  </div>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      margin: "0.25rem 0 0 0",
                    }}
                  >
                    {dashboardMetrics.instrumentUtilization.lcmsUtilization}%{" "}
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.utilization"
                      defaultMessage="utilization"
                    />
                  </p>
                </div>
              </Column>

              <Column lg={5} md={4} sm={4} style={{ marginRight: "1rem" }}>
                <div style={{ marginBottom: "1.5rem" }}>
                  <p
                    style={{
                      fontSize: "0.875rem",
                      fontWeight: "bold",
                      margin: "0 0 0.5rem 0",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.hplc"
                      defaultMessage="HPLC System"
                    />
                  </p>
                  <p style={{ fontSize: "0.875rem", margin: 0 }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.runs"
                      defaultMessage="Runs: {count}"
                      values={{
                        count: dashboardMetrics.instrumentUtilization.hplcRuns,
                      }}
                    />
                  </p>
                  <div
                    style={{
                      marginTop: "0.5rem",
                      backgroundColor: "#e0e0e0",
                      height: "8px",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        backgroundColor: "#8f2ba3",
                        height: "100%",
                        width: `${dashboardMetrics.instrumentUtilization.hplcUtilization}%`,
                      }}
                    />
                  </div>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      margin: "0.25rem 0 0 0",
                    }}
                  >
                    {dashboardMetrics.instrumentUtilization.hplcUtilization}%{" "}
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.utilization"
                      defaultMessage="utilization"
                    />
                  </p>
                </div>
              </Column>

              <Column lg={5} md={4} sm={4}>
                <div style={{ marginBottom: "1.5rem" }}>
                  <p
                    style={{
                      fontSize: "0.875rem",
                      fontWeight: "bold",
                      margin: "0 0 0.5rem 0",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.dissolution"
                      defaultMessage="Dissolution Tester"
                    />
                  </p>
                  <p style={{ fontSize: "0.875rem", margin: 0 }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.runs"
                      defaultMessage="Runs: {count}"
                      values={{
                        count:
                          dashboardMetrics.instrumentUtilization
                            .dissolutionRuns,
                      }}
                    />
                  </p>
                  <div
                    style={{
                      marginTop: "0.5rem",
                      backgroundColor: "#e0e0e0",
                      height: "8px",
                      borderRadius: "4px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        backgroundColor: "#f1c21b",
                        height: "100%",
                        width: `${dashboardMetrics.instrumentUtilization.dissolutionUtilization}%`,
                      }}
                    />
                  </div>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      margin: "0.25rem 0 0 0",
                    }}
                  >
                    {
                      dashboardMetrics.instrumentUtilization
                        .dissolutionUtilization
                    }
                    %{" "}
                    <FormattedMessage
                      id="notebook.bioanalytical.dashboard.utilization"
                      defaultMessage="utilization"
                    />
                  </p>
                </div>
              </Column>
            </Grid>
          </div>
        </Column>
      </Grid>
    </div>
  );
}

export default BioanalyticalReportingDashboard;
