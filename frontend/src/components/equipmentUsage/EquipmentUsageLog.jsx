import { useState, useEffect, useContext, useCallback } from "react";
import {
  Button,
  Grid,
  Column,
  TextInput,
  ButtonSet,
  DataTable,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Modal,
  Loading,
  DatePicker,
  DatePickerInput,
  Select,
  SelectItem,
  Dropdown,
  DropdownItem,
  TimePicker,
  TimePickerSelect,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CartridgeUsageAPI from "./EquipmentUsageService";
import ChooseEquipmentModal from "./modals/ChooseEquipment";
import "./EquipmentUsage.css";

/**
 * EquipmentUsageLog Component
 *
 * Main form for tracking cartridge (equipment) usage in the MNTD laboratory.
 * Displays:
 * - Equipment selection dropdown (filtered to CARTRIDGE type from inventory)
 * - Equipment details (name, serial number, department)
 * - Dynamic table of usage log entries with fields for:
 *   - Date
 *   - Operator Name
 *   - Login Time
 *   - Activities
 *   - Equipment Status
 *   - Logout Time
 *   - Signature
 *
 * Actions supported:
 * - Add/Remove rows from usage log
 * - Save as draft
 * - Submit for approval
 * - Load saved entry
 * - View history
 * - Print report
 */
const EquipmentUsageLog = () => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
      });
    },
    [addNotification, setNotificationVisible]
  );

  // Helper function to format time as HH:MM
  const formatTime = (date) => {
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  };

  // Helper function to format date as mm/dd/yyyy
  const formatDate = (date) => {
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const year = date.getFullYear();
    return `${month}/${day}/${year}`;
  };

  // Get current time formatted
  const getCurrentTime = () => formatTime(new Date());
  const getCurrentDate = () => formatDate(new Date());

  // Equipment Selection State
  const [selectedEquipment, setSelectedEquipment] = useState(null);
  const [equipment, setEquipment] = useState([]);
  const [loadingEquipment, setLoadingEquipment] = useState(true);
  const [equipmentError, setEquipmentError] = useState(null);

  // Modal State
  const [showChooseEquipmentModal, setShowChooseEquipmentModal] = useState(false);

  // Usage Log Table State
  const [usageRows, setUsageRows] = useState([]);

  // Form State
  const [isSaving, setIsSaving] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load Equipment (Cartridges) on Mount
  useEffect(() => {
    const fetchEquipment = async () => {
      setLoadingEquipment(true);
      setEquipmentError(null);
      try {
        // Fetch cartridges from inventory
        CartridgeUsageAPI.getCartridges(
          (data) => {
            if (data && Array.isArray(data)) {
              setEquipment(data);
            } else {
              setEquipmentError(
                intl.formatMessage({ id: "equipment.error.loadFailed" })
              );
            }
            setLoadingEquipment(false);
          },
          (error) => {
            console.error("Error loading equipment:", error);
            setEquipmentError(
              intl.formatMessage({ id: "equipment.error.loadFailed" })
            );
            setLoadingEquipment(false);
          }
        );
      } catch (error) {
        console.error("Error fetching equipment:", error);
        setEquipmentError(
          intl.formatMessage({ id: "equipment.error.loadFailed" })
        );
        setLoadingEquipment(false);
      }
    };

    fetchEquipment();
  }, [intl]);

  // Handle Equipment Selection
  const handleSelectEquipment = (equipment) => {
    setSelectedEquipment(equipment);
    setShowChooseEquipmentModal(false);
  };

  // Add new row to usage log with auto-filled values
  const handleAddRow = () => {
    const newRow = {
      id: Math.max(...usageRows.map((r) => r.id), 0) + 1,
      date: getCurrentDate(),
      operatorName: userSessionDetails?.firstName || "",
      loginTime: getCurrentTime(),
      activities: "",
      equipmentStatus: "Functional",
      logoutTime: getCurrentTime(),
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
      usageRows.map((row) => (row.id === rowId ? { ...row, [field]: value } : row))
    );
  };

  // Save as Draft
  const handleSaveDraft = () => {
    if (!selectedEquipment) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: intl.formatMessage({ id: "equipment.usage.error.selectEquipment" }),
      });
      return;
    }

    setIsSaving(true);
    try {
      // Save to localStorage for now (draft functionality)
      const draftData = {
        equipment: selectedEquipment,
        rows: usageRows,
        savedAt: new Date().toISOString(),
      };
      localStorage.setItem("equipmentUsageDraft", JSON.stringify(draftData));
      notify({
        kind: NotificationKinds.success,
        title: "Success",
        subtitle: intl.formatMessage({ id: "equipment.usage.message.saveDraft" }),
      });
    } catch (error) {
      console.error("Error saving draft:", error);
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: intl.formatMessage({ id: "equipment.usage.error.saveFailed" }),
      });
    } finally {
      setIsSaving(false);
    }
  };

  // Submit to Server (Record Usage)
  const handleSubmit = () => {
    if (!selectedEquipment) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: intl.formatMessage({ id: "equipment.usage.error.selectEquipment" }),
      });
      return;
    }

    if (usageRows.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: intl.formatMessage({ id: "equipment.usage.error.noRows" }),
      });
      return;
    }

    setIsSubmitting(true);
    try {
      // Record usage via inventory API
      const consumeRequest = {
        itemId: selectedEquipment.id,
        quantity: usageRows.length, // Number of usage entries as quantity
      };

      CartridgeUsageAPI.recordUsage(consumeRequest, (response) => {
        if (response.ok) {
          response.json().then((data) => {
            console.log("Usage recorded:", data);

            // Build a detailed message showing consumed lots
            const consumedLotsInfo = data.consumedLots
              ?.map(
                (lot) =>
                  `${lot.lotNumber || `Lot ${lot.lotId}`}: ${lot.quantityConsumed} unit(s) consumed (${lot.remainingQuantity} remaining)`
              )
              .join(", ") || "Consumption recorded";

            notify({
              kind: NotificationKinds.success,
              title: "Success",
              subtitle: intl.formatMessage(
                { id: "equipment.usage.message.consumedLots" },
                { lots: consumedLotsInfo }
              ),
            });

            // Clear draft
            localStorage.removeItem("equipmentUsageDraft");

            // Reset form
            setUsageRows([]);
            setSelectedEquipment(null);
            setIsSubmitting(false);
          });
        } else {
          throw new Error("Failed to record usage");
        }
      });
    } catch (error) {
      console.error("Error submitting usage:", error);
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: intl.formatMessage({ id: "equipment.usage.error.submitFailed" }),
      });
      setIsSubmitting(false);
    }
  };

  // Load Saved Draft
  const handleLoadSaved = () => {
    try {
      const draftData = localStorage.getItem("equipmentUsageDraft");
      if (draftData) {
        const { equipment: savedEquipment, rows: savedRows } = JSON.parse(draftData);
        setSelectedEquipment(savedEquipment);
        setUsageRows(savedRows);
        notify({
          kind: NotificationKinds.success,
          title: "Success",
          subtitle: intl.formatMessage({ id: "equipment.usage.message.loadedDraft" }),
        });
      } else {
        notify({
          kind: NotificationKinds.error,
          title: "Error",
          subtitle: intl.formatMessage({ id: "equipment.usage.error.noDraft" }),
        });
      }
    } catch (error) {
      console.error("Error loading draft:", error);
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: intl.formatMessage({ id: "equipment.usage.error.loadFailed" }),
      });
    }
  };

  // Clear Form
  const handleClear = () => {
    setSelectedEquipment(null);
    setUsageRows([
      {
        id: 1,
        date: "",
        operatorName: "",
        loginTime: "",
        activities: "",
        equipmentStatus: "Functional",
        logoutTime: "",
        signature: "",
      },
    ]);
  };

  // Print Report
  const handlePrint = () => {
    if (!selectedEquipment) {
      notify({
        kind: NotificationKinds.error,
        title: "Error",
        subtitle: intl.formatMessage({ id: "equipment.usage.error.selectEquipment" }),
      });
      return;
    }
    window.print();
  };

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="equipmentUsageContainer">
            {/* Title */}
            <h2>
              <FormattedMessage id="equipment.usage.title" />
            </h2>

            {/* Action Buttons - Top */}
            <div className="equipmentUsageActions">
              <Button kind="secondary" size="sm" onClick={handleLoadSaved}>
                <FormattedMessage id="equipment.usage.loadSaved" />
              </Button>
              <Button kind="secondary" size="sm" onClick={handleClear}>
                <FormattedMessage id="common.clear" />
              </Button>
              <Button kind="secondary" size="sm" onClick={handlePrint}>
                <FormattedMessage id="common.print" />
              </Button>
              <Button kind="primary" size="sm" onClick={handleSaveDraft}>
                <FormattedMessage id="equipment.usage.saveDraft" />
              </Button>
              <Button kind="primary" size="sm" onClick={handleSubmit}>
                <FormattedMessage id="equipment.usage.submit" />
              </Button>
            </div>

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
                        <span className="equipmentName">{selectedEquipment.name}</span>
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
                    <input type="text" value="MNTD" readOnly className="detailsInput" />
                  </div>
                </div>
              </div>
            )}

            {/* Usage Log Table */}
            <div className="usageLogSection">
              <h3>
                <FormattedMessage id="equipment.usage.logTable" />
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
                          <FormattedMessage id="equipment.usage.table.date" />
                        </th>
                        <th>
                          <FormattedMessage id="equipment.usage.table.operatorName" />
                        </th>
                        <th>
                          <FormattedMessage id="equipment.usage.table.loginTime" />
                        </th>
                        <th>
                          <FormattedMessage id="equipment.usage.table.activities" />
                        </th>
                        <th>
                          <FormattedMessage id="equipment.usage.table.equipmentStatus" />
                        </th>
                        <th>
                          <FormattedMessage id="equipment.usage.table.logoutTime" />
                        </th>
                        <th>
                          <FormattedMessage id="equipment.usage.table.signature" />
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {usageRows.map((row) => (
                      <tr key={row.id}>
                        <td>
                          <DatePicker dateFormat="mm/dd/yyyy">
                            <DatePickerInput
                              id={`date-picker-${row.id}`}
                              labelText="Date"
                              placeholder="mm/dd/yyyy"
                              value={row.date}
                              onChange={(e) =>
                                handleRowChange(row.id, "date", e.target.value)
                              }
                            />
                          </DatePicker>
                        </td>
                        <td>
                          <input
                            type="text"
                            value={row.operatorName}
                            onChange={(e) =>
                              handleRowChange(row.id, "operatorName", e.target.value)
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
                              handleRowChange(row.id, "loginTime", e.target.value)
                            }
                            placeholder="HH:MM"
                            className="tableInput"
                          />
                        </td>
                        <td>
                          <textarea
                            value={row.activities}
                            onChange={(e) =>
                              handleRowChange(row.id, "activities", e.target.value)
                            }
                            placeholder="Activities"
                            className="tableTextarea"
                          />
                        </td>
                        <td>
                          <select
                            value={row.equipmentStatus}
                            onChange={(e) =>
                              handleRowChange(row.id, "equipmentStatus", e.target.value)
                            }
                            className="tableSelect"
                          >
                            <option value="Functional">Functional</option>
                            <option value="Non-functional">Non-functional</option>
                            <option value="Maintenance">Maintenance</option>
                          </select>
                        </td>
                        <td>
                          <input
                            type="time"
                            value={row.logoutTime}
                            onChange={(e) =>
                              handleRowChange(row.id, "logoutTime", e.target.value)
                            }
                            placeholder="HH:MM"
                            className="tableInput"
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            value={row.signature}
                            onChange={(e) =>
                              handleRowChange(row.id, "signature", e.target.value)
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

            {/* Action Buttons - Bottom */}
            <div className="equipmentUsageActionsBottom">
              <ButtonSet>
                <Button kind="secondary" onClick={handleAddRow}>
                  Add Row
                </Button>
                {usageRows.length > 1 && (
                  <Button
                    kind="danger"
                    onClick={() => handleRemoveRow(usageRows[usageRows.length - 1].id)}
                  >
                    Remove Row
                  </Button>
                )}
              </ButtonSet>
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
