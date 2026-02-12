import { useState, useEffect, useCallback, useContext } from "react";
import {
  Grid,
  Column,
  Tile,
  Button,
  Loading,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import CartridgeUsageAPI from "./EquipmentUsageService";
import "./EquipmentUsage.css";

/**
 * EquipmentUsageDashboard Component
 *
 * Displays aggregated equipment usage metrics and recent submissions.
 * Features:
 * - Total equipment count metric
 * - Total usage records metric
 * - Usage breakdown by equipment (table)
 * - Recent submissions display with all response fields
 * - Print functionality
 */
const EquipmentUsageDashboard = ({ initialSubmission }) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle, message }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
        message,
      });
    },
    [addNotification, setNotificationVisible],
  );

  // Metrics State
  const [metrics, setMetrics] = useState({
    totalEquipmentCount: 0,
    totalUsageRecords: 0,
    usageByEquipment: [],
  });
  const [loadingMetrics, setLoadingMetrics] = useState(true);

  // Recent Submission State
  const [recentSubmission, setRecentSubmission] = useState(
    initialSubmission || null,
  );
  const [submissionRows, setSubmissionRows] = useState([]);
  const [loadingSubmissions, setLoadingSubmissions] = useState(true);

  // Fetch Metrics and Submissions on Mount
  useEffect(() => {
    let isMounted = true;
    const controller = new AbortController();

    const fetchMetrics = async () => {
      setLoadingMetrics(true);
      try {
        CartridgeUsageAPI.getEquipmentUsageMetrics(
          null,
          null,
          (data, error) => {
            if (error) {
              if (isMounted) {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({ id: "notification.error" }),
                  message: intl.formatMessage({
                    id: "equipment.usage.error.loadMetricsFailed",
                    defaultMessage: "Failed to load metrics",
                  }),
                });
                setLoadingMetrics(false);
              }
            } else if (isMounted && data && typeof data === "object") {
              setMetrics(data);
              setLoadingMetrics(false);
            } else if (isMounted) {
              setLoadingMetrics(false);
            }
          },
          controller.signal,
        );
      } catch (error) {
        if (isMounted) {
          setLoadingMetrics(false);
        }
      }
    };

    const fetchSubmissions = async () => {
      setLoadingSubmissions(true);
      try {
        CartridgeUsageAPI.getEquipmentUsageSubmissions(
          null,
          null,
          (data, error) => {
            if (error) {
              if (isMounted) {
                setLoadingSubmissions(false);
              }
            } else if (isMounted && Array.isArray(data)) {
              setSubmissionRows(data);
              setLoadingSubmissions(false);
            } else if (isMounted) {
              setLoadingSubmissions(false);
            }
          },
          controller.signal,
        );
      } catch (error) {
        if (isMounted) {
          setLoadingSubmissions(false);
        }
      }
    };

    fetchMetrics();
    fetchSubmissions();

    // Cleanup function
    return () => {
      isMounted = false;
      controller.abort();
    };
  }, [intl, notify]);

  // Handle successful submission from EquipmentUsageLog
  const handleSubmitSuccess = useCallback((submissionData) => {
    setRecentSubmission(submissionData);

    // Fetch fresh submissions from database (source of truth)
    CartridgeUsageAPI.getEquipmentUsageSubmissions(
      null,
      null,
      (data, error) => {
        if (error) {
          // Silent refresh - don't show error notification for background updates
        } else if (Array.isArray(data)) {
          setSubmissionRows(data);
        }
      },
    );

    // Refresh metrics to include the new submission
    CartridgeUsageAPI.getEquipmentUsageMetrics(null, null, (data, error) => {
      if (error) {
        // Silent refresh - don't show error notification for background updates
      } else if (data && typeof data === "object") {
        setMetrics(data);
      }
    });
  }, []);

  // Handle print
  const handlePrint = () => {
    window.print();
  };

  // Usage by Equipment Table Columns
  const equipmentTableHeaders = [
    {
      key: "equipmentName",
      header: intl.formatMessage({
        id: "equipment.metrics.table.name",
        defaultMessage: "Equipment Name",
      }),
    },
    {
      key: "usageCount",
      header: intl.formatMessage({
        id: "equipment.metrics.table.usageCount",
        defaultMessage: "Usage Count",
      }),
    },
    {
      key: "totalQuantityUsed",
      header: intl.formatMessage({
        id: "equipment.metrics.table.quantity",
        defaultMessage: "Total Quantity Used",
      }),
    },
  ];

  const equipmentTableRows = (metrics.usageByEquipment || []).map(
    (item, index) => ({
      id: `equipment-${index}`,
      equipmentName: item.equipmentName || "Unknown",
      usageCount: item.usageCount || 0,
      totalQuantityUsed: (item.totalQuantityUsed || 0).toFixed(2),
    }),
  );

  return (
    <>
      <AlertDialog />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="equipmentUsageContainer">
            {/* Metrics Tiles */}
            {loadingMetrics ? (
              <Loading
                description={intl.formatMessage({ id: "loading.metrics" })}
              />
            ) : (
              <div className="equipmentMetricsGrid">
                <Tile className="metricTile">
                  <p className="metricLabel">
                    <FormattedMessage
                      id="equipment.metrics.totalEquipment"
                      defaultMessage="Total Equipment"
                    />
                  </p>
                  <p className="metricValue">
                    {metrics.totalEquipmentCount || 0}
                  </p>
                </Tile>

                <Tile className="metricTile">
                  <p className="metricLabel">
                    <FormattedMessage
                      id="equipment.metrics.totalUsage"
                      defaultMessage="Total Usage Records"
                    />
                  </p>
                  <p className="metricValue">
                    {metrics.totalUsageRecords || 0}
                  </p>
                </Tile>

                <Tile className="metricTile">
                  <p className="metricLabel">
                    <FormattedMessage
                      id="equipment.metrics.equipmentWithUsage"
                      defaultMessage="Equipment with Usage"
                    />
                  </p>
                  <p className="metricValue">
                    {(metrics.usageByEquipment || []).length}
                  </p>
                </Tile>

                <Tile className="metricTile">
                  <p className="metricLabel">
                    <FormattedMessage
                      id="equipment.metrics.recentSubmission"
                      defaultMessage="Recent Submission"
                    />
                  </p>
                  <p className="metricValue">
                    {recentSubmission ? "Yes" : "No"}
                  </p>
                </Tile>
              </div>
            )}

            {/* Usage by Equipment Section */}
            <div className="usageHistorySection">
              <h3>
                <FormattedMessage
                  id="equipment.metrics.byEquipment"
                  defaultMessage="Usage by Equipment"
                />
              </h3>

              {metrics.usageByEquipment &&
              metrics.usageByEquipment.length > 0 ? (
                <div className="tableWrapper">
                  <table className="usageHistoryTable">
                    <thead>
                      <tr>
                        {equipmentTableHeaders.map((header) => (
                          <th key={header.key}>{header.header}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {equipmentTableRows.map((row) => (
                        <tr key={row.id}>
                          <td>{row.equipmentName}</td>
                          <td>{row.usageCount}</td>
                          <td>{row.totalQuantityUsed}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="emptyStateSection">
                  <p>
                    <FormattedMessage
                      id="equipment.metrics.noEquipmentUsage"
                      defaultMessage="No equipment usage records found"
                    />
                  </p>
                </div>
              )}
            </div>

            {/* Equipment Usage Submissions DataTable */}
            <div className="usageHistorySection">
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "1rem",
                }}
              >
                <h3 style={{ margin: 0 }}>
                  <FormattedMessage
                    id="equipment.usage.submissions"
                    defaultMessage="Equipment Usage Submissions"
                  />
                </h3>
                <Button kind="secondary" size="sm" onClick={handlePrint}>
                  <FormattedMessage id="common.print" defaultMessage="Print" />
                </Button>
              </div>

              {/* Submissions DataTable - Always visible */}
              <div className="tableWrapper">
                <table className="usageHistoryTable">
                  <thead>
                    <tr>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.id"
                          defaultMessage="Record ID"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.equipment"
                          defaultMessage="Equipment"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.lot"
                          defaultMessage="Lot Number"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.quantity"
                          defaultMessage="Quantity Used"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.user"
                          defaultMessage="Performed By"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.date"
                          defaultMessage="Date"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.operatorName"
                          defaultMessage="Operator Name"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.loginTime"
                          defaultMessage="Login Time"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.logoutTime"
                          defaultMessage="Logout Time"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.activities"
                          defaultMessage="Activities Done"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.equipmentStatus"
                          defaultMessage="Equipment Status"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.approvedBy"
                          defaultMessage="Approved By"
                        />
                      </th>
                      <th>
                        <FormattedMessage
                          id="equipment.submission.approvalDate"
                          defaultMessage="Approval Date"
                        />
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {submissionRows && submissionRows.length > 0 ? (
                      submissionRows.map((submission) => (
                        <tr key={submission.id}>
                          <td>{submission.id || "N/A"}</td>
                          <td>{submission.inventoryItemName || "N/A"}</td>
                          <td>{submission.lotNumber || "N/A"}</td>
                          <td>{submission.quantityUsed || 0}</td>
                          <td>{submission.performedByUserName || "N/A"}</td>
                          <td>
                            {submission.usageDate
                              ? new Date(submission.usageDate).toLocaleString(
                                  intl.locale,
                                )
                              : "N/A"}
                          </td>
                          <td>{submission.operatorName || "N/A"}</td>
                          <td>{submission.loginTime || "N/A"}</td>
                          <td>{submission.logoutTime || "N/A"}</td>
                          <td>{submission.activities || "N/A"}</td>
                          <td>{submission.equipmentStatus || "N/A"}</td>
                          <td>{submission.approvedBy || "N/A"}</td>
                          <td>{submission.approvalDate || "N/A"}</td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan="13" style={{ textAlign: "center" }}>
                          <em>
                            <FormattedMessage
                              id="equipment.usage.noSubmissions"
                              defaultMessage="No submissions yet"
                            />
                          </em>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default EquipmentUsageDashboard;
