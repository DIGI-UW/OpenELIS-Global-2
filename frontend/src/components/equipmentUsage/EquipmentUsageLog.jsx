import { useState, useEffect, useContext, useCallback } from "react";
import {
  Button,
  Grid,
  Column,
  Loading,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CartridgeUsageAPI from "./EquipmentUsageService";
import ChooseEquipmentModal from "./modals/ChooseEquipment";
import "./EquipmentUsage.css";

/**
 * EquipmentUsageLog Component
 *
 * Form for recording equipment usage in the MNTD laboratory.
 * Features:
 * - Equipment selection (filtered to CARTRIDGE type from inventory)
 * - Equipment details display (name, serial number, department)
 * - Table for recording multiple usage entries with:
 *   - Date
 *   - Operator Name
 *   - Login Time
 *   - Activities
 *   - Equipment Status
 *   - Logout Time
 *   - Signature
 * - Submit button to record usage without reducing inventory
 * - Calls onSubmitSuccess callback with API response for display in dashboard
 */
const EquipmentUsageLog = ({ onSubmitSuccess }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
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

  // Helper functions for date/time formatting
  const formatTime = (date) => {
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  };

  const getCurrentTime = () => formatTime(new Date());

  // Equipment Selection State
  const [selectedEquipment, setSelectedEquipment] = useState(null);
  const [equipment, setEquipment] = useState([]);
  const [loadingEquipment, setLoadingEquipment] = useState(true);
  const [equipmentError, setEquipmentError] = useState(null);

  // Modal State
  const [showChooseEquipmentModal, setShowChooseEquipmentModal] =
    useState(false);

  // Usage Log Table State
  const [usageRows, setUsageRows] = useState([]);

  // Form State
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load Equipment (Cartridges) on Mount
  useEffect(() => {
    const fetchEquipment = async () => {
      setLoadingEquipment(true);
      setEquipmentError(null);
      try {
        // Fetch cartridges from inventory
        CartridgeUsageAPI.getCartridges((data, error) => {
          if (error) {
            notify({
              kind: NotificationKinds.error,
              title: intl.formatMessage({ id: "notification.error" }),
              message: intl.formatMessage({
                id: "equipment.error.loadFailed",
                defaultMessage: "Failed to load equipment",
              }),
            });
            setLoadingEquipment(false);
          } else if (data && Array.isArray(data)) {
            setEquipment(data);
            setLoadingEquipment(false);
          } else {
            notify({
              kind: NotificationKinds.error,
              title: intl.formatMessage({ id: "notification.error" }),
              message: intl.formatMessage({
                id: "equipment.error.loadFailed",
                defaultMessage: "Failed to load equipment",
              }),
            });
            setLoadingEquipment(false);
          }
        });
      } catch (error) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.error" }),
          message: intl.formatMessage({
            id: "equipment.error.loadFailed",
            defaultMessage: "Failed to load equipment",
          }),
        });
        setLoadingEquipment(false);
      }
    };

    fetchEquipment();
  }, [intl, notify]);

  // Handle Equipment Selection
  const handleSelectEquipment = (equipment) => {
    setSelectedEquipment(equipment);
    setShowChooseEquipmentModal(false);
  };

  // Add new row to usage log with auto-filled values
  const handleAddRow = () => {
    const newRow = {
      id: Math.max(...usageRows.map((r) => r.id), 0) + 1,
      date: "",
      operatorName: userSessionDetails?.firstName || "",
      loginTime: getCurrentTime(),
      activities: "",
      equipmentStatus: "Functional",
      logoutTime: getCurrentTime(),
      approvedBy: "",
      approvalDate: "",
      signature: userSessionDetails?.firstName || "",
    };
    setUsageRows([...usageRows, newRow]);
  };

  // Remove row from usage log
  const handleRemoveRow = (rowId) => {
    if (usageRows.length > 1) {
      setUsageRows(usageRows.filter((row) => row.id !== rowId));
    }
  };

  // Update row field
  const handleRowChange = (rowId, field, value) => {
    setUsageRows(
      usageRows.map((row) =>
        row.id === rowId ? { ...row, [field]: value } : row,
      ),
    );
  };

  // Submit to Server (Record Equipment Usage - without inventory deduction)
  const handleSubmit = () => {
    if (!selectedEquipment) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({
          id: "equipment.usage.error.selectEquipment",
        }),
      });
      return;
    }

    if (usageRows.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({
          id: "equipment.usage.error.noRows",
          defaultMessage: "Please add at least one usage entry",
        }),
      });
      return;
    }

    setIsSubmitting(true);
    try {
      // Get the first available lot for this equipment
      CartridgeUsageAPI.getAvailableLots(
        selectedEquipment.id,
        (lots) => {
          if (!lots || lots.length === 0) {
            notify({
              kind: NotificationKinds.error,
              title: intl.formatMessage({ id: "notification.error" }),
              message: intl.formatMessage({
                id: "equipment.usage.error.noLotsAvailable",
              }),
            });
            setIsSubmitting(false);
            return;
          }

          // Use the first available lot
          const lot = lots[0];

          // Submit each row with all form data to the new /submit endpoint
          let lastResponse = null;
          let submitCount = 0;

          const submitNextRow = (rowIndex) => {
            if (rowIndex >= usageRows.length) {
              // All rows submitted successfully
              notify({
                kind: NotificationKinds.success,
                title: intl.formatMessage({ id: "notification.success" }),
                message: intl.formatMessage({
                  id: "equipment.usage.message.recordedSuccess",
                }),
              });

              // Reset form
              setSelectedEquipment(null);
              setUsageRows([]);

              // Call parent callback with last response to display in dashboard
              if (onSubmitSuccess && lastResponse) {
                onSubmitSuccess(lastResponse);
              }

              setIsSubmitting(false);
              return;
            }

            const row = usageRows[rowIndex];

            // Build the complete equipment usage entry request with all form fields
            const entryRequest = {
              itemId: selectedEquipment.id,
              lotId: lot.id,
              quantity: 1, // Equipment usage counts as 1 unit per submission
              labUnitId: userSessionDetails?.labUnit || "",
              operatorName: row.operatorName,
              date: row.date,
              loginTime: row.loginTime,
              activities: row.activities,
              equipmentStatus: row.equipmentStatus,
              logoutTime: row.logoutTime,
              approvedBy: row.approvedBy,
              approvalDate: row.approvalDate,
            };

            // Submit to new endpoint that accepts all form data
            CartridgeUsageAPI.submitEquipmentUsageEntry(
              entryRequest,
              (response) => {
                console.log("=== ENTRY SUBMITTED CALLBACK ===", response);
                if (response.ok) {
                  response
                    .json()
                    .then((data) => {
                      console.log("Equipment usage entry submitted:", data);
                      lastResponse = data;
                      submitCount++;

                      // Submit next row
                      submitNextRow(rowIndex + 1);
                    })
                    .catch((error) => {
                      console.error("Error parsing response:", error);
                      notify({
                        kind: NotificationKinds.error,
                        title: intl.formatMessage({ id: "notification.error" }),
                        message: "Failed to process response",
                      });
                      setIsSubmitting(false);
                    });
                } else {
                  console.error("Response not OK:", response.status);
                  notify({
                    kind: NotificationKinds.error,
                    title: intl.formatMessage({ id: "notification.error" }),
                    message: intl.formatMessage({
                      id: "equipment.usage.error.submitFailed",
                    }),
                  });
                  setIsSubmitting(false);
                }
              },
              (error) => {
                console.error("Error submitting entry:", error);
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({ id: "notification.error" }),
                  message: intl.formatMessage({
                    id: "equipment.usage.error.submitFailed",
                  }),
                });
                setIsSubmitting(false);
              },
            );
          };

          // Start submitting from first row
          submitNextRow(0);
        },
        (error) => {
          console.error("Error loading lots:", error);
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            message: "Failed to load available lots",
          });
          setIsSubmitting(false);
        },
      );
    } catch (error) {
      console.error("Error submitting usage:", error);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({
          id: "equipment.usage.error.submitFailed",
        }),
      });
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <AlertDialog />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="equipmentUsageContainer">
            {/* Equipment Selection Section */}
            {loadingEquipment ? (
              <Loading description="Loading equipment..." />
            ) : (
              <div className="equipmentSelectionSection">
                <h3>
                  <FormattedMessage id="equipment.usage.selectedEquipment" />
                </h3>
                {selectedEquipment ? (
                  <div className="equipmentListSection">
                    <div className="equipmentItem">
                      <div className="equipmentItemContent">
                        <span className="equipmentName">
                          {selectedEquipment.name}
                        </span>
                        <span className="equipmentSerial">
                          {selectedEquipment.catalogNumber || "No serial"}
                        </span>
                        <Button
                          kind="ghost"
                          size="sm"
                          className="removeEquipmentBtn"
                          onClick={() => handleSelectEquipment(null)}
                        >
                          <FormattedMessage id="common.remove" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <Button
                    kind="primary"
                    size="sm"
                    onClick={() => setShowChooseEquipmentModal(true)}
                  >
                    <FormattedMessage id="equipment.usage.chooseEquipment" />
                  </Button>
                )}
              </div>
            )}

            {/* Equipment Details Section */}
            {selectedEquipment && (
              <div className="equipmentDetailsSection">
                <div className="detailsRow">
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.name" />
                    </label>
                    <input
                      type="text"
                      value={selectedEquipment.name}
                      readOnly
                      className="detailsInput"
                    />
                  </div>
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.serialNumber" />
                    </label>
                    <input
                      type="text"
                      value={selectedEquipment.catalogNumber || ""}
                      readOnly
                      className="detailsInput"
                    />
                  </div>
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.department" />
                    </label>
                    <input
                      type="text"
                      value="MNTD"
                      readOnly
                      className="detailsInput"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Usage Log Table */}
            <div className="usageLogSection">
              <h3>
                <FormattedMessage
                  id="equipment.usage.logTable"
                  defaultMessage="Usage Log"
                />
              </h3>
              {usageRows.length === 0 ? (
                <div className="emptyStateSection">
                  <p>
                    <FormattedMessage
                      id="equipment.usage.emptyTable"
                      defaultMessage="No usage records yet. Click 'Add Row' below to start recording equipment usage."
                    />
                  </p>
                </div>
              ) : (
                <div className="tableWrapper">
                  <table className="usageLogTable">
                    <thead>
                      <tr>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.date"
                            defaultMessage="Date"
                          />
                        </th>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.operatorName"
                            defaultMessage="Operator Name"
                          />
                        </th>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.loginTime"
                            defaultMessage="Login Time"
                          />
                        </th>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.activities"
                            defaultMessage="Activities"
                          />
                        </th>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.equipmentStatus"
                            defaultMessage="Equipment Status"
                          />
                        </th>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.logoutTime"
                            defaultMessage="Logout Time"
                          />
                        </th>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.approvedBy"
                            defaultMessage="Approved By"
                          />
                        </th>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.approvalDate"
                            defaultMessage="Approval Date"
                          />
                        </th>
                        <th>
                          <FormattedMessage
                            id="equipment.usage.table.signature"
                            defaultMessage="Signature"
                          />
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {usageRows.map((row) => (
                        <tr key={row.id}>
                          <td>
                            <DatePicker
                              datePickerType="single"
                              dateFormat="mm/dd/yyyy"
                            >
                              <DatePickerInput
                                id={`date-picker-${row.id}`}
                                labelText="Date"
                                placeholder="mm/dd/yyyy"
                                value={row.date}
                                onChange={(e) =>
                                  handleRowChange(
                                    row.id,
                                    "date",
                                    e.target.value,
                                  )
                                }
                              />
                            </DatePicker>
                          </td>
                          <td>
                            <input
                              type="text"
                              value={row.operatorName}
                              onChange={(e) =>
                                handleRowChange(
                                  row.id,
                                  "operatorName",
                                  e.target.value,
                                )
                              }
                              placeholder="Operator name"
                              className="tableInput"
                            />
                          </td>
                          <td>
                            <input
                              type="time"
                              value={row.loginTime}
                              onChange={(e) =>
                                handleRowChange(
                                  row.id,
                                  "loginTime",
                                  e.target.value,
                                )
                              }
                              placeholder="HH:MM"
                              className="tableInput"
                            />
                          </td>
                          <td>
                            <textarea
                              value={row.activities}
                              onChange={(e) =>
                                handleRowChange(
                                  row.id,
                                  "activities",
                                  e.target.value,
                                )
                              }
                              placeholder="Activities"
                              className="tableTextarea"
                            />
                          </td>
                          <td>
                            <select
                              value={row.equipmentStatus}
                              onChange={(e) =>
                                handleRowChange(
                                  row.id,
                                  "equipmentStatus",
                                  e.target.value,
                                )
                              }
                              className="tableSelect"
                            >
                              <option value="Functional">Functional</option>
                              <option value="Non-functional">
                                Non-functional
                              </option>
                              <option value="Maintenance">Maintenance</option>
                            </select>
                          </td>
                          <td>
                            <input
                              type="time"
                              value={row.logoutTime}
                              onChange={(e) =>
                                handleRowChange(
                                  row.id,
                                  "logoutTime",
                                  e.target.value,
                                )
                              }
                              placeholder="HH:MM"
                              className="tableInput"
                            />
                          </td>
                          <td>
                            <input
                              type="text"
                              value={row.approvedBy}
                              onChange={(e) =>
                                handleRowChange(
                                  row.id,
                                  "approvedBy",
                                  e.target.value,
                                )
                              }
                              placeholder="Approved by"
                              className="tableInput"
                            />
                          </td>
                          <td>
                            <DatePicker
                              datePickerType="single"
                              dateFormat="mm/dd/yyyy"
                            >
                              <DatePickerInput
                                id={`approval-date-picker-${row.id}`}
                                labelText="Approval Date"
                                placeholder="mm/dd/yyyy"
                                value={row.approvalDate}
                                onChange={(e) =>
                                  handleRowChange(
                                    row.id,
                                    "approvalDate",
                                    e.target.value,
                                  )
                                }
                              />
                            </DatePicker>
                          </td>
                          <td>
                            <input
                              type="text"
                              value={row.signature}
                              onChange={(e) =>
                                handleRowChange(
                                  row.id,
                                  "signature",
                                  e.target.value,
                                )
                              }
                              placeholder="Type name or upload"
                              className="tableInput signatureInput"
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Add/Remove Row Buttons */}
            <div className="equipmentUsageActionsBottom">
              <Button kind="secondary" onClick={handleAddRow}>
                <FormattedMessage
                  id="equipment.usage.addRow"
                  defaultMessage="Add Row"
                />
              </Button>
              {usageRows.length > 1 && (
                <Button
                  kind="danger"
                  onClick={() =>
                    handleRemoveRow(usageRows[usageRows.length - 1].id)
                  }
                >
                  <FormattedMessage
                    id="equipment.usage.removeRow"
                    defaultMessage="Remove Row"
                  />
                </Button>
              )}
            </div>

            {/* Action Buttons */}
            <div className="equipmentUsageActionsBottom">
              <Button
                kind="primary"
                size="sm"
                onClick={handleSubmit}
                disabled={!selectedEquipment || isSubmitting}
              >
                {isSubmitting ? (
                  <FormattedMessage
                    id="common.submitting"
                    defaultMessage="Submitting..."
                  />
                ) : (
                  <FormattedMessage id="equipment.usage.submit" />
                )}
              </Button>
            </div>
          </div>
        </Column>
      </Grid>

      {/* Choose Equipment Modal */}
      <ChooseEquipmentModal
        open={showChooseEquipmentModal}
        onClose={() => setShowChooseEquipmentModal(false)}
        equipment={equipment}
        onSelectEquipment={handleSelectEquipment}
      />
    </>
  );
};

export default EquipmentUsageLog;
