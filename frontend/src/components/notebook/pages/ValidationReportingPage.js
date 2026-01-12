import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
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
  Tag,
  TextArea,
  TextInput,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  NumberInput,
  ProgressBar,
} from "@carbon/react";
import {
  Checkmark,
  Close,
  DocumentPdf,
  Email,
  Printer,
  Analytics,
  ChartLine,
  Time,
  CheckmarkFilled,
  WarningAlt,
  Settings,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import "../workflow/NotebookWorkflow.css";

/**
 * ValidationReportingPage - Validation, Reporting & Performance Monitoring
 * Combines result verification, report delivery, and performance dashboards.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ValidationReportingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Active tab
  const [activeTab, setActiveTab] = useState(0);

  // Validation state
  const [samplesForVerification, setSamplesForVerification] = useState([]);
  const [loadingVerification, setLoadingVerification] = useState(true);

  // Reporting state
  const [samplesForReporting, setSamplesForReporting] = useState([]);
  const [loadingReporting, setLoadingReporting] = useState(true);

  // Performance dashboard state
  const [dashboardData, setDashboardData] = useState(null);
  const [loadingDashboard, setLoadingDashboard] = useState(true);

  // Error state
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Modals
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [deliveryModalOpen, setDeliveryModalOpen] = useState(false);
  const [refRangeModalOpen, setRefRangeModalOpen] = useState(false);

  // Selection
  const [selectedSample, setSelectedSample] = useState(null);
  const [selectedTest, setSelectedTest] = useState(null);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Form state
  const [rejectionComments, setRejectionComments] = useState("");
  const [deliveryMethod, setDeliveryMethod] = useState("print");
  const [recipient, setRecipient] = useState("");

  // Reference range modification
  const [refRangeTestId, setRefRangeTestId] = useState(null);
  const [refRangeLowNormal, setRefRangeLowNormal] = useState("");
  const [refRangeHighNormal, setRefRangeHighNormal] = useState("");
  const [refRangeLowCritical, setRefRangeLowCritical] = useState("");
  const [refRangeHighCritical, setRefRangeHighCritical] = useState("");

  // Processing state
  const [verifying, setVerifying] = useState(false);
  const [completing, setCompleting] = useState(false);

  // Load verification samples
  const loadSamplesForVerification = useCallback(() => {
    if (!entryId) {
      setLoadingVerification(false);
      return;
    }

    setLoadingVerification(true);
    const url = `/rest/medlab/entry/${entryId}/results-for-verification`;

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setSamplesForVerification(response);
        } else {
          setSamplesForVerification([]);
        }
        setLoadingVerification(false);
      }
    });
  }, [entryId]);

  // Load reporting samples
  const loadSamplesForReporting = useCallback(() => {
    if (!entryId) {
      setLoadingReporting(false);
      return;
    }

    setLoadingReporting(true);
    const url = `/rest/medlab/entry/${entryId}/results-for-reporting`;

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setSamplesForReporting(response);
        } else {
          setSamplesForReporting([]);
        }
        setLoadingReporting(false);
      }
    });
  }, [entryId]);

  // Load performance dashboard
  const loadDashboard = useCallback(() => {
    if (!entryId) {
      setLoadingDashboard(false);
      return;
    }

    setLoadingDashboard(true);
    const url = `/rest/medlab/entry/${entryId}/performance-dashboard`;

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && response.success !== false) {
          setDashboardData(response);
        } else {
          setDashboardData(null);
        }
        setLoadingDashboard(false);
      }
    });
  }, [entryId]);

  // Load all data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadSamplesForVerification();
    loadSamplesForReporting();
    loadDashboard();

    return () => {
      componentMounted.current = false;
    };
  }, [
    entryId,
    pageData?.id,
    loadSamplesForVerification,
    loadSamplesForReporting,
    loadDashboard,
  ]);

  // Approve a result
  const handleApproveResult = useCallback(
    (sample, test) => {
      setVerifying(true);
      setError(null);

      const verifyData = {
        labNo: sample.labNo,
        testId: test.testId,
        approved: true,
        notebookPageId: pageData?.id,
      };

      postToOpenElisServer(
        "/rest/medlab/verify-result",
        JSON.stringify(verifyData),
        (status) => {
          setVerifying(false);
          if (status === 200) {
            setSuccess(
              intl.formatMessage({
                id: "medlab.validation.approve.success",
                defaultMessage: "Result verified successfully",
              }),
            );
            loadSamplesForVerification();
            loadSamplesForReporting();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(
              intl.formatMessage({
                id: "medlab.validation.approve.error",
                defaultMessage: "Error verifying result",
              }),
            );
          }
        },
      );
    },
    [
      pageData,
      intl,
      loadSamplesForVerification,
      loadSamplesForReporting,
      onProgressUpdate,
    ],
  );

  // Open rejection modal
  const handleOpenRejectModal = useCallback((sample, test) => {
    setSelectedSample(sample);
    setSelectedTest(test);
    setRejectionComments("");
    setRejectModalOpen(true);
  }, []);

  // Submit rejection
  const handleSubmitRejection = useCallback(() => {
    if (!selectedSample || !selectedTest) return;

    setVerifying(true);
    setError(null);

    const verifyData = {
      labNo: selectedSample.labNo,
      testId: selectedTest.testId,
      approved: false,
      comments: rejectionComments,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServer(
      "/rest/medlab/verify-result",
      JSON.stringify(verifyData),
      (status) => {
        setVerifying(false);
        if (status === 200) {
          setSuccess(
            intl.formatMessage({
              id: "medlab.validation.reject.success",
              defaultMessage: "Result rejected - needs re-entry",
            }),
          );
          setRejectModalOpen(false);
          loadSamplesForVerification();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "medlab.validation.reject.error",
              defaultMessage: "Error rejecting result",
            }),
          );
        }
      },
    );
  }, [
    selectedSample,
    selectedTest,
    rejectionComments,
    pageData,
    intl,
    loadSamplesForVerification,
    onProgressUpdate,
  ]);

  // Open delivery modal
  const handleOpenDeliveryModal = useCallback((sample) => {
    setSelectedSample(sample);
    setDeliveryMethod("print");
    setRecipient("");
    setDeliveryModalOpen(true);
  }, []);

  // Submit delivery
  const handleSubmitDelivery = useCallback(() => {
    if (!selectedSample) return;

    const reportData = {
      labNo: selectedSample.labNo,
      deliveryMethod: deliveryMethod,
      recipient: recipient,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServer(
      "/rest/medlab/mark-reported",
      JSON.stringify(reportData),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage({
              id: "medlab.reporting.delivery.success",
              defaultMessage: "Report marked as delivered",
            }),
          );
          setDeliveryModalOpen(false);
          loadSamplesForReporting();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "medlab.reporting.delivery.error",
              defaultMessage: "Error marking report as delivered",
            }),
          );
        }
      },
    );
  }, [
    selectedSample,
    deliveryMethod,
    recipient,
    pageData,
    intl,
    loadSamplesForReporting,
    onProgressUpdate,
  ]);

  // Open reference range modal
  const handleOpenRefRangeModal = useCallback((test) => {
    setRefRangeTestId(test.testId);
    setRefRangeLowNormal(test.lowNormal || "");
    setRefRangeHighNormal(test.highNormal || "");
    setRefRangeLowCritical(test.lowCritical || "");
    setRefRangeHighCritical(test.highCritical || "");
    setRefRangeModalOpen(true);
  }, []);

  // Submit reference range modification
  const handleSubmitRefRange = useCallback(() => {
    if (!refRangeTestId) return;

    const data = {
      testId: refRangeTestId,
      lowNormal: refRangeLowNormal ? parseFloat(refRangeLowNormal) : null,
      highNormal: refRangeHighNormal ? parseFloat(refRangeHighNormal) : null,
      lowCritical: refRangeLowCritical ? parseFloat(refRangeLowCritical) : null,
      highCritical: refRangeHighCritical
        ? parseFloat(refRangeHighCritical)
        : null,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServerJsonResponse(
      "/rest/medlab/modify-reference-range",
      JSON.stringify(data),
      (response) => {
        if (response && response.success) {
          setSuccess(
            intl.formatMessage({
              id: "medlab.validation.refRange.success",
              defaultMessage: "Reference range updated successfully",
            }),
          );
          setRefRangeModalOpen(false);
          loadSamplesForVerification();
        } else {
          setError(response?.error || "Error updating reference range");
        }
      },
    );
  }, [
    refRangeTestId,
    refRangeLowNormal,
    refRangeHighNormal,
    refRangeLowCritical,
    refRangeHighCritical,
    pageData,
    intl,
    loadSamplesForVerification,
  ]);

  // Mark verification complete
  const handleMarkVerificationComplete = useCallback(() => {
    const verifiedSamples = samplesForVerification.filter(
      (s) => s.verificationStatus === "VERIFIED" || s.pendingVerification === 0,
    );

    if (verifiedSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "medlab.validation.noSamplesToComplete",
          defaultMessage: "No fully verified samples to mark complete",
        }),
      );
      return;
    }

    setCompleting(true);
    setError(null);

    const sampleIds = verifiedSamples.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      "/rest/medlab/verification/mark-complete",
      JSON.stringify({
        sampleIds,
        notebookPageId: pageData?.id,
      }),
      (response) => {
        setCompleting(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "medlab.validation.markComplete.success",
                defaultMessage:
                  "Marked {count} samples as verification complete",
              },
              { count: response.updatedCount },
            ),
          );
          loadSamplesForVerification();
          loadSamplesForReporting();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Error marking samples complete");
        }
      },
    );
  }, [
    samplesForVerification,
    pageData,
    intl,
    loadSamplesForVerification,
    loadSamplesForReporting,
    onProgressUpdate,
  ]);

  // Get status tag type
  const getStatusTagType = (status) => {
    switch (status) {
      case "VERIFIED":
      case "REPORTED":
        return "green";
      case "PENDING_VERIFICATION":
      case "READY":
        return "blue";
      case "REJECTED":
        return "red";
      default:
        return "gray";
    }
  };

  // Calculate verification stats
  const totalVerificationSamples = samplesForVerification.length;
  const pendingVerification = samplesForVerification.reduce(
    (sum, s) => sum + (s.pendingVerification || 0),
    0,
  );
  const verified = samplesForVerification.reduce(
    (sum, s) => sum + (s.verified || 0),
    0,
  );

  // Calculate reporting stats
  const totalReportingSamples = samplesForReporting.length;
  const reportedSamples = samplesForReporting.filter(
    (s) => s.reportingStatus === "REPORTED",
  ).length;
  const readyToReport = samplesForReporting.filter(
    (s) => s.reportingStatus === "READY",
  ).length;

  return (
    <div className="validation-reporting-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.validationReporting.title"
            defaultMessage="Validation, Reporting & Performance Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.validationReporting.description"
            defaultMessage="Authorize results, generate reports, and monitor lab performance."
          />
        </p>
      </div>

      {/* Error/Success notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "error", defaultMessage: "Error" })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Main Tabs */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Validation and Reporting tabs">
          <Tab renderIcon={Checkmark}>
            <FormattedMessage
              id="medlab.validation.tab.validation"
              defaultMessage="Validation ({count})"
              values={{ count: pendingVerification }}
            />
          </Tab>
          <Tab renderIcon={DocumentPdf}>
            <FormattedMessage
              id="medlab.validation.tab.reporting"
              defaultMessage="Reporting ({count})"
              values={{ count: readyToReport }}
            />
          </Tab>
          <Tab renderIcon={Analytics}>
            <FormattedMessage
              id="medlab.validation.tab.dashboard"
              defaultMessage="Performance Dashboard"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Validation Tab */}
          <TabPanel>
            {/* Validation Progress */}
            <Grid fullWidth className="progress-section">
              <Column lg={16} md={8} sm={4}>
                <div className="progress-tiles">
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="medlab.validation.totalResults"
                        defaultMessage="Total Results"
                      />
                    </span>
                    <span className="progress-value">
                      {verified + pendingVerification}
                    </span>
                  </Tile>
                  <Tile className="progress-tile verified">
                    <span className="progress-label">
                      <FormattedMessage
                        id="medlab.validation.verified"
                        defaultMessage="Verified"
                      />
                    </span>
                    <span className="progress-value">{verified}</span>
                  </Tile>
                  <Tile className="progress-tile pending">
                    <span className="progress-label">
                      <FormattedMessage
                        id="medlab.validation.pending"
                        defaultMessage="Pending"
                      />
                    </span>
                    <span className="progress-value">
                      {pendingVerification}
                    </span>
                  </Tile>
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="medlab.validation.samples"
                        defaultMessage="Samples"
                      />
                    </span>
                    <span className="progress-value">
                      {totalVerificationSamples}
                    </span>
                  </Tile>
                </div>
              </Column>
            </Grid>

            {/* Mark Complete Button */}
            <div
              style={{
                marginBottom: "1rem",
                display: "flex",
                justifyContent: "flex-end",
              }}
            >
              <Button
                kind="primary"
                size="md"
                renderIcon={Checkmark}
                onClick={handleMarkVerificationComplete}
                disabled={completing || verified === 0}
              >
                {completing ? (
                  <FormattedMessage
                    id="medlab.validation.completing"
                    defaultMessage="Completing..."
                  />
                ) : (
                  <FormattedMessage
                    id="medlab.validation.markComplete"
                    defaultMessage="Mark Verification Complete"
                  />
                )}
              </Button>
            </div>

            {/* Loading */}
            {loadingVerification && <Loading />}

            {/* Samples for Verification */}
            {!loadingVerification && samplesForVerification.length > 0 && (
              <div className="orders-section">
                {samplesForVerification.map((sample) => (
                  <div
                    key={sample.id}
                    className="sample-card"
                    style={{ marginBottom: "1rem" }}
                  >
                    <Tile style={{ padding: "1rem" }}>
                      <Grid>
                        <Column lg={4} md={2} sm={2}>
                          <strong>
                            <FormattedMessage id="sample.label.labnumber" />:
                          </strong>{" "}
                          {sample.labNo}
                        </Column>
                        <Column lg={4} md={2} sm={2}>
                          <strong>
                            <FormattedMessage id="patient.label" />:
                          </strong>{" "}
                          {sample.patientName}
                        </Column>
                        <Column lg={4} md={2} sm={2}>
                          <strong>
                            <FormattedMessage
                              id="sample.sampleType"
                              defaultMessage="Sample Type"
                            />
                            :
                          </strong>{" "}
                          {sample.sampleType}
                        </Column>
                        <Column lg={4} md={2} sm={2}>
                          <Tag
                            type={getStatusTagType(sample.verificationStatus)}
                          >
                            {sample.verified || 0}/{(sample.tests || []).length}{" "}
                            <FormattedMessage
                              id="medlab.validation.verified"
                              defaultMessage="verified"
                            />
                          </Tag>
                        </Column>
                      </Grid>

                      {/* Tests Table */}
                      <div style={{ marginTop: "1rem" }}>
                        <DataTable
                          rows={(sample.tests || []).map((t, idx) => ({
                            ...t,
                            id: `${sample.id}-${t.testId}-${idx}`,
                          }))}
                          headers={[
                            {
                              key: "testName",
                              header: intl.formatMessage({
                                id: "test.testName",
                                defaultMessage: "Test",
                              }),
                            },
                            {
                              key: "resultValue",
                              header: intl.formatMessage({
                                id: "medlab.result.value",
                                defaultMessage: "Result",
                              }),
                            },
                            {
                              key: "verificationStatus",
                              header: intl.formatMessage({
                                id: "medlab.validation.status",
                                defaultMessage: "Status",
                              }),
                            },
                            {
                              key: "actions",
                              header: intl.formatMessage({
                                id: "label.button.actions",
                              }),
                            },
                          ]}
                          size="sm"
                        >
                          {({
                            rows,
                            headers,
                            getHeaderProps,
                            getTableProps,
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
                                  {rows.map((row) => {
                                    const test =
                                      sample.tests.find(
                                        (t) =>
                                          `${sample.id}-${t.testId}` ===
                                          row.id.substring(
                                            0,
                                            row.id.lastIndexOf("-"),
                                          ),
                                      ) || sample.tests[rows.indexOf(row)];
                                    return (
                                      <TableRow key={row.id}>
                                        {row.cells.map((cell) => (
                                          <TableCell key={cell.id}>
                                            {cell.info.header === "actions" ? (
                                              test?.verificationStatus ===
                                              "PENDING_VERIFICATION" ? (
                                                <div
                                                  style={{
                                                    display: "flex",
                                                    gap: "0.5rem",
                                                  }}
                                                >
                                                  <Button
                                                    kind="primary"
                                                    size="sm"
                                                    renderIcon={Checkmark}
                                                    onClick={() =>
                                                      handleApproveResult(
                                                        sample,
                                                        test,
                                                      )
                                                    }
                                                    disabled={verifying}
                                                  >
                                                    <FormattedMessage
                                                      id="medlab.validation.approve"
                                                      defaultMessage="Approve"
                                                    />
                                                  </Button>
                                                  <Button
                                                    kind="danger"
                                                    size="sm"
                                                    renderIcon={Close}
                                                    onClick={() =>
                                                      handleOpenRejectModal(
                                                        sample,
                                                        test,
                                                      )
                                                    }
                                                    disabled={verifying}
                                                  >
                                                    <FormattedMessage
                                                      id="medlab.validation.reject"
                                                      defaultMessage="Reject"
                                                    />
                                                  </Button>
                                                  <Button
                                                    kind="ghost"
                                                    size="sm"
                                                    renderIcon={Settings}
                                                    onClick={() =>
                                                      handleOpenRefRangeModal(
                                                        test,
                                                      )
                                                    }
                                                    hasIconOnly
                                                    iconDescription="Modify Reference Range"
                                                  />
                                                </div>
                                              ) : (
                                                <Tag type="green" size="sm">
                                                  <CheckmarkFilled size={16} />
                                                </Tag>
                                              )
                                            ) : cell.info.header ===
                                              "verificationStatus" ? (
                                              <Tag
                                                type={getStatusTagType(
                                                  cell.value,
                                                )}
                                                size="sm"
                                              >
                                                {cell.value}
                                              </Tag>
                                            ) : (
                                              cell.value
                                            )}
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
                      </div>
                    </Tile>
                  </div>
                ))}
              </div>
            )}

            {/* Empty state */}
            {!loadingVerification && samplesForVerification.length === 0 && (
              <div className="empty-state">
                <WarningAlt
                  size={48}
                  style={{ color: "#f1c21b", marginBottom: "1rem" }}
                />
                <p>
                  <FormattedMessage
                    id="medlab.validation.empty"
                    defaultMessage="No results available for verification. Complete the Result Entry page first."
                  />
                </p>
              </div>
            )}
          </TabPanel>

          {/* Reporting Tab */}
          <TabPanel>
            {/* Reporting Progress */}
            <Grid fullWidth className="progress-section">
              <Column lg={16} md={8} sm={4}>
                <div className="progress-tiles">
                  <Tile className="progress-tile">
                    <span className="progress-label">
                      <FormattedMessage
                        id="medlab.reporting.total"
                        defaultMessage="Total Samples"
                      />
                    </span>
                    <span className="progress-value">
                      {totalReportingSamples}
                    </span>
                  </Tile>
                  <Tile className="progress-tile verified">
                    <span className="progress-label">
                      <FormattedMessage
                        id="medlab.reporting.reported"
                        defaultMessage="Reported"
                      />
                    </span>
                    <span className="progress-value">{reportedSamples}</span>
                  </Tile>
                  <Tile className="progress-tile pending">
                    <span className="progress-label">
                      <FormattedMessage
                        id="medlab.reporting.ready"
                        defaultMessage="Ready"
                      />
                    </span>
                    <span className="progress-value">{readyToReport}</span>
                  </Tile>
                </div>
              </Column>
            </Grid>

            {/* Loading */}
            {loadingReporting && <Loading />}

            {/* Samples for Reporting */}
            {!loadingReporting && samplesForReporting.length > 0 && (
              <div className="orders-section">
                {samplesForReporting.map((sample) => (
                  <div
                    key={sample.id}
                    className="sample-card"
                    style={{ marginBottom: "1rem" }}
                  >
                    <Tile style={{ padding: "1rem" }}>
                      <Grid>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage id="sample.label.labnumber" />:
                          </strong>{" "}
                          {sample.labNo}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage id="patient.label" />:
                          </strong>{" "}
                          {sample.patientName}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage
                              id="sample.sampleType"
                              defaultMessage="Sample Type"
                            />
                            :
                          </strong>{" "}
                          {sample.sampleType}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <Tag type={getStatusTagType(sample.reportingStatus)}>
                            {sample.reportingStatus}
                          </Tag>
                        </Column>
                        <Column lg={4} md={2} sm={2}>
                          {sample.reportingStatus === "READY" && (
                            <Button
                              kind="primary"
                              size="sm"
                              renderIcon={DocumentPdf}
                              onClick={() => handleOpenDeliveryModal(sample)}
                            >
                              <FormattedMessage
                                id="medlab.reporting.deliver"
                                defaultMessage="Deliver Report"
                              />
                            </Button>
                          )}
                          {sample.reportingStatus === "REPORTED" && (
                            <Tag type="green" size="sm">
                              <CheckmarkFilled
                                size={16}
                                style={{ marginRight: "4px" }}
                              />
                              <FormattedMessage
                                id="medlab.reporting.delivered"
                                defaultMessage="Delivered"
                              />
                            </Tag>
                          )}
                        </Column>
                      </Grid>
                    </Tile>
                  </div>
                ))}
              </div>
            )}

            {/* Empty state */}
            {!loadingReporting && samplesForReporting.length === 0 && (
              <div className="empty-state">
                <WarningAlt
                  size={48}
                  style={{ color: "#f1c21b", marginBottom: "1rem" }}
                />
                <p>
                  <FormattedMessage
                    id="medlab.reporting.empty"
                    defaultMessage="No results available for reporting. Complete verification first."
                  />
                </p>
              </div>
            )}
          </TabPanel>

          {/* Performance Dashboard Tab */}
          <TabPanel>
            {loadingDashboard && <Loading />}

            {!loadingDashboard && dashboardData && (
              <Grid fullWidth>
                {/* Sample Acceptance Rate */}
                <Column lg={8} md={4} sm={4}>
                  <Tile style={{ marginBottom: "1rem", padding: "1rem" }}>
                    <h5>
                      <FormattedMessage
                        id="medlab.dashboard.acceptanceRate"
                        defaultMessage="Sample Acceptance Rate"
                      />
                    </h5>
                    <div style={{ marginTop: "1rem" }}>
                      <ProgressBar
                        value={
                          dashboardData.sampleAcceptance?.acceptanceRate || 0
                        }
                        max={100}
                        label={`${(dashboardData.sampleAcceptance?.acceptanceRate || 0).toFixed(1)}%`}
                        helperText={`${dashboardData.sampleAcceptance?.acceptedSamples || 0} of ${dashboardData.sampleAcceptance?.totalSamples || 0} samples accepted`}
                      />
                    </div>
                    <div
                      style={{
                        marginTop: "1rem",
                        display: "flex",
                        gap: "1rem",
                      }}
                    >
                      <Tag type="green">
                        Accepted:{" "}
                        {dashboardData.sampleAcceptance?.acceptedSamples || 0}
                      </Tag>
                      <Tag type="red">
                        Rejected:{" "}
                        {dashboardData.sampleAcceptance?.rejectedSamples || 0}
                      </Tag>
                    </div>
                  </Tile>
                </Column>

                {/* Turnaround Time */}
                <Column lg={8} md={4} sm={4}>
                  <Tile style={{ marginBottom: "1rem", padding: "1rem" }}>
                    <h5>
                      <Time size={20} style={{ marginRight: "0.5rem" }} />
                      <FormattedMessage
                        id="medlab.dashboard.turnaroundTime"
                        defaultMessage="Turnaround Time (TAT)"
                      />
                    </h5>
                    <div style={{ marginTop: "1rem" }}>
                      <p>
                        <strong>
                          <FormattedMessage
                            id="medlab.dashboard.avgTat"
                            defaultMessage="Average Total TAT:"
                          />
                        </strong>{" "}
                        {dashboardData.turnaroundTime?.avgTotalTatMinutes || 0}{" "}
                        <FormattedMessage
                          id="medlab.dashboard.minutes"
                          defaultMessage="minutes"
                        />
                      </p>
                      <p>
                        <strong>
                          <FormattedMessage
                            id="medlab.dashboard.samplesProcessed"
                            defaultMessage="Samples Processed:"
                          />
                        </strong>{" "}
                        {dashboardData.turnaroundTime?.sampleCount || 0}
                      </p>
                    </div>
                  </Tile>
                </Column>

                {/* QC Performance */}
                <Column lg={8} md={4} sm={4}>
                  <Tile style={{ marginBottom: "1rem", padding: "1rem" }}>
                    <h5>
                      <ChartLine size={20} style={{ marginRight: "0.5rem" }} />
                      <FormattedMessage
                        id="medlab.dashboard.qcPerformance"
                        defaultMessage="QC Performance by Analyzer"
                      />
                    </h5>
                    <div style={{ marginTop: "1rem" }}>
                      {(dashboardData.qcTrends || []).length > 0 ? (
                        <DataTable
                          rows={(dashboardData.qcTrends || []).map(
                            (trend, idx) => ({
                              id: `qc-${idx}`,
                              analyzer: trend.analyzer,
                              totalRuns: trend.totalQcRuns,
                              passed: trend.passedRuns,
                              failed: trend.failedRuns,
                              passRate: `${(trend.passRate || 0).toFixed(1)}%`,
                            }),
                          )}
                          headers={[
                            { key: "analyzer", header: "Analyzer" },
                            { key: "totalRuns", header: "Total Runs" },
                            { key: "passed", header: "Passed" },
                            { key: "failed", header: "Failed" },
                            { key: "passRate", header: "Pass Rate" },
                          ]}
                          size="sm"
                        >
                          {({
                            rows,
                            headers,
                            getHeaderProps,
                            getTableProps,
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
                                    <TableRow key={row.id}>
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
                      ) : (
                        <p style={{ color: "#6f6f6f" }}>No QC data available</p>
                      )}
                    </div>
                  </Tile>
                </Column>

                {/* Equipment Usage */}
                <Column lg={8} md={4} sm={4}>
                  <Tile style={{ marginBottom: "1rem", padding: "1rem" }}>
                    <h5>
                      <Analytics size={20} style={{ marginRight: "0.5rem" }} />
                      <FormattedMessage
                        id="medlab.dashboard.equipmentUsage"
                        defaultMessage="Equipment Usage"
                      />
                    </h5>
                    <div style={{ marginTop: "1rem" }}>
                      {(dashboardData.equipmentUsage || []).length > 0 ? (
                        <DataTable
                          rows={(dashboardData.equipmentUsage || []).map(
                            (eq, idx) => ({
                              id: `eq-${idx}`,
                              analyzer: eq.analyzerName,
                              testCount: eq.testCount,
                              technology: eq.technology,
                            }),
                          )}
                          headers={[
                            { key: "analyzer", header: "Analyzer" },
                            { key: "testCount", header: "Tests Run" },
                            { key: "technology", header: "Technology" },
                          ]}
                          size="sm"
                        >
                          {({
                            rows,
                            headers,
                            getHeaderProps,
                            getTableProps,
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
                                    <TableRow key={row.id}>
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
                      ) : (
                        <p style={{ color: "#6f6f6f" }}>
                          No equipment usage data
                        </p>
                      )}
                    </div>
                  </Tile>
                </Column>

                {/* Sample Utilization */}
                <Column lg={8} md={4} sm={4}>
                  <Tile style={{ marginBottom: "1rem", padding: "1rem" }}>
                    <h5>
                      <FormattedMessage
                        id="medlab.dashboard.sampleUtilization"
                        defaultMessage="Sample Utilization"
                      />
                    </h5>
                    <div style={{ marginTop: "1rem" }}>
                      <ProgressBar
                        value={
                          dashboardData.sampleUtilization?.utilizationRate || 0
                        }
                        max={100}
                        label={`${(dashboardData.sampleUtilization?.utilizationRate || 0).toFixed(1)}%`}
                        helperText="Samples with results"
                      />
                      <div style={{ marginTop: "0.5rem" }}>
                        <p>
                          Total:{" "}
                          {dashboardData.sampleUtilization?.totalSamples || 0}
                        </p>
                        <p>
                          With Results:{" "}
                          {dashboardData.sampleUtilization
                            ?.samplesWithResults || 0}
                        </p>
                      </div>
                    </div>
                  </Tile>
                </Column>

                {/* Corrective Actions */}
                <Column lg={8} md={4} sm={4}>
                  <Tile style={{ marginBottom: "1rem", padding: "1rem" }}>
                    <h5>
                      <WarningAlt
                        size={20}
                        style={{ marginRight: "0.5rem", color: "#f1c21b" }}
                      />
                      <FormattedMessage
                        id="medlab.dashboard.correctiveActions"
                        defaultMessage="Corrective Actions Log"
                      />
                    </h5>
                    <div style={{ marginTop: "1rem" }}>
                      {(dashboardData.correctiveActions || []).length > 0 ? (
                        <ul style={{ paddingLeft: "1rem" }}>
                          {(dashboardData.correctiveActions || [])
                            .slice(0, 5)
                            .map((action, idx) => (
                              <li key={idx} style={{ marginBottom: "0.5rem" }}>
                                <strong>{action.deviationType}:</strong>{" "}
                                {action.actionTaken}
                              </li>
                            ))}
                        </ul>
                      ) : (
                        <p style={{ color: "#24a148" }}>
                          <CheckmarkFilled
                            size={16}
                            style={{ marginRight: "0.5rem" }}
                          />
                          No corrective actions recorded
                        </p>
                      )}
                    </div>
                  </Tile>
                </Column>
              </Grid>
            )}

            {!loadingDashboard && !dashboardData && (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="medlab.dashboard.noData"
                    defaultMessage="No performance data available yet."
                  />
                </p>
              </div>
            )}
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Rejection Modal */}
      <Modal
        open={rejectModalOpen}
        onRequestClose={() => setRejectModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.validation.rejectModal.title",
          defaultMessage: "Reject Result",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.validation.reject",
          defaultMessage: "Reject",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitRejection}
        danger
        size="sm"
      >
        {selectedSample && selectedTest && (
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Tile style={{ marginBottom: "1rem" }}>
                <strong>
                  <FormattedMessage id="sample.label.labnumber" />:
                </strong>{" "}
                {selectedSample.labNo}
                <br />
                <strong>
                  <FormattedMessage id="test.testName" defaultMessage="Test" />:
                </strong>{" "}
                {selectedTest.testName}
                <br />
                <strong>
                  <FormattedMessage
                    id="medlab.result.value"
                    defaultMessage="Result"
                  />
                  :
                </strong>{" "}
                {selectedTest.resultValue}
              </Tile>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="rejection-comments"
                labelText={intl.formatMessage({
                  id: "medlab.validation.comments",
                  defaultMessage: "Rejection Comments",
                })}
                value={rejectionComments}
                onChange={(e) => setRejectionComments(e.target.value)}
                rows={3}
                placeholder={intl.formatMessage({
                  id: "medlab.validation.comments.placeholder",
                  defaultMessage: "Enter reason for rejection...",
                })}
              />
            </Column>
          </Grid>
        )}
      </Modal>

      {/* Delivery Modal */}
      <Modal
        open={deliveryModalOpen}
        onRequestClose={() => setDeliveryModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.reporting.deliverModal.title",
          defaultMessage: "Deliver Report",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.reporting.deliver",
          defaultMessage: "Deliver",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitDelivery}
        size="sm"
      >
        {selectedSample && (
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Tile style={{ marginBottom: "1rem" }}>
                <strong>
                  <FormattedMessage id="sample.label.labnumber" />:
                </strong>{" "}
                {selectedSample.labNo}
                <br />
                <strong>
                  <FormattedMessage id="patient.label" />:
                </strong>{" "}
                {selectedSample.patientName}
              </Tile>
            </Column>
            <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
              <div
                style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}
              >
                <Button
                  kind={deliveryMethod === "print" ? "primary" : "tertiary"}
                  size="md"
                  renderIcon={Printer}
                  onClick={() => setDeliveryMethod("print")}
                >
                  Print
                </Button>
                <Button
                  kind={deliveryMethod === "email" ? "primary" : "tertiary"}
                  size="md"
                  renderIcon={Email}
                  onClick={() => setDeliveryMethod("email")}
                >
                  Email
                </Button>
                <Button
                  kind={deliveryMethod === "pickup" ? "primary" : "tertiary"}
                  size="md"
                  onClick={() => setDeliveryMethod("pickup")}
                >
                  Pickup
                </Button>
              </div>
            </Column>
            {deliveryMethod === "email" && (
              <Column lg={16} md={8} sm={4}>
                <TextInput
                  id="recipient-email"
                  labelText={intl.formatMessage({
                    id: "medlab.reporting.recipientEmail",
                    defaultMessage: "Recipient Email",
                  })}
                  value={recipient}
                  onChange={(e) => setRecipient(e.target.value)}
                  placeholder="email@example.com"
                />
              </Column>
            )}
          </Grid>
        )}
      </Modal>

      {/* Reference Range Modal */}
      <Modal
        open={refRangeModalOpen}
        onRequestClose={() => setRefRangeModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.validation.refRangeModal.title",
          defaultMessage: "Modify Reference Range",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.button.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitRefRange}
        size="sm"
      >
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="ref-low-normal"
              labelText={intl.formatMessage({
                id: "medlab.validation.refRange.lowNormal",
                defaultMessage: "Low Normal",
              })}
              value={refRangeLowNormal}
              onChange={(e) => setRefRangeLowNormal(e.target.value)}
              type="number"
              style={{ marginBottom: "1rem" }}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="ref-high-normal"
              labelText={intl.formatMessage({
                id: "medlab.validation.refRange.highNormal",
                defaultMessage: "High Normal",
              })}
              value={refRangeHighNormal}
              onChange={(e) => setRefRangeHighNormal(e.target.value)}
              type="number"
              style={{ marginBottom: "1rem" }}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="ref-low-critical"
              labelText={intl.formatMessage({
                id: "medlab.validation.refRange.lowCritical",
                defaultMessage: "Low Critical (optional)",
              })}
              value={refRangeLowCritical}
              onChange={(e) => setRefRangeLowCritical(e.target.value)}
              type="number"
              style={{ marginBottom: "1rem" }}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="ref-high-critical"
              labelText={intl.formatMessage({
                id: "medlab.validation.refRange.highCritical",
                defaultMessage: "High Critical (optional)",
              })}
              value={refRangeHighCritical}
              onChange={(e) => setRefRangeHighCritical(e.target.value)}
              type="number"
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default ValidationReportingPage;
