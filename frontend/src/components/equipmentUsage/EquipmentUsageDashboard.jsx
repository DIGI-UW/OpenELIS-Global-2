import { useState, useEffect, useCallback, useContext } from "react";
import { Grid, Column, Tile, Button, Loading } from "@carbon/react";
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

  // Fetch Metrics on Mount
  useEffect(() => {
    let isMounted = true;
    const controller = new AbortController();

    const fetchMetrics = async () => {
      setLoadingMetrics(true);
      try {
        CartridgeUsageAPI.getEquipmentUsageMetrics(
          null,
          null,
          (data) => {
            if (isMounted && data && typeof data === "object") {
              setMetrics(data);
            }
            if (isMounted) {
              setLoadingMetrics(false);
            }
          },
          (error) => {
            console.error("Error loading metrics:", error);
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
          },
          controller.signal,
        );
      } catch (error) {
        console.error("Error fetching metrics:", error);
        if (isMounted) {
          setLoadingMetrics(false);
        }
      }
    };

    fetchMetrics();

    // Cleanup function
    return () => {
      isMounted = false;
      controller.abort();
    };
  }, [intl, notify]);

  // Handle successful submission from EquipmentUsageLog
  const handleSubmitSuccess = useCallback((submissionData) => {
    setRecentSubmission(submissionData);
    // Refresh metrics to include the new submission
    CartridgeUsageAPI.getEquipmentUsageMetrics(
      null,
      null,
      (data) => {
        if (data && typeof data === "object") {
          setMetrics(data);
        }
      },
      (error) => {
        console.error("Error refreshing metrics:", error);
      },
    );
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

            {/* Recent Submission Section */}
            {recentSubmission && (
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
                      id="equipment.usage.recentSubmission"
                      defaultMessage="Recent Submission"
                    />
                  </h3>
                  <Button kind="secondary" size="sm" onClick={handlePrint}>
                    <FormattedMessage
                      id="common.print"
                      defaultMessage="Print"
                    />
                  </Button>
                </div>

                {/* Recent Submission Details */}
                <div className="submissionDetailsSection">
                  <div className="submissionDetailRow">
                    <div className="submissionDetailField">
                      <label>
                        <FormattedMessage
                          id="equipment.submission.id"
                          defaultMessage="Record ID"
                        />
                      </label>
                      <span className="submissionDetailValue">
                        {recentSubmission.id}
                      </span>
                    </div>
                    <div className="submissionDetailField">
                      <label>
                        <FormattedMessage
                          id="equipment.submission.equipment"
                          defaultMessage="Equipment"
                        />
                      </label>
                      <span className="submissionDetailValue">
                        {recentSubmission.inventoryItemName}
                      </span>
                    </div>
                    <div className="submissionDetailField">
                      <label>
                        <FormattedMessage
                          id="equipment.submission.lot"
                          defaultMessage="Lot Number"
                        />
                      </label>
                      <span className="submissionDetailValue">
                        {recentSubmission.lotNumber}
                      </span>
                    </div>
                  </div>

                  <div className="submissionDetailRow">
                    <div className="submissionDetailField">
                      <label>
                        <FormattedMessage
                          id="equipment.submission.quantity"
                          defaultMessage="Quantity Used"
                        />
                      </label>
                      <span className="submissionDetailValue">
                        {recentSubmission.quantityUsed}
                      </span>
                    </div>
                    <div className="submissionDetailField">
                      <label>
                        <FormattedMessage
                          id="equipment.submission.user"
                          defaultMessage="Performed By"
                        />
                      </label>
                      <span className="submissionDetailValue">
                        {recentSubmission.performedByUserName}
                      </span>
                    </div>
                    <div className="submissionDetailField">
                      <label>
                        <FormattedMessage
                          id="equipment.submission.date"
                          defaultMessage="Usage Date"
                        />
                      </label>
                      <span className="submissionDetailValue">
                        {recentSubmission.usageDate
                          ? new Date(recentSubmission.usageDate).toLocaleString(
                              intl.locale,
                            )
                          : "N/A"}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default EquipmentUsageDashboard;
