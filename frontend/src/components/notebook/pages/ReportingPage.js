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
  Tag,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { DocumentPdf, Email, Printer } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import "../workflow/NotebookWorkflow.css";

/**
 * ReportingPage - Page 6 of the MedLab workflow.
 * Handles generating and delivering patient reports for verified results.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ReportingPage({ entryId, pageData, progress, onProgressUpdate }) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Delivery modal state
  const [deliveryModalOpen, setDeliveryModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [deliveryMethod, setDeliveryMethod] = useState("print");
  const [recipient, setRecipient] = useState("");

  // Load results for reporting
  const loadResultsForReporting = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const url = `/rest/medlab/entry/${entryId}/results-for-reporting`;

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setSamples(response);
        } else {
          setSamples([]);
        }
        setLoading(false);
      }
    });
  }, [entryId]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadResultsForReporting();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadResultsForReporting]);

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
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.reporting.delivery.success",
              defaultMessage: "Report marked as delivered",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setDeliveryModalOpen(false);
          loadResultsForReporting();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.reporting.delivery.error",
              defaultMessage: "Error marking report as delivered",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedSample,
    deliveryMethod,
    recipient,
    pageData,
    intl,
    addNotification,
    setNotificationVisible,
    loadResultsForReporting,
    onProgressUpdate,
  ]);

  // Calculate stats
  const totalSamples = samples.length;
  const readySamples = samples.filter(
    (s) => s.reportingStatus === "READY",
  ).length;
  const reportedSamples = samples.filter(
    (s) => s.reportingStatus === "REPORTED",
  ).length;
  const pendingSamples = samples.filter(
    (s) => s.reportingStatus === "PENDING_VERIFICATION",
  ).length;

  // Get status tag type
  const getStatusTagType = (status) => {
    switch (status) {
      case "REPORTED":
        return "green";
      case "READY":
        return "blue";
      case "PENDING_VERIFICATION":
        return "gray";
      default:
        return "gray";
    }
  };

  // Get delivery method icon
  const getDeliveryIcon = (method) => {
    switch (method) {
      case "email":
        return Email;
      case "print":
        return Printer;
      default:
        return DocumentPdf;
    }
  };

  return (
    <div className="reporting-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.reporting.title"
            defaultMessage="Result Reporting"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.reporting.description"
            defaultMessage="Generate and deliver patient reports for verified results."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.reporting.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.reporting.reported"
                  defaultMessage="Reported"
                />
              </span>
              <span className="progress-value">{reportedSamples}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.reporting.ready"
                  defaultMessage="Ready to Report"
                />
              </span>
              <span className="progress-value">{readySamples}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.reporting.pendingVerification"
                  defaultMessage="Pending Verification"
                />
              </span>
              <span className="progress-value">{pendingSamples}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Loading State */}
      {loading && <Loading />}

      {/* Samples Ready for Reporting */}
      {!loading && samples.length > 0 && (
        <div className="orders-section">
          <h5>
            <FormattedMessage
              id="medlab.page.reporting.resultsForReporting"
              defaultMessage="Results for Reporting"
            />
          </h5>
          {samples.map((sample) => (
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
                        <FormattedMessage
                          id="medlab.reporting.delivered"
                          defaultMessage="Delivered"
                        />
                      </Tag>
                    )}
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
                        key: "reportStatus",
                        header: intl.formatMessage({
                          id: "medlab.reporting.status",
                          defaultMessage: "Report Status",
                        }),
                      },
                    ]}
                    size="sm"
                  >
                    {({ rows, headers, getHeaderProps, getTableProps }) => (
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
                                    {cell.info.header === "reportStatus" ? (
                                      <Tag
                                        type={
                                          cell.value === "REPORTED"
                                            ? "green"
                                            : "blue"
                                        }
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
                            ))}
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
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="medlab.page.reporting.empty"
              defaultMessage="No results available for reporting. Verify results on the Result Verification page first."
            />
          </p>
        </div>
      )}

      {/* Delivery Modal */}
      <Modal
        open={deliveryModalOpen}
        onRequestClose={() => setDeliveryModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.page.reporting.deliverModal",
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
        <Grid>
          {selectedSample && (
            <>
              <Column lg={16} md={8} sm={4}>
                <Tile
                  className="order-info-tile"
                  style={{ marginBottom: "1rem" }}
                >
                  <strong>
                    <FormattedMessage id="sample.label.labnumber" />:
                  </strong>{" "}
                  {selectedSample.labNo}
                  <br />
                  <strong>
                    <FormattedMessage id="patient.label" />:
                  </strong>{" "}
                  {selectedSample.patientName}
                  <br />
                  <strong>
                    <FormattedMessage
                      id="medlab.reporting.testsCount"
                      defaultMessage="Tests"
                    />
                    :
                  </strong>{" "}
                  {selectedSample.totalTests}
                </Tile>
              </Column>
              <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                <Select
                  id="delivery-method"
                  labelText={intl.formatMessage({
                    id: "medlab.reporting.deliveryMethod",
                    defaultMessage: "Delivery Method",
                  })}
                  value={deliveryMethod}
                  onChange={(e) => setDeliveryMethod(e.target.value)}
                >
                  <SelectItem value="print" text="Print" />
                  <SelectItem value="email" text="Email" />
                  <SelectItem value="pickup" text="Patient Pickup" />
                </Select>
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
                    placeholder={intl.formatMessage({
                      id: "medlab.reporting.recipientEmail.placeholder",
                      defaultMessage: "Enter email address",
                    })}
                  />
                </Column>
              )}
            </>
          )}
        </Grid>
      </Modal>
    </div>
  );
}

export default ReportingPage;
